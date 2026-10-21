package net.luversof.api.poe.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoeCraftCalcService.Target;
import net.luversof.api.poe.service.PoeCraftEngine.Action;
import net.luversof.api.poe.service.PoeCraftEngine.Affix;
import net.luversof.api.poe.service.PoeCraftEngine.Item;
import net.luversof.api.poe.service.PoeCraftEngine.Rarity;

/**
 * 최적 제작 경로(10-19 C457 사용자 요청 "에센스 · 제작대 · 야수 같은 다양한 방법을 조합해 원하는 아이템을 뽑는 최소 비용을 계산할 수 있어야 한다").
 *
 * <p>정해 둔 몇 갈래를 견주는 대신, 제작 과정을 상태(희귀도 · 맞은 목표 · 쪽마다 남는 잡옵션 수 · 같은 그룹이 막힌 목표 · 제작대 옵션)로 보고 상태마다 쓸 수
 * 있는 모든 수단(기본 화폐 · 에센스 · 화석 · 수확 · 제작대 목표 옵션 · 메타 제작 · 제작대 옵션 지우기 · 짐승 맞바꾸기)의 결과를 같은 제작 엔진으로 표본을 떠
 * 전이 확률을 만든다. 그 위에서 가치 반복(확률적 최단 경로)으로 상태마다 "목표까지 남은 최소 기대 비용"과 그때 쓸 수단(정책)을 구하고, 그 정책을 실제 아이템에 따라
 * 굴려 비용을 다시 잰다(상태를 줄여 본 근사가 틀리면 여기서 드러난다).
 */
@Service
public class PoeCraftOptimizer {

  /** upper = 아는 가장 싼 방법의 성공 한 번당 비용(계산기 표 최저, 카오스) — 없으면 연타 표본으로 잰다. */
  public record Request(
      Item item,
      List<Target> targets,
      Integer budgetMs,
      Double upper,
      String upperLabel,
      // 계산기 표가 고른 화석 · 수확 방법 id(FOSSIL:… · HARVEST:…) — 있으면 다시 고르지 않는다(1.8초 절약)
      List<String> methods,
      // 사용자 가격(C529) — 작업 스레드에서 건다
      Map<String, Double> prices) {

    public Request(
        Item item,
        List<Target> targets,
        Integer budgetMs,
        Double upper,
        String upperLabel,
        List<String> methods) {
      this(item, targets, budgetMs, upper, upperLabel, methods, null);
    }
  }

  /** 정책 규칙 하나 — state = 상태 설명, action = 그 상태에서 쓸 수단, share = 시뮬레이션에서 이 상태를 지난 비율. */
  public record Rule(String state, String action, double share, Double remaining) {}

  /**
   * 결과 — expectedCost = 정책을 실제 아이템에 굴려 잰 성공 한 번당 비용(카오스), planCost = 가치 반복이 낸 값(상태 근사), perSuccess
   * = 성공 한 번당 화폐 개수, path = 흔한 흐름(성공한 회차 중 비용이 중간인 것), rules = 자주 지나는 상태의 결정, methods = 고려한 수단.
   */
  public record Result(
      Double expectedCost,
      // 정책 모의 비용의 표준 오차(카오스) — 회차가 적으면 크다
      Double stderr,
      Double planCost,
      // 비교 기준 — 정제 + 아는 최저(위 경계)와 그 이름
      Double upper,
      String upperLabel,
      int states,
      int runs,
      int successes,
      Map<String, Double> perSuccess,
      // 화폐 한국어 이름(영어 이름 → 한국어)
      Map<String, String> namesKo,
      String path,
      List<Rule> rules,
      List<String> methods,
      String league,
      String pricesAt,
      String note,
      String error,
      // 진단(C459b) — 목표에 못 닿은 회차의 마지막 걸음들(화면엔 안 씀)
      String diag,
      // 결과 예시(C502, craftofexile 시뮬레이터의 쇼케이스) — 성공 회차 중 싼 편 · 중간 · 비싼 편의 마지막 아이템
      List<Showcase> showcase,
      // 성공 회차 비용 분포(C505) — 25% · 50% · 90% 지점(못 닿은 회차 비용은 안 넣음, 기대 비용과 다를 수 있다)
      List<Double> costQuantiles,
      // 엘드리치 임플리싯 목표(C549) — 명시 옵션과 따로 잰 비용(더해서 본다), 없으면 null
      PoeCraftCalcService.EldritchResult eldritch) {}

  /** 결과 예시 하나 — 그 회차 비용(카오스) · 옵션 줄(문구는 범위). */
  public record Showcase(double cost, String rarity, List<PoeCraftService.Line> lines) {}

  /** 백그라운드 작업 상태(C457k) — state = running | done, elapsedMs · budgetMs = 진행률, result = 끝났을 때. */
  public record JobStatus(String id, String state, long elapsedMs, long budgetMs, Result result) {}

  private record Running(long started, long budget, java.util.concurrent.Future<Result> future) {}

  /** 에뮬레이터 따라 하기(C464) — 끝난 작업의 정책(상태 → 수단)과 그 상태의 남은 기대 비용. */
  private record Policy(
      long at, Job job, Map<String, Integer> pol, Map<String, Double> v, List<Act> usable) {}

  /**
   * 다음 수단(C464) — done = 이미 목표, known = 정책에 있는 상태인가, action = 쓸 수단(한국어), actionId = 수단 id(CHAOS ·
   * ESSENCE:슬러그 · BENCH:옵션 id 등, 넘김이면 FALLBACK), remaining = 그 상태의 남은 기대 비용.
   */
  public record Next(
      boolean done,
      boolean known,
      String state,
      String action,
      String actionId,
      Double remaining) {}

  /** 다음 수단 요청 — imprint = 에뮬레이터의 각인(없으면 null). */
  public record NextRequest(Item item, Item imprint) {}

  private final Map<String, Policy> policies = new java.util.concurrent.ConcurrentHashMap<>();

  /** 다음 수단 — 모르는 작업이면 null. */
  public Next next(String id, Item item, Item imprint) {
    Policy p = id == null ? null : policies.get(id);
    if (p == null || item == null) {
      return null;
    }
    String k =
        p.job().key(craft.normalize(item))
            + (imprint == null ? "" : "#" + p.job().key(craft.normalize(imprint)));
    int n = p.job().goal.size();
    if (p.job().goalKey(k)) {
      return new Next(true, true, describe(k, n), null, null, 0d);
    }
    Integer ai = p.pol().get(k);
    if (ai == null) {
      return new Next(false, false, describe(k, n), null, null, null);
    }
    return new Next(
        false,
        true,
        describe(k, n),
        label(p.usable(), ai, p.job().upperLabel),
        ai == FALLBACK ? "FALLBACK" : p.usable().get(ai).id(),
        p.v().get(k));
  }

  private final java.util.concurrent.ExecutorService pool =
      java.util.concurrent.Executors.newFixedThreadPool(
          JOB_THREADS,
          r -> {
            Thread t = new Thread(r, "poe-craft-optimizer");
            t.setDaemon(true);
            return t;
          });

  private final Map<String, Running> jobs = new java.util.concurrent.ConcurrentHashMap<>();

  /** 백그라운드로 시작 — 예산 없으면 20초. 작업 id. */
  public String start(Request req) {
    long now = System.currentTimeMillis();
    jobs.entrySet().removeIf(e -> now - e.getValue().started() > JOB_KEEP_MS);
    long budget = req.budgetMs() == null ? JOB_BUDGET_MS : Math.min(40_000, req.budgetMs());
    Request r =
        new Request(
            req.item(),
            req.targets(),
            (int) budget,
            req.upper(),
            req.upperLabel(),
            req.methods(),
            req.prices());
    String id = java.util.UUID.randomUUID().toString();
    jobs.put(
        id,
        new Running(
            now,
            budget,
            pool.submit(
                () -> {
                  // 사용자 가격(C529) — 작업 스레드에서(요청 스레드의 값은 따라오지 않는다)
                  PoeCraftPriceService.override(r.prices());
                  try {
                    return optimize(r, id);
                  } finally {
                    PoeCraftPriceService.override(null);
                  }
                })));
    policies.entrySet().removeIf(e -> now - e.getValue().at() > JOB_KEEP_MS);
    return id;
  }

  /** 작업 조회 — 모르는 id 면 null. */
  public JobStatus status(String id) {
    Running run = id == null ? null : jobs.get(id);
    if (run == null) {
      return null;
    }
    long el = System.currentTimeMillis() - run.started();
    if (!run.future().isDone()) {
      return new JobStatus(id, "running", el, run.budget(), null);
    }
    Result res;
    try {
      res = run.future().get();
    } catch (Exception e) {
      res = error("계산 중 오류: " + e.getClass().getSimpleName());
    }
    return new JobStatus(id, "done", el, run.budget(), res);
  }

  /** 수단 하나 — 같은 엔진 동작 · 인자, 한 번 쓸 때 화폐(이름 → 개수)와 비용(카오스). */
  private record Act(
      String id, Action action, String arg, String ko, Map<String, Double> spend, double cost) {}

  static final int MAX_STATES = 5_000;
  // 다시 굴리기는 남는 옵션이 같은 상태끼리 결과 분포를 함께 써 종류가 적다 — 드문 결과(목표 둘이 한 번에)의 확률을 잡도록 크게(C457e: 1,200 이면 목표 둘
  // 매직 확률을
  // 몇 번 안 되는 성공으로 잡아 계획 82c ↔ 실제 202c)
  static final int SAMPLE_REROLL = 30_000;
  static final int SAMPLE_STEP = 400;
  // 다시 굴리기 표본을 늘리는 기준 — 목표를 새로 맞힌 결과 수(C472d)
  static final int RARE_HITS = 40;
  // 다듬기(C467) — 정책이 닿는 상태의 수단을 다시 뜨는 표본 수 · 상태 수 · 상태마다 수단 수
  static final int SAMPLE_REFINE = 4_000;
  static final int REFINE_STATES = 300;
  static final int REFINE_ACTS = 3;
  // 각인(C468) — 수단 id(에뮬레이터 단추와 같다)
  static final String IMPRINT = "IMPRINT";
  static final String RESTORE = "IMPRINT_RESTORE";
  // 바꾸기 기억에서 '바꿀 수 없음' 표시(C469b)
  private static final Affix NO_AFFIX = new Affix("", "", List.of(), false, false);
  static final int SIM_RUNS = 400;
  static final int SIM_STEPS = 50_000;
  static final int STUCK_STEPS = 200;
  // 한 회차에서 '처음부터 다시(이 조합)'를 하는 최대 횟수(C469j) — 넘으면 못 끝낸 회차(C536)
  static final int RESTART_RUNS = 200;
  // 고리 감시(C469f) — 한 회차 비용이 계획의 이 배수를 넘으면 넘김
  static final double CYCLE_FACTOR = 12;
  // 모의 하드 한도 — 예산 뒤 이만큼까지만(C472f)
  static final long SIM_GRACE_MS = 10_000;
  // 시간이 다 된 뒤 최소 회차에서 내다볼 상태 수 상한(C469l)
  static final int LATE_LOOKS = 300;
  static final long DEFAULT_BUDGET_MS = 8_000;

  /** 백그라운드 작업 기본 예산(C457k) — 게이트가 조회하며 기다린다. */
  static final long JOB_BUDGET_MS = 30_000;

  static final int JOB_THREADS = 2;
  static final long JOB_KEEP_MS = 10 * 60_000L;
  // 라운드당 넓힐 상태 — 병렬로 넓혀 코어 수보다 넉넉히(C459e)
  static final int EXPAND_PER_ROUND = 120;
  static final int SPAM_SAMPLE = 20_000;

  /** 정책 값 — '정제 → 아는 최저 방법'(위 경계). */
  static final int FALLBACK = -1;

  // 라운드당 가치 반복 횟수 — 앞 라운드 값에서 이어 가므로 라운드마다 수렴까지 갈 필요 없다(C457m)
  static final int VI_SWEEPS = 300;
  static final double MIN_COST = 0.001;

  private final PoeCraftService craft;
  private final PoeCraftCalcService calc;
  private final PoeCraftPriceService prices;

  public PoeCraftOptimizer(
      PoeCraftService craft, PoeCraftCalcService calc, PoeCraftPriceService prices) {
    this.craft = craft;
    this.calc = calc;
    this.prices = prices;
  }

  /** 한 요청의 작업 상태. */
  private final class Job {
    final List<Target> goal;
    // 빈 자리 조건(C520 — C519 의 '최적 조합엔 미반영'을 없앰): 쪽마다 남아야 할 빈 자리 [접두, 접미] · 레어 쪽당 한도. 있으면 열쇠 끝에 쪽별 옵션
    // 수
    int[] openNeed = new int[2];
    int openCap = 3;
    final List<List<Integer>> targetGroups = new ArrayList<>();
    final List<java.util.Set<String>> goalSets = new ArrayList<>();
    // 옵션 id → 채우는 목표 비트, 옵션 그룹 → 그 그룹이 막는 목표 비트(C457l)
    final Map<String, Integer> idMask = new HashMap<>();
    final Map<Integer, Integer> groupMask = new HashMap<>();
    final String lockPre;
    final String lockSuf;
    final String multi;
    final List<Act> acts;
    // 넓히기를 병렬로 해서(C459e) 동시 맵
    final Map<String, Item> rep = new java.util.concurrent.ConcurrentHashMap<>();
    final Map<String, Map<Integer, Map<String, Double>>> trans =
        new java.util.concurrent.ConcurrentHashMap<>();
    final Map<String, Map<String, Double>> rerollCache =
        new java.util.concurrent.ConcurrentHashMap<>();
    final long deadline;
    final SplittableRandom rng = new SplittableRandom(457);
    // 표본 난수는 스레드마다(SplittableRandom 은 스레드 안전하지 않다)
    final java.util.concurrent.atomic.AtomicLong seeds =
        new java.util.concurrent.atomic.AtomicLong(4570);
    final ThreadLocal<SplittableRandom> localRng =
        ThreadLocal.withInitial(() -> new SplittableRandom(seeds.incrementAndGet()));
    double upper = 1e9;
    // 엘드리치 수단이 있으면 상태에 우세(작열의 총주교 e · 세계 포식자 a)를 넣는다(C459)
    boolean eldritch;
    // 다시 시작(C471) — 정제한 출발 아이템의 열쇠 · 정제 비용 · 가치 반복이 낸 넘김 값
    String restartKey;
    double restartCost;
    volatile double effUpper = Double.MAX_VALUE;
    // 수확 원소 바꾸기(C469) — 수단과 바꿀 원본 옵션 유형
    final List<Act> converts = new ArrayList<>();
    final List<java.util.Set<String>> convertSrc = new ArrayList<>();
    // 원본 옵션 → 바꾼 옵션(C469b — 열쇠마다 엔진 바꾸기를 돌리면 처음 상태 하나에 9초)
    final Map<String, Affix> convertMemo = new java.util.concurrent.ConcurrentHashMap<>();
    String upperLabel = "";
    final PoeCraftEngine engine = craft.engine();

    Job(
        List<Target> goal,
        String lockPre,
        String lockSuf,
        String multi,
        List<Act> acts,
        long deadline) {
      this.goal = goal;
      this.lockPre = lockPre;
      this.lockSuf = lockSuf;
      this.multi = multi;
      this.acts = acts;
      this.deadline = deadline;
      for (Target t : goal) {
        goalSets.add(new java.util.HashSet<>(t.anyOf()));
        java.util.Set<Integer> g = new java.util.TreeSet<>();
        for (String id : t.anyOf()) {
          PoeCraftDataService.CraftMod m = craft.mod(id);
          if (m != null && m.grp() != null) {
            g.addAll(m.grp());
          }
        }
        targetGroups.add(new ArrayList<>(g));
        int bit = 1 << (targetGroups.size() - 1);
        for (String id : t.anyOf()) {
          idMask.merge(id, bit, (x, y) -> x | y);
        }
        for (Integer gi : g) {
          groupMask.merge(gi, bit, (x, y) -> x | y);
        }
      }
    }

    /** 상태 열쇠 — 희귀도 | 맞은 목표 | 잡옵션 접두,접미 | 그룹이 막힌 목표 | 제작대 옵션 종류. */
    String key(Item it) {
      int n = goal.size();
      List<Affix> af = it.affixes();
      int m = af.size();
      int[] am = new int[m];
      for (int i = 0; i < m; i++) {
        am[i] = idMask.getOrDefault(af.get(i).id(), 0);
      }
      // 최대 배정 — dp[i][s] = 옵션 i 개까지 써서 목표 집합 s 를 채울 수 있나(옵션 하나 = 목표 하나)
      int full = 1 << n;
      boolean[][] dp = new boolean[m + 1][full];
      dp[0][0] = true;
      for (int i = 0; i < m; i++) {
        for (int sMask = 0; sMask < full; sMask++) {
          if (!dp[i][sMask]) {
            continue;
          }
          dp[i + 1][sMask] = true;
          int free = am[i] & ~sMask;
          while (free != 0) {
            int bit = free & -free;
            dp[i + 1][sMask | bit] = true;
            free &= free - 1;
          }
        }
      }
      int mask = 0;
      for (int sMask = 0; sMask < full; sMask++) {
        if (dp[m][sMask] && Integer.bitCount(sMask) > Integer.bitCount(mask)) {
          mask = sMask;
        }
      }
      // 배정 되짚기 — 어느 옵션이 목표에 쓰였나
      boolean[] used = new boolean[m];
      int cur = mask;
      for (int i = m - 1; i >= 0; i--) {
        if (dp[i][cur]) {
          continue; // 옵션 i 없이도 된다
        }
        int cand = am[i] & cur;
        while (cand != 0) {
          int bit = cand & -cand;
          if (dp[i][cur & ~bit]) {
            used[i] = true;
            cur &= ~bit;
            break;
          }
          cand &= cand - 1;
        }
      }
      int jp = 0;
      int js = 0;
      int blocked = 0;
      StringBuilder cr = new StringBuilder();
      for (int i = 0; i < m; i++) {
        Affix a = af.get(i);
        if (a.crafted()) {
          cr.append(
              used[i]
                  ? 'T'
                  : a.id().equals(lockPre)
                      ? 'P'
                      : a.id().equals(lockSuf) ? 'S' : a.id().equals(multi) ? 'M' : 'j');
          continue;
        }
        // 분열된 옵션(C465) — 다시 굴려도 남으니 보통 옵션과 다른 상태: 목표면 F, 잡옵션이면 f(접두) · g(접미)
        if (used[i]) {
          if (a.fractured()) {
            cr.append('F');
          }
          continue;
        }
        if (a.fractured()) {
          cr.append("prefix".equals(a.gen()) ? 'f' : 'g');
        }
        // 장막 옵션(C469e) — 해제는 장막 옵션이 있어야(열쇠가 몰라 같은 열쇠의 다른 아이템에서 378번 실패)
        if (PoeCraftEngine.veiled(a)) {
          cr.append('v');
        }
        if ("prefix".equals(a.gen())) {
          jp++;
        } else {
          js++;
        }
        if (a.groups() != null) {
          for (Integer g : a.groups()) {
            blocked |= groupMask.getOrDefault(g, 0);
          }
        }
      }
      blocked &= ~mask;
      char[] c = cr.toString().toCharArray();
      java.util.Arrays.sort(c);
      // 우세는 레어에서만(C469k — 섬뜩한 화폐는 레어 전용. 정제해도 임플리싯이 남아 '일반 · 작열의 총주교 우세'가 못 넓힌 다른 상태가 되면
      // 처음부터 값을 못 써 넘김(위 경계 전부)이 났다)
      boolean rareDom = eldritch && it.rarity() == Rarity.RARE;
      String dom = rareDom ? engine.dominance(it) : null;
      if (rareDom && dom == null && it.implicits() != null) {
        // 둘 다 있는데 우세가 없으면 동률(t) — 임플리싯 없음과 다르다(불씨 · 영액이 할 일이 다름)
        boolean ex = it.implicits().stream().anyMatch(x -> PoeCraftEngine.EXARCH.equals(x.gen()));
        boolean ea = it.implicits().stream().anyMatch(x -> PoeCraftEngine.EATER.equals(x.gen()));
        dom = ex && ea ? "tie" : null;
      }
      return it.rarity().name().charAt(0)
          + (dom == null ? "" : dom.substring(0, 1))
          + "|"
          + mask
          + "|"
          + jp
          + ","
          + js
          + "|"
          + blocked
          + "|"
          + new String(c)
          + convertBits(it, Integer.bitCount(mask))
          + openBits(it);
    }

    /** 빈 자리 조건이 있으면 '|o접두수,접미수'(C520) — 제작대 잡옵션의 쪽을 열쇠가 몰라 같은 열쇠에 남은 자리가 다른 아이템이 섞이지 않게. */
    String openBits(Item it) {
      if (openNeed[0] + openNeed[1] == 0) {
        return "";
      }
      int p = 0;
      int s = 0;
      for (Affix a : it.affixes()) {
        if ("prefix".equals(a.gen())) {
          p++;
        } else if ("suffix".equals(a.gen())) {
          s++;
        }
      }
      return "|o" + p + "," + s;
    }

    /** 목표 도달(실제 아이템) — 옵션 목표 전부 + 빈 자리 조건. */
    boolean met(Item it) {
      if (!PoeCraftCalcService.meets(it, goal)) {
        return false;
      }
      if (openNeed[0] + openNeed[1] == 0) {
        return true;
      }
      int[] free = PoeCraftCalcService.openFree(it);
      return free[0] >= openNeed[0] && free[1] >= openNeed[1];
    }

    /**
     * 바꾸기 표시(C469) — 원소 바꾸기는 결정적(첫 해당 옵션 → 같은 등급)인데 쓸 수 있는지 · 목표가 되는지가 잡옵션의 정체에 달려, 열쇠가 그것을 모르면 같은
     * 열쇠의 다른 아이템에서 실패한다. 바꾸기마다 한 자리(0 · 1 · 2, 하나도 못 쓰면 빈 문자열).
     */
    String convertBits(Item it, int met) {
      if (converts.isEmpty()) {
        return "";
      }
      List<Affix> af = it.affixes();
      List<Affix> fixed = null;
      // 바꾸기마다 0 = 쓸 수 없음 · 1 = 쓸 수 있지만 맞은 목표가 안 늘어남 · 2 = 늘어남(C469e — 1 을 0 과 같은 열쇠로 두면 바꾸기를 골랐다가
      // 원본 옵션이 없는 아이템에서 실패)
      char[] conv = new char[converts.size()];
      java.util.Arrays.fill(conv, '0');
      boolean anyConv = false;
      for (int ci = 0; ci < converts.size(); ci++) {
        java.util.Set<String> src = convertSrc.get(ci);
        // 엔진과 같은 원본 — 분열 · 제작 · 메타로 고정된 쪽이 아닌 첫 해당 유형 옵션
        Affix from = null;
        for (Affix a : af) {
          if (a.fractured() || a.crafted()) {
            continue;
          }
          PoeCraftDataService.CraftMod md = craft.mod(a.id());
          if (md != null && md.mt() != null && src.contains(md.mt())) {
            if (fixed == null) {
              fixed = PoeCraftEngine.kept(it);
            }
            if (!fixed.contains(a)) {
              from = a;
              break;
            }
          }
        }
        if (from == null) {
          continue;
        }
        Affix to = convertTo(ci, from, it);
        if (to == null) {
          continue;
        }
        // 바뀐 옵션이 남은 옵션과 같은 그룹이면 엔진이 거절
        boolean clash = false;
        for (Affix a : af) {
          if (a != from && a.groups() != null && to.groups() != null) {
            for (Integer g : a.groups()) {
              if (to.groups().contains(g)) {
                clash = true;
                break;
              }
            }
          }
        }
        if (clash) {
          continue;
        }
        List<Affix> out = new ArrayList<>(af);
        out.set(out.indexOf(from), to);
        conv[ci] = PoeCraftCalcService.matched(it.with(it.rarity(), out), goal) > met ? '2' : '1';
        anyConv = true;
      }
      return anyConv ? "|c" + new String(conv) : "";
    }

    /** 바꾼 옵션(엔진을 원본 옵션 하나짜리 아이템에 한 번 — 원본 id 마다 기억, 결정적). 바꿀 수 없으면 null. */
    private Affix convertTo(int ci, Affix from, Item it) {
      String mk = ci + "|" + from.id();
      Affix hit = convertMemo.get(mk);
      if (hit != null) {
        return hit == NO_AFFIX ? null : hit;
      }
      PoeCraftEngine.Result r =
          engine.apply(
              it.with(Rarity.RARE, List.of(from)),
              Action.HARVEST,
              converts.get(ci).arg(),
              localRng.get());
      Affix to =
          r.error() != null || r.item().affixes().isEmpty() ? null : r.item().affixes().get(0);
      convertMemo.put(mk, to == null ? NO_AFFIX : to);
      return to;
    }

    /** 해제 고르기(C459) — 제시 셋 중 목표에 드는 것, 없으면 무작위(사람이 고르는 것처럼). 제시가 없으면 null. */
    String unveilPick(Item it) {
      List<String> offers = engine.unveilOffers(it);
      if (offers.isEmpty()) {
        return null;
      }
      for (String o : offers) {
        if (idMask.containsKey(o)) {
          return o;
        }
      }
      return offers.get(localRng.get().nextInt(offers.size()));
    }

    /** 열쇠의 맞은 목표 비트(첫 '|' 뒤 칸). */
    int maskOf(String key) {
      int bar = key.indexOf('|');
      return Integer.parseInt(key.substring(bar + 1, key.indexOf('|', bar + 1)));
    }

    boolean goalKey(String k) {
      if (Integer.parseInt(k.split("\\|")[1]) != (1 << goal.size()) - 1) {
        return false;
      }
      if (openNeed[0] + openNeed[1] == 0) {
        return true;
      }
      // 빈 자리 조건(C520) — 열쇠 끝 '|o접두수,접미수'(각인 열쇠는 '#' 앞), 매직 쪽당 1 · 그 밖 레어 한도
      int hash = k.indexOf('#');
      String main = hash < 0 ? k : k.substring(0, hash);
      int o = main.lastIndexOf("|o");
      if (o < 0) {
        return false;
      }
      String[] ps = main.substring(o + 2).split(",");
      int cap = main.charAt(0) == 'M' ? 1 : openCap;
      return cap - Integer.parseInt(ps[0]) >= openNeed[0]
          && cap - Integer.parseInt(ps[1]) >= openNeed[1];
    }

    /** 수단 a 를 상태 k(대표 아이템)에 쓴 결과 분포 — 쓸 수 없으면 null. 기억 없는 다시 굴리기는 남는 옵션이 같으면 결과 분포가 같아 함께 쓴다. */
    Map<String, Double> sample(String k, int ai) {
      return sample(k, ai, SAMPLE_STEP);
    }

    /** step = 한 걸음 수단의 표본 수(다듬기는 더 많이). */
    Map<String, Double> sample(String k, int ai, int step) {
      Item it = rep.get(k);
      Act a = acts.get(ai);
      // 각인(C468) — 열쇠 '#' 뒤가 각인한 상태. 각인은 매직 · 각인 없을 때만, 되돌리기는 각인이 있을 때(각인은 사라진다)
      int hash = k.indexOf('#');
      String imp = hash < 0 ? null : k.substring(hash + 1);
      if (IMPRINT.equals(a.id())) {
        if (imp != null || it.rarity() != Rarity.MAGIC || it.corrupted()) {
          return null;
        }
        rep.putIfAbsent(k + "#" + k, it);
        return Map.of(k + "#" + k, 1d);
      }
      if (RESTORE.equals(a.id())) {
        return imp == null ? null : Map.of(imp, 1d);
      }
      String impTail = imp == null ? "" : "#" + imp;
      boolean reroll = rerolls(a);
      String ck = null;
      if (reroll) {
        StringBuilder sb = new StringBuilder(a.id()).append('|').append(it.rarity()).append('|');
        PoeCraftEngine.kept(it).stream()
            .map(x -> x.id() + (x.crafted() ? "c" : ""))
            .sorted()
            .forEach(x -> sb.append(x).append(','));
        ck = sb.append(impTail).toString();
        Map<String, Double> hit = rerollCache.get(ck);
        if (hit != null) {
          // '제자리뿐'은 상태마다 판정(C461b — 처음 뜬 상태 기준으로 저장하면 다른 상태에서도 빠졌다)
          return hit.isEmpty() || (hit.size() == 1 && hit.containsKey(k)) ? null : hit;
        }
      }
      int n = reroll ? SAMPLE_REROLL : step;
      int curMask = maskOf(k);
      int newHits = 0;
      Map<String, Double> out = new HashMap<>();
      int ok = 0;
      for (int i = 0; i < n; i++) {
        String arg = a.arg();
        if (a.action() == Action.UNVEIL) {
          arg = unveilPick(it);
          if (arg == null) {
            break;
          }
        }
        // 한 번 넓히기가 길어 예산을 넘지 않게(C457k — 9.9초) — 모자란 표본도 그대로 쓴다
        if ((i & 255) == 255 && System.currentTimeMillis() > deadline) {
          break;
        }
        PoeCraftEngine.Result r = engine.apply(it, a.action(), arg, localRng.get());
        if (r.error() != null) {
          if (i >= 20 && ok == 0) {
            break; // 이 상태엔 못 쓴다
          }
          continue;
        }
        ok++;
        String nk = key(r.item()) + impTail;
        rep.putIfAbsent(nk, r.item());
        out.merge(nk, 1d, Double::sum);
        // 드문 결과(C472d) — 목표를 새로 맞힌 결과가 RARE_HITS 번 나올 때까지 다시 굴리기 표본을 늘린다(최대 10배). 1/5,000 결과를 2~3만
        // 번으로
        // 재면 몇 번뿐이라 운 좋게 높게 뜬 수단이 뽑혀 계획이 모의의 절반으로 나왔다
        if ((maskOf(nk) & ~curMask) != 0) {
          newHits++;
        }
        if (reroll && i == n - 1 && newHits < RARE_HITS && n < 10 * SAMPLE_REROLL) {
          n = Math.min(n * 2, 10 * SAMPLE_REROLL);
        }
        // 한 걸음 수단도(C473) — 목표를 새로 맞힌 결과가 1~39번이면 최대 8배(0번이면 그대로 — 넓히기 시간). 드문 엑잘 결과를 300번으로 재면 계획이
        // 모의보다 낙관적이었다(재조합 사례 계획 12.8 ↔ 모의 19~34div)
        if (!reroll && i == n - 1 && newHits > 0 && newHits < RARE_HITS && n < 8 * step) {
          n = Math.min(n * 2, 8 * step);
        }
        // 정해진 결과(제작대 등 — 처음 40번이 모두 같은 상태)면 더 뜨지 않는다(C457i: 상태마다 300번씩 떠 넓히기가 느렸다)
        if (!reroll && ok == 40 && out.size() == 1) {
          break;
        }
      }
      if (ok == 0) {
        if (ck != null) {
          rerollCache.put(ck, Map.of());
        }
        return null;
      }
      final double okD = ok;
      out.replaceAll((x, v) -> v / okD);
      if (ck != null) {
        rerollCache.put(ck, out);
      }
      // 결과가 그대로인 수단(같은 상태로만 감)은 버린다 — 분포 저장과 따로(C461b)
      if (out.size() == 1 && out.containsKey(k)) {
        return null;
      }
      return out;
    }
  }

  /** 기억 없는 다시 굴리기인가 — 수확은 '태그 옵션 포함 재련'만(C466: 바꾸기 · 추가/제거 · 같은 유형 재련은 지금 옵션에 달린다). */
  private boolean rerolls(Act act) {
    if (act.action() == Action.HARVEST) {
      PoeCraftDataService.Harvest h =
          craft.harvests().stream().filter(x -> x.id().equals(act.arg())).findFirst().orElse(null);
      return h != null
          && ("reroll_with_mod".equals(h.command())
              || "reroll_with_influence_mod".equals(h.command()));
    }
    return rerolls(act.action());
  }

  private static boolean rerolls(Action a) {
    return switch (a) {
      case CHAOS, ALTERATION, ESSENCE, FOSSIL, ALCHEMY, TRANSMUTE, HARVEST, SCOUR -> true;
      default -> false;
    };
  }

  public Result optimize(Request req) {
    return optimize(req, null);
  }

  /** jobId 가 있으면 끝난 정책을 보관한다(C464 — 에뮬레이터 따라 하기). */
  private Result optimize(Request req, String jobId) {
    long started = System.currentTimeMillis();
    // 빈 자리 조건(C520)은 옵션 목표와 나눠 Job 에 — 옵션 목표가 하나는 있어야
    int[] openNeed =
        PoeCraftCalcService.openNeed(req.targets() == null ? List.of() : req.targets());
    List<Target> allReal =
        PoeCraftCalcService.real(req.targets() == null ? List.of() : req.targets());
    // 엘드리치 임플리싯 목표(C549) — 명시 옵션 제작과 무관해 따로 재서 붙인다(정책은 명시 옵션 목표로만)
    List<Target> eldTargets = allReal.stream().filter(calc::eldritchTarget).toList();
    List<Target> realTargets = allReal.stream().filter(t -> !calc.eldritchTarget(t)).toList();
    if (realTargets.isEmpty() && !eldTargets.isEmpty()) {
      return error("명시 옵션(접두 · 접미) 목표가 없습니다 — 엘드리치 임플리싯 비용은 계산기 표 위 '엘드리치 임플리싯' 줄");
    }
    if (realTargets.isEmpty()) {
      return error("목표 옵션을 하나 이상 고르세요");
    }
    if (realTargets.size() > 6) {
      return error("목표는 여섯 개까지입니다");
    }
    Item start = craft.normalize(req.item());
    // 쪽마다 자리 검사(C471e) — 한쪽(접두/접미)에만 붙는 목표가 그쪽 자리 수보다 많으면 동시에 채울 수 없다(접미 저항 넷 — 정책이 넘김뿐인 1e9 결과를
    // '모의 400/400 성공'으로 보였다)
    int onlyPre = 0;
    int onlySuf = 0;
    for (Target t : realTargets) {
      java.util.Set<String> gens = new java.util.HashSet<>();
      for (String id : t.anyOf()) {
        PoeCraftDataService.CraftMod md = craft.mod(id);
        String g = md == null ? null : md.g();
        if (g != null) {
          gens.add(g);
        }
      }
      if (gens.equals(java.util.Set.of("prefix"))) {
        onlyPre++;
      } else if (gens.equals(java.util.Set.of("suffix"))) {
        onlySuf++;
      }
    }
    int perSide = PoeCraftEngine.maxPerGen(start, Rarity.RARE);
    if (onlyPre + openNeed[0] > perSide || onlySuf + openNeed[1] > perSide) {
      // 빈 자리 조건도 그 쪽 자리를 쓴다(C520)
      return error(
          "목표를 동시에 채울 수 없습니다 — 접두 목표 "
              + onlyPre
              + "개"
              // 빈 자리 조건도 같이 적는다(C540 — 판정은 빈 자리까지 세는데 문구엔 목표 수만 나와 '접두 1개, 쪽마다 3자리'처럼 앞뒤가 안 맞았다)
              + (openNeed[0] > 0 ? "(+빈 자리 " + openNeed[0] + ")" : "")
              + " · 접미 목표 "
              + onlySuf
              + "개"
              + (openNeed[1] > 0 ? "(+빈 자리 " + openNeed[1] + ")" : "")
              + ", 레어는 쪽마다 "
              + perSide
              + "자리");
    }
    if (start.corrupted()) {
      return error("타락한 아이템은 바꿀 수 없습니다");
    }
    long budget =
        Math.max(
            2_000, Math.min(40_000, req.budgetMs() == null ? DEFAULT_BUDGET_MS : req.budgetMs()));
    String cls = start.itemClass();
    PoeCraftService.Options o = craft.options(cls, start.variant() == null ? "" : start.variant());
    List<Target> targets = realTargets;
    // 목표로 치는 옵션 — 제작대 · 에센스 보장 옵션도(같은 그룹 · 문구 · 최소 굴림이 목표 최하 등급 이상)
    List<List<String>> extra = new ArrayList<>();
    targets.forEach(t -> extra.add(new ArrayList<>(t.anyOf())));
    List<Act> acts = new ArrayList<>();
    List<PoeCraftDataService.BenchCraft> bench = o.bench() == null ? List.of() : o.bench();
    for (int t = 0; t < targets.size(); t++) {
      PoeCraftDataService.BenchCraft best = null;
      double bestCost = Double.MAX_VALUE;
      for (PoeCraftDataService.BenchCraft b : bench) {
        if (b.modId() == null || !calc.benchCounts(b, targets.get(t))) {
          continue;
        }
        Act a =
            act("BENCH:" + b.modId(), Action.BENCH, b.modId(), "제작대: " + text(b.ko(), b.en()), cls);
        if (a != null && a.cost() < bestCost) {
          best = b;
          bestCost = a.cost();
        }
      }
      if (best != null) {
        extra.get(t).add(best.modId());
        acts.add(
            act(
                "BENCH:" + best.modId(),
                Action.BENCH,
                best.modId(),
                "제작대: " + text(best.ko(), best.en()),
                cls));
      }
    }
    Map<String, PoeCraftDataService.Essence> ess = new LinkedHashMap<>();
    for (PoeCraftDataService.Essence e : craft.essences(cls)) {
      if (e.modId() == null
          || e.slug() == null
          || (e.ilvlMax() > 0 && start.ilvl() > e.ilvlMax())) {
        continue;
      }
      boolean counts = false;
      for (int t = 0; t < targets.size(); t++) {
        if (calc.essenceCounts(e, targets.get(t))) {
          counts = true;
        }
      }
      if (!counts) {
        continue;
      }
      // 가문마다 목표를 채우는 등급 중 가장 싼 것(C459 — 보장 옵션 말고는 결과가 같다)
      PoeCraftDataService.Essence cur = ess.get(e.family());
      Double pe = prices.chaos(e.name());
      Double pc = cur == null ? null : prices.chaos(cur.name());
      if (cur == null || (pe != null && (pc == null || pe < pc))) {
        ess.put(e.family(), e);
      }
    }
    for (PoeCraftDataService.Essence e : ess.values()) {
      for (int t = 0; t < targets.size(); t++) {
        if (calc.essenceCounts(e, targets.get(t)) && !extra.get(t).contains(e.modId())) {
          extra.get(t).add(e.modId());
        }
      }
      acts.add(act("ESSENCE:" + e.slug(), Action.ESSENCE, e.slug(), e.nameKo(), cls));
    }
    List<Target> goal = extra.stream().map(Target::new).toList();
    // 기본 화폐
    acts.add(act("TRANSMUTE", Action.TRANSMUTE, null, "진화의 오브", cls));
    acts.add(act("ALTERATION", Action.ALTERATION, null, "변화의 오브", cls));
    acts.add(act("AUGMENTATION", Action.AUGMENTATION, null, "확장의 오브", cls));
    acts.add(act("REGAL", Action.REGAL, null, "제왕의 오브", cls));
    acts.add(act("ALCHEMY", Action.ALCHEMY, null, "연금술의 오브", cls));
    acts.add(act("CHAOS", Action.CHAOS, null, "카오스 오브", cls));
    acts.add(act("EXALT", Action.EXALT, null, "엑잘티드 오브", cls));
    acts.add(act("ANNUL", Action.ANNUL, null, "소멸의 오브", cls));
    acts.add(act("SCOUR", Action.SCOUR, null, "정제의 오브", cls));
    // 분열의 오브(C465) — 옵션 4개 이상 레어(영향력 · 이미 분열이면 엔진이 거절)
    acts.add(act("FRACTURE", Action.FRACTURE, null, "분열의 오브", cls));
    // 삿된 화폐(C459) — 레벨 높은 속성 가중
    acts.add(act("FOULBORN_AUG", Action.FOULBORN_AUG, null, "삿된 확장의 오브", cls));
    acts.add(act("FOULBORN_REGAL", Action.FOULBORN_REGAL, null, "삿된 제왕의 오브", cls));
    acts.add(act("FOULBORN_EXALT", Action.FOULBORN_EXALT, null, "삿된 엑잘티드 오브", cls));
    // 장막(C459) — 장막 카오스(장막 옵션 하나 보장) · 장막 엑잘 · 해제(제시 셋 중 목표 먼저)
    acts.add(act("VEILED_CHAOS", Action.VEILED_CHAOS, null, "장막의 카오스 오브", cls));
    acts.add(act("VEILED_EXALT", Action.VEILED_EXALT, null, "장막의 엑잘티드 오브", cls));
    acts.add(act("UNVEIL", Action.UNVEIL, null, "장막 해제(목표 먼저)", cls));
    // 엘드리치(C459) — 갑옷 · 장화 · 장갑 · 투구, 영향력 없는 아이템. 불씨 · 영액으로 우세를 만들고 섬뜩한 화폐는 우세한 쪽만
    if (PoeCraftEngine.ELDRITCH_CLASSES.contains(cls)
        && (start.influence() == null || start.influence().isEmpty())) {
      acts.add(act("EMBER:1", Action.EMBER, "1", "하급 섬뜩한 불씨", cls));
      acts.add(act("ICHOR:1", Action.ICHOR, "1", "하급 섬뜩한 영액", cls));
      acts.add(act("ELDRITCH_CHAOS", Action.ELDRITCH_CHAOS, null, "섬뜩한 카오스 오브", cls));
      acts.add(act("ELDRITCH_EXALT", Action.ELDRITCH_EXALT, null, "섬뜩한 엑잘티드 오브", cls));
      acts.add(act("ELDRITCH_ANNUL", Action.ELDRITCH_ANNUL, null, "섬뜩한 소멸의 오브", cls));
    }
    // 화석 · 수확 — 계산기가 고르는 것과 같은 상위 조합(짧게)
    long tFossil = System.currentTimeMillis();
    // 계산기 표(위 경계용)를 먼저 — 표가 이미 화석 조합을 골라 두니 화석 탐색을 따로 다시 하지 않는다(C469d: 준비 9.5초 중 절반이 중복)
    PoeCraftCalcService.Response table = null;
    if (req.upper() == null && req.methods() == null) {
      try {
        // 빈 자리 조건(@open)까지 넣은 표(C540 — 옵션 목표만 넘겨 위 경계가 빈 자리 조건 없는 싼 값으로 잡혔다: 넘김 값 · 수단 거르기 · '한 가지
        // 방법
        // 최저'가 위 계산기 표와 어긋남)
        table = calc.calc(new PoeCraftCalcService.Request(start, req.targets(), null, null, null));
      } catch (RuntimeException e) {
        // 표가 실패하면 화석 탐색 · 연타 위 경계로
      }
    }
    List<String[]> picked = new ArrayList<>();
    if (req.methods() != null) {
      for (String m : req.methods()) {
        if (m != null && (m.startsWith("FOSSIL:") || m.startsWith("HARVEST:"))) {
          picked.add(new String[] {m, m});
        }
      }
    } else if (table != null) {
      for (PoeCraftCalcService.MethodResult r : table.results()) {
        if (r.method() != null && r.method().startsWith("FOSSIL:")) {
          picked.add(new String[] {r.method(), r.label()});
        }
      }
    } else {
      for (PoeCraftCalcService.MethodResult r : calc.fossils(start, targets, 4_000)) {
        picked.add(new String[] {r.method(), r.label()});
      }
    }
    // 수확 재련(C459) — 목표 태그에 맞는 것 전부(표의 상위 둘만이 아니라)
    java.util.Set<String> tags = calc.targetTags(targets);
    for (PoeCraftDataService.Harvest h : craft.harvests()) {
      String tag = h.params() == null ? "" : h.params().split("\\s+")[0];
      // 나머지 수확(C466) — 원소 바꾸기(CONVERT 뒤 원소가 목표 태그) · 유형 옵션 추가 + 무작위 제거(추가 태그가 목표 태그) · 같은 유형
      // 더/덜 나오게 재련(늘). 쓸 수 없는 상태(영향력 · 바꿀 옵션 없음)는 엔진이 거절해 전이에 안 들어간다
      String[] hp = h.params() == null ? new String[0] : h.params().trim().split("\s+");
      boolean otherHarvest =
          switch (h.command() == null ? "" : h.command()) {
            case "convert_mod" -> hp.length > 0 && tags.contains(hp[hp.length - 1]);
            case "remove_type_and_add_type_mod" -> hp.length > 2 && tags.contains(hp[2]);
            case "reroll_with_current_tags_affinity_multiplier" -> true;
            default -> false;
          };
      if ((otherHarvest || "reroll_with_mod".equals(h.command()) && tags.contains(tag))
          && picked.stream().noneMatch(x -> x[0].equals("HARVEST:" + h.id()))) {
        picked.add(new String[] {"HARVEST:" + h.id(), "HARVEST:" + h.id()});
      }
    }
    for (String[] m : picked) {
      boolean fossil = m[0].startsWith("FOSSIL:");
      String arg = m[0].substring(m[0].indexOf(':') + 1);
      acts.add(
          act(
              m[0],
              fossil ? Action.FOSSIL : Action.HARVEST,
              arg,
              m[1].equals(m[0]) ? methodKo(fossil, arg) : m[1],
              cls));
    }
    // 메타 제작 · 제작대 옵션 지우기 · 짐승 맞바꾸기
    String lockPre = null;
    String lockSuf = null;
    String multi = null;
    for (PoeCraftDataService.BenchCraft b : bench) {
      String id = b.modId() == null ? "" : b.modId();
      if (id.endsWith("ItemGenerationCannotChangePrefixes")) {
        lockPre = id;
        acts.add(act("BENCH:" + id, Action.BENCH, id, "제작대: 접두어 변경 불가", cls));
      } else if (id.endsWith("ItemGenerationCannotChangeSuffixes")) {
        lockSuf = id;
        acts.add(act("BENCH:" + id, Action.BENCH, id, "제작대: 접미어 변경 불가", cls));
      } else if (id.endsWith("ItemGenerationCanHaveMultipleCraftedMods")) {
        multi = id;
        acts.add(act("BENCH:" + id, Action.BENCH, id, "제작대: 제작 속성 최대 3개", cls));
      }
    }
    acts.add(act("BENCH_REMOVE", Action.BENCH_REMOVE, null, "제작대: 제작 속성 지우기", cls));
    // 영향력 아이템 — 짐승 '그 영향력 아이템에 속성 추가'(C463, BestiaryRecipes EinharMasterCraft42~47, 엑잘처럼 그 아이템 풀에서
    // 하나)
    Map<String, String> infKo =
        Map.of(
            "shaper",
            "쉐이퍼",
            "elder",
            "엘더",
            "crusader",
            "십자군",
            "eyrie",
            "대속자",
            "basilisk",
            "사냥꾼",
            "adjudicator",
            "전쟁군주");
    for (String inf :
        start.influence() == null || start.influence().isBlank()
            ? new String[0]
            : start.influence().split(",")) {
      acts.add(
          act(
              "BEAST:influence:" + inf,
              Action.BEAST,
              "influence:" + inf,
              "짐승: " + infKo.getOrDefault(inf, inf) + " 아이템에 속성 추가",
              cls));
    }
    acts.add(act("BEAST:swap-prefix", Action.BEAST, "swap-prefix", "짐승: 접두어 추가, 무작위 접미어 제거", cls));
    acts.add(act("BEAST:swap-suffix", Action.BEAST, "swap-suffix", "짐승: 접미어 추가, 무작위 접두어 제거", cls));
    // 각인(C468) — 매직 아이템 저장 · 되돌리기(되돌리기는 화폐가 안 든다 — 최소 비용)
    acts.add(act(IMPRINT, Action.BEAST, "imprint", "짐승: 각인(매직 아이템 저장)", cls));
    acts.add(new Act(RESTORE, Action.BEAST, "restore", "각인으로 되돌리기", Map.of(), MIN_COST));
    List<String> skipped = new ArrayList<>();
    List<Act> usable = new ArrayList<>();
    for (Act a : acts) {
      if (a == null) {
        continue;
      }
      if (Double.isNaN(a.cost())) {
        skipped.add(a.ko());
      } else if (usable.stream().noneMatch(x -> x.id().equals(a.id()))) {
        usable.add(a);
      }
    }
    Job job = new Job(goal, lockPre, lockSuf, multi, usable, started + budget * 92 / 100);
    job.openNeed = openNeed;
    job.openCap = perSide;
    job.eldritch = usable.stream().anyMatch(x -> x.action() == Action.ELDRITCH_CHAOS);
    for (Act a : usable) {
      if (a.action() != Action.HARVEST) {
        continue;
      }
      craft.harvests().stream()
          .filter(h -> h.id().equals(a.arg()) && "convert_mod".equals(h.command()))
          .findFirst()
          .ifPresent(
              h -> {
                List<String> ps = java.util.Arrays.asList(h.params().trim().split(" +"));
                int cv = ps.indexOf("CONVERT");
                if (cv > 0) {
                  job.converts.add(a);
                  job.convertSrc.add(new java.util.HashSet<>(ps.subList(0, cv)));
                }
              });
    }
    // 위 경계 — 정제 + 아는 가장 싼 방법. 계산기 표 최저가 없으면 카오스 · 변화(+확장) · 에센스 연타를 표본으로 잰다
    double scour =
        usable.stream()
            .filter(x -> x.action() == Action.SCOUR)
            .mapToDouble(Act::cost)
            .findFirst()
            .orElse(1);
    double known = req.upper() != null && req.upper() > 0 ? req.upper() : spamUpper(job, start);
    if (table != null) {
      // 계산기 표의 최저도 위 경계로(C462 — 표와 동시에 시작해 표 값을 못 받는다, 최적 조합이 표보다 비싸게 나오지 않게)
      PoeCraftCalcService.MethodResult top =
          table.results().stream().filter(x -> x.expectedCost() != null).findFirst().orElse(null);
      if (top != null && top.expectedCost() < known) {
        known = top.expectedCost();
        job.upperLabel = top.label();
      }
    }
    job.upper = known + scour;
    job.restartKey = job.key(PoeCraftCalcService.normal(start));
    job.restartCost = scour;
    // 넘김 값 시작 — 위 경계가 아주 크면(아는 방법 없음 1e9) 1e7 에서(기울기를 빼는 자릿수 손실, 근보다 작으면 뉴턴이 한 번에 넘어 위에서 내려온다)
    job.effUpper = Math.min(job.upper, 1e7);
    // 한 번 비용이 위 경계 이상인 수단은 뺀다(C459d — 한 번만 써도 '정제 → 아는 최저'보다 비싸다, 넓히기 시간만 든다)
    final double cap = job.upper;
    usable.removeIf(x -> x.action() != Action.SCOUR && x.cost() >= cap);
    if (req.upper() != null && req.upper() > 0) {
      job.upperLabel =
          req.upperLabel() == null || req.upperLabel().isBlank()
              ? "계산기 표의 가장 싼 방법"
              : req.upperLabel();
    }
    String s0 = job.key(start);
    job.rep.put(s0, start);
    // 1) LAO* 식 탐색 — 넓힌 상태에서 가치 반복(자기 자신으로 돌아오는 몫은 닫힌 식) → 그 정책으로 처음 상태에서 닿는
    //    넓히지 않은 상태를 넓힘. 넓힐 것이 없으면(정책이 닿는 곳이 모두 넓혀짐) 그 정책이 이 상태 근사에서 최적이다.
    //    넓히지 않은 상태 값은 위 경계(정제 + 아는 최저) — 아는 최저보다 나을 때만 모르는 곳으로 간다
    Map<String, Double> v = new HashMap<>();
    Map<String, Integer> pol = new HashMap<>();
    int rounds = 0;
    boolean capped = false;
    boolean converged = false;
    long learnEnd = started + budget * 75 / 100;
    // 탐색 안내용 낙관 값(넓히지 않은 상태 0) — 위 경계만 쓰면 모르는 곳이 늘 비싸 보여 넓히지 않는다
    Map<String, Double> vLo = new HashMap<>();
    long tVi = 0;
    long tExp = 0;
    Map<String, Integer> polLo = new HashMap<>();
    long tLearn = System.currentTimeMillis();
    expand(job, s0);
    while (System.currentTimeMillis() < learnEnd) {
      rounds++;
      long tv0 = System.currentTimeMillis();
      decide(job, v, pol, learnEnd);
      valueIteration(job, vLo, polLo, learnEnd, 0, false);
      tVi += System.currentTimeMillis() - tv0;
      // 결정 정책이 닿는 넓히지 않은 상태(다듬기) + 낙관 정책이 닿는 것(찾기), 반씩
      List<String> frontier = frontier(job, s0, pol, EXPAND_PER_ROUND / 2, new ArrayList<>());
      frontier = frontier(job, s0, polLo, EXPAND_PER_ROUND, frontier);
      if (frontier.isEmpty()) {
        converged = true;
        break;
      }
      // 넓힐 상태들을 나란히(C459e — 상태 하나에 수단 수십 개 × 표본 수백 번, 한 줄로는 상태당 ~30ms)
      final long until = learnEnd;
      long te0 = System.currentTimeMillis();
      boolean[] hitCap = {false};
      frontier.parallelStream()
          .forEach(
              k -> {
                // 상태 한도와 시간 한도를 가른다(C538 — 시간이 다 돼도 '상태 한도로 더 넓히지 못함'이라 적어 상태 1,520개(한도 5,000)에도
                // 그렇게 보였다). 시간이면 바깥 반복이 끝난다
                if (job.trans.size() >= MAX_STATES) {
                  hitCap[0] = true;
                  return;
                }
                if (System.currentTimeMillis() >= until) {
                  return;
                }
                expand(job, k);
              });
      capped = hitCap[0];
      tExp += System.currentTimeMillis() - te0;
      if (capped) {
        break;
      }
    }
    decide(job, v, pol, learnEnd);
    long tRefine = System.currentTimeMillis();
    int refined = refine(job, s0, v, pol, started + budget * 85 / 100);
    // 다듬기 뒤 가치 반복은 시간 한도가 이미 지나 16번만 돌기도 했다 — 덜 수렴한 값으로 정책이 상태 사이를 돌았다(C469f), 1.5초 보장
    decide(job, v, pol, System.currentTimeMillis() + 1_500);
    tRefine = System.currentTimeMillis() - tRefine;
    long tSim = System.currentTimeMillis();
    Double planCost = job.goalKey(s0) ? Double.valueOf(0) : v.get(s0);
    if (planCost == null || pol.get(s0) == null) {
      return error("목표까지 가는 길을 찾지 못했습니다(시간 안에 넓힌 상태 " + job.trans.size() + "개)");
    }
    // 3) 정책을 실제 아이템에 굴려 비용 재기 — 열쇠가 표에 없으면 그 자리에서 표본(시간 안) 또는 처음 상태의 정책
    Map<String, Double> spent = new LinkedHashMap<>();
    Map<String, Integer> visits = new HashMap<>();
    List<List<Integer>> paths = new ArrayList<>();
    List<Double> runCost = new ArrayList<>();
    // 목표에 실제로 닿은 회차의 비용(C540) — 분포(운 좋으면 · 보통 · 운 나쁘면)는 이것만. 넘김 · 고리 감시 회차는 '아는 최저'의 기대 비용을 덩어리로
    // 더해 분포처럼 보였다
    List<Double> reachedCost = new ArrayList<>();
    // 성공 회차의 마지막 아이템(C502 결과 예시) — runCost 와 같은 순서
    List<Item> finals = new ArrayList<>();
    int runs = 0;
    int succ = 0;
    int fallbacks = 0;
    int cycles = 0;
    // 시간 뒤 내다본 상태 수(C469l — 상한 LATE_LOOKS, 없으면 모의가 20초)
    int lateLooks = 0;
    int cycleBreaks = 0;
    // 넘김 대신 정제 → 이 정책으로 처음부터(C469j)
    // 넘김 이름(C471) — 다시 시작 상한이 위 경계보다 싸면 '처음부터 이 조합', 아니면 아는 최저 방법
    String fbLabel = job.effUpper < job.upper ? "처음부터 다시(이 조합)" : job.upperLabel;
    int policyRestarts = 0;
    // 다시 시작 한도에 못 끝낸 회차(C536, PoE2 C472c 와 같게)
    int unfinished = 0;
    Map<String, Integer> fallbackWhy = new HashMap<>();
    int scourAct = -1;
    for (int i = 0; i < usable.size(); i++) {
      if (usable.get(i).action() == Action.SCOUR) {
        scourAct = i;
      }
    }
    // 고리 끊기로 상태마다 금지한 수단(C469i)
    Map<String, java.util.Set<Integer>> banned = new HashMap<>();
    int replaced = 0;
    // 진단(C469) — 실제 아이템에 안 통한 정책 수단과 그 까닭
    Map<String, Integer> failedActs = new HashMap<>();
    String diag = "";
    int stuckStates = 0;
    double failCost = 0;
    long simEnd = started + budget;
    // 하드 한도(C472f) — 최소 30회를 채우느라 예산을 크게 넘기지 않게(다시 시작이 많은 목표에서 2분 넘게 걸렸다)
    long simHard = simEnd + SIM_GRACE_MS;
    for (;
        runs < SIM_RUNS
            && (runs < 30 || System.currentTimeMillis() < simEnd)
            && System.currentTimeMillis() < simHard;
        runs++) {
      Item it = start;
      // 각인(C468) — 각인한 상태 열쇠와 아이템
      String imp = null;
      Item impItem = null;
      double c = 0;
      List<Integer> path = new ArrayList<>();
      // 이 회차에 지난 상태(지나는 회차 비율 — 한 회차에 여러 번 지나도 한 번)
      java.util.Set<String> seenRun = new java.util.HashSet<>();
      boolean done = false;
      ArrayDeque<String> trace = new ArrayDeque<>();
      int sameRun = 0;
      int steps = 0;
      Map<String, Integer> runCount = new HashMap<>();
      int restartRun = 0;
      // 목표에 실제로 닿았나(C536) — 넘김(아는 최저로) 회차의 마지막 아이템은 목표 아이템이 아니라 결과 예시에서 뺀다
      boolean reached = false;
      for (int step = 0; step < SIM_STEPS; step++) {
        steps = step;
        if ((step & 4095) == 4095 && System.currentTimeMillis() > simHard) {
          break; // 하드 한도 — 못 끝낸 회차(쓴 비용은 실패 비용으로)
        }
        String k = job.key(it) + (imp == null ? "" : "#" + imp);
        if (trace.size() >= 40) {
          trace.poll();
        }
        trace.add(k + "→" + (pol.get(k) == null ? "?" : label(usable, pol.get(k), "")));
        if (job.goalKey(k)) {
          done = true;
          reached = true;
          break;
        }
        if (seenRun.add(k)) {
          visits.merge(k, 1, Integer::sum);
        }
        // 고리 감시(C469f) — 한 회차 비용이 계획의 CYCLE_FACTOR 배와 위 경계를 넘으면 정책이 상태 사이를 도는 것으로 보고 넘김(정상 연타가 그만큼
        // 길어질 확률은 e^-CYCLE_FACTOR 정도)
        if (c > CYCLE_FACTOR * planCost && c > Math.min(job.upper, job.effUpper)) {
          if (diag.isEmpty()) {
            diag = "고리 · 걸음 " + step + " · 비용 " + Math.round(c) + " · " + String.join(" ; ", trace);
          }
          c += job.upper;
          fallbacks++;
          cycles++;
          path.add(FALLBACK);
          done = true;
          break;
        }
        Integer ai = pol.get(k);
        // 최소 30회 안에서는 시간이 지나도 내다본다(C469l — 시간이 다 된 뒤 모르는 상태를 바로 넘김(위 경계 전부)으로 셈해 극단 목표가
        // 계획의 10배로 나왔다)
        boolean late = System.currentTimeMillis() >= simEnd;
        if (ai == null && (!late || (runs < 30 && lateLooks < LATE_LOOKS))) {
          if (late) {
            lateLooks++;
          }
          // 넓히지 못한 상태 — 그 자리에서 수단마다 표본을 떠 한 걸음 내다보기(남은 값은 아는 상태 값, 모르면 처음부터 다시)
          job.rep.putIfAbsent(k, it);
          double best = Double.MAX_VALUE;
          for (int x = 0; x < usable.size(); x++) {
            Map<String, Double> t = job.sample(k, x);
            if (t == null) {
              continue;
            }
            double self = t.getOrDefault(k, 0d);
            if (self >= 0.999999) {
              continue;
            }
            double rest = 0;
            for (Map.Entry<String, Double> sp : t.entrySet()) {
              if (!sp.getKey().equals(k) && !job.goalKey(sp.getKey())) {
                Double nv = v.get(sp.getKey());
                rest += sp.getValue() * (nv != null ? nv : job.effUpper);
              }
            }
            double qv = (usable.get(x).cost() + rest) / (1 - self);
            if (qv < best) {
              best = qv;
              ai = x;
            }
          }
          if (ai != null && best >= job.effUpper) {
            ai = FALLBACK;
          }
          if (ai != null) {
            pol.put(k, ai);
          }
        }
        // 넘김을 실제로(C469j) — 정책이 '정제 → 아는 최저'인데 정제한 아이템 상태의 값이 알려져 있고 정제 + 그 값이 위 경계보다 싸면, 정제하고 이
        // 정책으로 처음부터(못 넓힌 영역에 들어간 회차가 위 경계를 통째로 더해 극단 목표가 계획의 5~10배로 나왔다)
        if (ai != null && ai == FALLBACK && scourAct >= 0 && restartRun < RESTART_RUNS) {
          PoeCraftEngine.Result sr = job.engine.apply(it, Action.SCOUR, null, job.rng);
          if (sr.error() == null) {
            String ks = job.key(sr.item()) + (imp == null ? "" : "#" + imp);
            Double vs = v.get(ks);
            if (vs != null && !ks.equals(k) && usable.get(scourAct).cost() + vs < job.upper) {
              ai = scourAct;
              restartRun++;
              policyRestarts++;
            } else {
              fallbackWhy.merge(
                  "처음부터 못 함(" + (vs == null ? "값 없음 " + ks : "값 " + Math.round(vs)) + ")",
                  1,
                  Integer::sum);
            }
          } else {
            fallbackWhy.merge("정제 실패 " + sr.error(), 1, Integer::sum);
          }
        }
        // 끝내지 못한 회차(C536, PoE2 C472c 와 같게) — 처음부터 다시를 한도만큼 해도 못 닿으면 쓴 비용만 실패 비용으로 더하고 성공으로 세지
        // 않는다(전체 비용 / 성공 수). 넘김 값(다시 시작 + 처음 상태 값)을 더하면 계획 값이 그대로 돌아와 확인이 안 됐다(10-21 ⑬: 다시 시작
        // 6,000번에 목표 0번인데 '성공 30/30 · 5,686c ±52', 결과 예시는 목표 밖 아이템)
        if (ai != null
            && ai == FALLBACK
            && restartRun >= RESTART_RUNS
            && job.effUpper < job.upper) {
          unfinished++;
          break;
        }
        // 고리 끊기(C469g) — 결과가 정해진 수단(제작대 만들기 · 지우기 등)을 한 회차에 같은 상태에서 20번 넘게 쓰면 상태 근사 차이로 두 상태를
        // 오가는 것(대표 아이템과 실제 아이템의 다음 열쇠가 다름). 그 상태의 결정을 다른 수단 중 Q 최소로 바꾼다
        if (ai != null && ai != FALLBACK && job.trans.get(k) != null) {
          Map<String, Double> d = job.trans.get(k).get(ai);
          if (d != null && d.size() == 1 && runCount.merge(k, 1, Integer::sum) > 20) {
            // 이 상태에서 방금 수단을 금지(회차 사이 유지)하고 나머지 중 Q 최소 — 정해진 결과 수단만 빼면 대안이 없어 넘김(위 경계 전부)으로
            // 가 극단 목표에서 회차 11/30 이 통째로 비쌌다(C469i)
            java.util.Set<Integer> ban = banned.computeIfAbsent(k, x -> new java.util.HashSet<>());
            ban.add(ai);
            Integer alt = null;
            double bq = Double.MAX_VALUE;
            for (Map.Entry<Integer, Map<String, Double>> e : job.trans.get(k).entrySet()) {
              if (ban.contains(e.getKey()) || e.getValue().getOrDefault(k, 0d) >= 0.999999) {
                continue;
              }
              double self = e.getValue().getOrDefault(k, 0d);
              double rest = 0;
              for (Map.Entry<String, Double> sp : e.getValue().entrySet()) {
                if (!sp.getKey().equals(k) && !job.goalKey(sp.getKey())) {
                  rest += sp.getValue() * v.getOrDefault(sp.getKey(), job.effUpper);
                }
              }
              double q = (usable.get(e.getKey()).cost() + rest) / (1 - self);
              if (q < bq) {
                bq = q;
                alt = e.getKey();
              }
            }
            ai = alt == null ? FALLBACK : alt;
            pol.put(k, ai);
            runCount.put(k, 0);
            cycleBreaks++;
          }
        }
        Act a = ai == null || ai == FALLBACK ? null : usable.get(ai);
        String simArg =
            a != null && a.action() == Action.UNVEIL
                ? job.unveilPick(it)
                : a == null ? null : a.arg();
        PoeCraftEngine.Result r;
        if (a != null && IMPRINT.equals(a.id())) {
          r =
              imp == null && it.rarity() == Rarity.MAGIC
                  ? new PoeCraftEngine.Result(it, null, List.of(), List.of())
                  : null;
          if (r != null) {
            imp = k;
            impItem = it;
          }
        } else if (a != null && RESTORE.equals(a.id())) {
          r = imp == null ? null : new PoeCraftEngine.Result(impItem, null, List.of(), List.of());
          imp = null;
          impItem = null;
        } else {
          r = a == null ? null : job.engine.apply(it, a.action(), simArg, job.rng);
        }
        // 확률적 실패(C469h — 수확 '추가 + 무작위 제거'는 지운 쪽에 그 태그 자리가 없으면 실패) — 계획은 성공만 센 분포(실패는 공짜로 다시)라
        // 모의도 같은 수단을 몇 번 다시
        for (int retry = 0;
            retry < 10
                && r != null
                && r.error() != null
                && a != null
                && !IMPRINT.equals(a.id())
                && !RESTORE.equals(a.id());
            retry++) {
          r = job.engine.apply(it, a.action(), simArg, job.rng);
        }
        if ((r == null || r.error() != null) && a != null && job.trans.get(k) != null) {
          failedActs.merge(a.ko() + "(" + (r == null ? "" : r.error()) + ")", 1, Integer::sum);
          // 근사 차이(같은 열쇠의 다른 아이템)로 정책 수단을 못 쓰면 — 이 아이템에 통하는 수단 중 Q 가 가장 낮은 것(C469: 바로 넘기면
          // 위 경계를 통째로 더해 모의가 수십 배로 나왔다)
          List<double[]> qs = new ArrayList<>();
          for (Map.Entry<Integer, Map<String, Double>> e : job.trans.get(k).entrySet()) {
            Act x = usable.get(e.getKey());
            if (e.getKey().equals(ai) || IMPRINT.equals(x.id()) || RESTORE.equals(x.id())) {
              continue;
            }
            double self = e.getValue().getOrDefault(k, 0d);
            if (self >= 0.999999) {
              continue;
            }
            double rest = 0;
            for (Map.Entry<String, Double> sp : e.getValue().entrySet()) {
              if (!sp.getKey().equals(k) && !job.goalKey(sp.getKey())) {
                rest += sp.getValue() * v.getOrDefault(sp.getKey(), job.effUpper);
              }
            }
            double q = (x.cost() + rest) / (1 - self);
            if (q < job.effUpper) {
              qs.add(new double[] {q, e.getKey()});
            }
          }
          qs.sort(Comparator.comparingDouble(x -> x[0]));
          for (double[] qa : qs) {
            Act x = usable.get((int) qa[1]);
            String xa = x.action() == Action.UNVEIL ? job.unveilPick(it) : x.arg();
            PoeCraftEngine.Result xr = job.engine.apply(it, x.action(), xa, job.rng);
            if (xr.error() == null) {
              a = x;
              ai = (int) qa[1];
              r = xr;
              replaced++;
              break;
            }
          }
        }
        if (r == null || r.error() != null) {
          // '정제 → 아는 최저'(정책이거나, 근사 차이로 정책 수단을 못 쓸 때) — 그 기대 비용을 더하고 끝
          fallbackWhy.merge(
              a == null
                  ? "정책 넘김 " + describe(k, targets.size()).replaceAll(" · 잡옵션.*", "")
                  : "수단 실패 " + a.ko(),
              1,
              Integer::sum);
          c += Math.min(job.upper, job.effUpper);
          fallbacks++;
          path.add(FALLBACK);
          done = true;
          break;
        }
        // 제자리 걸음 감시 — 근사 차이로 같은 상태에서 같은 수단을 끝없이 쓰면 그 상태는 '정제 → 아는 최저'로
        if ((job.key(r.item()) + (imp == null ? "" : "#" + imp)).equals(k)) {
          if (++sameRun > STUCK_STEPS) {
            pol.put(k, FALLBACK);
            stuckStates++;
          }
        } else {
          sameRun = 0;
        }
        it = r.item();
        c += a.cost();
        a.spend().forEach((n, x) -> spent.merge(n, x, Double::sum));
        if (path.size() < 2_000) {
          path.add(ai);
        }
      }
      if (done) {
        succ++;
        paths.add(path);
        runCost.add(c);
        finals.add(reached ? it : null);
        if (reached) {
          reachedCost.add(c);
        }
      } else {
        failCost += c;
        if (diag.isEmpty()) {
          diag = "걸음 " + steps + " · 비용 " + Math.round(c) + " · " + String.join(" ; ", trace);
        }
      }
    }
    double totalCost = runCost.stream().mapToDouble(Double::doubleValue).sum();
    // 못 닿은 회차의 비용도 넣는다(성공 회차만 나누면 낮게 나온다)
    Double expected = succ == 0 ? null : (totalCost + failCost) / succ;
    Map<String, Double> per = new LinkedHashMap<>();
    final int succF = Math.max(1, succ);
    spent.forEach((n, x) -> per.put(n, x / succF));
    // 흔한 흐름 — 성공 회차 중 비용이 중간인 것, 같은 수단 연속은 ×n
    String pathText = "";
    if (!paths.isEmpty()) {
      List<Integer> order = new ArrayList<>();
      for (int i = 0; i < paths.size(); i++) {
        order.add(i);
      }
      order.sort(Comparator.comparingDouble(runCost::get));
      List<Integer> mid = paths.get(order.get(order.size() / 2));
      List<String> parts = new ArrayList<>();
      // 다시 시작(정제)으로 길어지지 않게 — 마지막 다시 시작 뒤만 보이고 그 앞은 횟수로(C461c)
      int last = -1;
      int restarts = 0;
      for (int i = 0; i < mid.size(); i++) {
        if (mid.get(i) != FALLBACK && usable.get(mid.get(i)).action() == Action.SCOUR) {
          last = i;
          restarts++;
        }
      }
      if (restarts > 0) {
        parts.add("정제로 다시 시작 ×" + restarts);
        mid = mid.subList(last + 1, mid.size());
      }
      for (int i = 0; i < mid.size(); ) {
        int j = i;
        while (j < mid.size() && mid.get(j).equals(mid.get(i))) {
          j++;
        }
        parts.add(label(usable, mid.get(i), fbLabel) + (j - i > 1 ? " ×" + (j - i) : ""));
        i = j;
      }
      pathText = String.join(" → ", parts);
    }
    // 자주 지나는 상태의 결정
    final int runsF = Math.max(1, runs);
    // 문구 · 수단이 같은 상태는 한 줄로(C469n — 바꾸기 종류만 다른 열쇠가 같은 문구로 여러 줄 보였다). 지나는 회차는 더하고(1 상한), 남은 비용은
    // 많이 지난 쪽
    Map<String, Rule> merged = new LinkedHashMap<>();
    visits.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(
            e -> {
              String st = describe(e.getKey(), targets.size());
              String act =
                  pol.get(e.getKey()) == null
                      ? "(모름)"
                      : label(usable, pol.get(e.getKey()), fbLabel);
              double share = (double) e.getValue() / runsF;
              merged.merge(
                  st + "|" + act,
                  new Rule(st, act, Math.min(1, share), v.get(e.getKey())),
                  (x, y) ->
                      new Rule(
                          x.state(),
                          x.action(),
                          Math.min(1, x.share() + y.share()),
                          x.remaining()));
            });
    List<Rule> rules =
        merged.values().stream()
            .sorted(Comparator.comparingDouble(Rule::share).reversed())
            .limit(12)
            .toList();
    PoeCraftPriceService.Prices p = prices.prices();
    String note =
        "준비 "
            + (tLearn - started)
            + "ms(화석 · 수확 "
            + (tLearn - tFossil)
            + "ms) · 탐색 "
            + (tSim - tLearn)
            + "ms(가치 반복 "
            + tVi
            + " · 넓히기 "
            + tExp
            + ")"
            + "ms · 모의 "
            + (System.currentTimeMillis() - tSim)
            + "ms · 상태 "
            + job.trans.size()
            + "개 · 수단 "
            + usable.size()
            + "개"
            + " · 탐색 "
            + rounds
            + "단계"
            + (refined > 0 ? " · 다듬기 " + refined + "수단(" + tRefine + "ms)" : "")
            + (converged
                ? "(정책이 닿는 상태를 모두 넓힘)"
                : capped ? " · 상태 한도(" + MAX_STATES + "개)로 더 넓히지 못함" : " · 시간 한도로 덜 넓힘")
            + (skipped.isEmpty() ? "" : " · 시세가 없어 뺀 수단: " + String.join(", ", skipped))
            + (fallbacks > 0 ? " · 정제 → " + fbLabel + "로 넘긴 회차 " + fallbacks + "(그 기대 비용으로 셈)" : "")
            + (stuckStates > 0 ? " · 제자리 걸음으로 넘긴 상태 " + stuckStates : "")
            + (cycles > 0 ? " · 상태 사이를 돌아 넘긴 회차 " + cycles : "")
            + (cycleBreaks > 0 ? " · 고리를 끊은 상태 " + cycleBreaks : "")
            + (policyRestarts > 0 ? " · 넘김 대신 정제 → 이 정책으로 처음부터 " + policyRestarts + "번" : "")
            + (unfinished > 0
                ? " · 다시 시작 " + RESTART_RUNS + "번에 못 끝낸 회차 " + unfinished + "(쓴 비용은 넣고 성공으로 안 셈)"
                : "")
            + (replaced > 0 ? " · 정책 수단이 이 아이템에 안 통해 바꾼 걸음 " + replaced : "")
            + " · 정책 모의 "
            + runs
            + "회(성공 "
            + succ
            + ")";
    Double se = null;
    if (succ > 1) {
      double mean = totalCost / succ;
      double var = 0;
      for (double x : runCost) {
        var += (x - mean) * (x - mean);
      }
      se = Math.sqrt(var / (succ - 1) / succ);
    }
    // 못 끝낸 회차가 있으면 비용 = 전체 / 성공 수(C536) — 성공이 적을수록 거칠다(성공 한 번당 비용이 기하 분포에 가까워 상대 오차 ≈ 1/√성공)
    if (unfinished > 0 && expected != null) {
      se = Math.max(se == null ? 0 : se, expected / Math.sqrt(succ));
    }
    if (jobId != null) {
      policies.put(
          jobId,
          new Policy(
              System.currentTimeMillis(), job, new HashMap<>(pol), new HashMap<>(v), usable));
    }
    Map<String, String> ko = new LinkedHashMap<>();
    Map<String, String> all = calc.spentKo(cls);
    per.keySet().forEach(n -> ko.put(n, all.getOrDefault(n, n)));
    // 결과 예시(C502) — 성공 회차를 비용 순으로 줄 세워 1/4 · 1/2 · 3/4 자리(겹치면 하나)
    List<Showcase> showcase = new ArrayList<>();
    List<Double> quant = new ArrayList<>();
    // 성공이 다섯 번은 돼야(C536 — 한 번이면 '운 좋으면 · 보통 · 운 나쁘면'이 같은 값으로 나와 분포처럼 보였다)
    if (reachedCost.size() >= 5) {
      List<Double> sorted = new ArrayList<>(reachedCost);
      java.util.Collections.sort(sorted);
      for (double q : new double[] {0.25, 0.5, 0.9}) {
        quant.add(sorted.get(Math.min(sorted.size() - 1, (int) Math.floor(q * sorted.size()))));
      }
    }
    // 목표에 닿은 회차만(C536 — 넘김 회차는 마지막 아이템이 목표 아이템이 아니다)
    List<Integer> ord = new ArrayList<>();
    for (int i = 0; i < finals.size(); i++) {
      if (finals.get(i) != null) {
        ord.add(i);
      }
    }
    if (!ord.isEmpty()) {
      ord.sort(Comparator.comparingDouble(runCost::get));
      java.util.LinkedHashSet<Integer> pick = new java.util.LinkedHashSet<>();
      for (double q : new double[] {0.25, 0.5, 0.75}) {
        pick.add(ord.get(Math.min(ord.size() - 1, (int) Math.floor(q * ord.size()))));
      }
      for (int i : pick) {
        PoeCraftService.View fv = craft.view(finals.get(i));
        showcase.add(new Showcase(runCost.get(i), fv.item().rarity().name(), fv.lines()));
      }
    }
    return new Result(
        expected,
        se,
        planCost,
        job.upper,
        job.upperLabel,
        job.trans.size(),
        runs,
        succ,
        per,
        ko,
        pathText,
        rules,
        usable.stream().map(Act::ko).toList(),
        p.league(),
        p.fetchedAt(),
        note,
        succ == 0
            ? "정책 모의에서 목표에 닿지 못했습니다"
                + (unfinished > 0
                    ? " — 처음부터 다시를 "
                        + policyRestarts
                        + "번 해도 목표 0번이라 계획 값 "
                        + Math.round(planCost)
                        + "c 는 확인되지 않았습니다(드문 결과에 기댄 값)"
                        + (job.upper < 1e8
                            ? ". 아는 가장 싼 방법: " + job.upperLabel + " " + Math.round(job.upper) + "c"
                            : "")
                    : "")
            : null,
        (fallbackWhy.isEmpty() ? "" : "넘김 사유 " + fallbackWhy + " · ")
            + (failedActs.isEmpty()
                ? diag
                : "안 통한 정책 수단 " + failedActs + (diag.isEmpty() ? "" : " · " + diag)),
        showcase,
        quant,
        eldTargets.isEmpty() ? null : calc.eldritchPlan(start, eldTargets));
  }

  /** 화석 · 수확 이름(표가 넘긴 id 만 있을 때) — 화석 한국어 이름 + 공명기, 수확은 설명. */
  private String methodKo(boolean fossil, String arg) {
    if (fossil) {
      List<String> names = new ArrayList<>();
      for (String slug : arg.split(",")) {
        craft.fossils().stream()
            .filter(f -> f.slug().equals(slug.trim()))
            .findFirst()
            .ifPresent(f -> names.add(f.nameKo() != null ? f.nameKo() : f.name()));
      }
      return String.join(" + ", names);
    }
    return craft.harvests().stream()
        .filter(h -> h.id().equals(arg))
        .findFirst()
        .map(h -> "수확: " + (h.textKo() != null ? h.textKo() : h.text()))
        .orElse("수확");
  }

  private static String label(List<Act> usable, int ai, String upperLabel) {
    return ai == FALLBACK ? "정제 → " + upperLabel : usable.get(ai).ko();
  }

  /** 연타 위 경계 — 카오스(레어 출발) · 변화+확장(매직 출발) · 에센스(일반 출발) 연타의 성공 한 번당 비용 중 최저. 표본에서 한 번도 안 나오면 1e9. */
  private double spamUpper(Job job, Item start) {
    double best = 1e9;
    Item rare = PoeCraftCalcService.normal(start);
    if (rare.rarity() == Rarity.NORMAL) {
      rare = job.engine.apply(rare, Action.ALCHEMY, null, job.rng).item();
    }
    Item magic =
        new Item(
            start.itemClass(),
            start.variant(),
            start.influence(),
            start.ilvl(),
            Rarity.MAGIC,
            start.affixes().stream().filter(Affix::fractured).toList(),
            false,
            start.implicits(),
            start.quality());
    for (Act a : job.acts) {
      Item from =
          switch (a.action()) {
            case CHAOS -> rare;
            case ALTERATION -> magic;
            case ESSENCE -> PoeCraftCalcService.normal(start);
            default -> null;
          };
      if (from == null) {
        continue;
      }
      int hits = 0;
      double cost = 0;
      for (int i = 0; i < SPAM_SAMPLE; i++) {
        PoeCraftEngine.Result r = job.engine.apply(from, a.action(), a.arg(), job.rng);
        if (r.error() != null) {
          break;
        }
        Item it = r.item();
        cost += a.cost();
        if (a.action() == Action.ALTERATION && it.affixes().size() < 2 && !job.met(it)) {
          Act aug =
              job.acts.stream()
                  .filter(x -> x.action() == Action.AUGMENTATION)
                  .findFirst()
                  .orElse(null);
          PoeCraftEngine.Result g =
              aug == null ? null : job.engine.apply(it, Action.AUGMENTATION, null, job.rng);
          if (g != null && g.error() == null) {
            it = g.item();
            cost += aug.cost();
          }
        }
        if (job.met(it)) {
          hits++;
        }
      }
      if (hits > 0 && cost / hits < best) {
        best = cost / hits;
        job.upperLabel = a.ko() + " 연타";
      }
    }
    return best;
  }

  /**
   * 다듬기(C467) — 정책이 처음 상태에서 닿는 넓힌 상태(넓이 우선, REFINE_STATES 개까지)마다 한 걸음 수단 중 Q 가 낮은 REFINE_ACTS 개를
   * SAMPLE_REFINE 번으로 다시 떠 전이를 바꾸고 가치 반복을 다시. 두 번(바뀐 정책이 새로 고른 수단도). 다시 굴리기(30,000번 공유)는 그대로. 다시 뜬
   * 수단 수.
   */
  private int refine(
      Job job, String s0, Map<String, Double> v, Map<String, Integer> pol, long until) {
    java.util.Set<String> done = java.util.concurrent.ConcurrentHashMap.newKeySet();
    for (int pass = 0; pass < 2 && System.currentTimeMillis() < until; pass++) {
      // 닿는 상태(넓이 우선)
      List<String> reach = new ArrayList<>();
      ArrayDeque<String> q = new ArrayDeque<>(List.of(s0));
      java.util.Set<String> seen = new java.util.HashSet<>(List.of(s0));
      while (!q.isEmpty() && reach.size() < REFINE_STATES) {
        String k = q.poll();
        Map<Integer, Map<String, Double>> tk = job.trans.get(k);
        Integer a = pol.get(k);
        if (job.goalKey(k) || tk == null || a == null || a == FALLBACK || tk.get(a) == null) {
          continue;
        }
        reach.add(k);
        for (String nk : tk.get(a).keySet()) {
          if (seen.add(nk)) {
            q.add(nk);
          }
        }
      }
      Map<String, List<Integer>> work = new LinkedHashMap<>();
      for (String k : reach) {
        Map<Integer, Map<String, Double>> tk = job.trans.get(k);
        List<double[]> qs = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Double>> e : tk.entrySet()) {
          if (rerolls(job.acts.get(e.getKey())) || done.contains(k + "#" + e.getKey())) {
            continue;
          }
          double self = e.getValue().getOrDefault(k, 0d);
          if (self >= 0.999999) {
            continue;
          }
          double rest = 0;
          for (Map.Entry<String, Double> sp : e.getValue().entrySet()) {
            if (!sp.getKey().equals(k) && !job.goalKey(sp.getKey())) {
              rest += sp.getValue() * v.getOrDefault(sp.getKey(), job.effUpper);
            }
          }
          qs.add(new double[] {(job.acts.get(e.getKey()).cost() + rest) / (1 - self), e.getKey()});
        }
        qs.sort(Comparator.comparingDouble(x -> x[0]));
        List<Integer> pick = new ArrayList<>();
        for (int i = 0; i < Math.min(REFINE_ACTS, qs.size()); i++) {
          pick.add((int) qs.get(i)[1]);
        }
        if (!pick.isEmpty()) {
          work.put(k, pick);
        }
      }
      if (work.isEmpty()) {
        break;
      }
      // 상태마다 한 스레드(안쪽 맵은 동시 맵이 아니다)
      work.entrySet().parallelStream()
          .forEach(
              w -> {
                if (System.currentTimeMillis() >= until) {
                  return;
                }
                Map<Integer, Map<String, Double>> tk = job.trans.get(w.getKey());
                for (int ai : w.getValue()) {
                  Map<String, Double> t = job.sample(w.getKey(), ai, SAMPLE_REFINE);
                  if (t == null) {
                    tk.remove(ai);
                  } else {
                    tk.put(ai, t);
                  }
                  done.add(w.getKey() + "#" + ai);
                }
              });
      decide(job, v, pol, until);
    }
    return done.size();
  }

  /** 상태 넓히기 — 수단마다 결과 분포 표본. */
  private static void expand(Job job, String k) {
    // 수단도 나란히(C469c — 처음 상태 하나를 한 스레드로 넓히면 화석 · 수확 다시 굴리기 30,000번씩이 이어져 수 초)
    Map<Integer, Map<String, Double>> tk = new java.util.concurrent.ConcurrentHashMap<>();
    java.util.stream.IntStream.range(0, job.acts.size())
        .parallel()
        .forEach(
            ai -> {
              Map<String, Double> t = job.sample(k, ai);
              if (t != null) {
                tk.put(ai, t);
              }
            });
    job.trans.put(k, new HashMap<>(tk));
  }

  /**
   * 결정 가치 반복 + 넘김 값 뉴턴(C471d) — 넘김(다시 시작)의 값 U 는 '다시 시작 비용 + 처음 상태 값'이 상한이라, 수렴한 가치 반복의 처음 상태 값 V0
   * 와 그 U 기울기로 U ← (다시 시작 + V0 − 기울기 · U)/(1 − 기울기). 위 경계에서 출발해 위에서만 내려온다(V0(U) 오목 — 단조 수렴).
   */
  private static void decide(Job job, Map<String, Double> v, Map<String, Integer> pol, long until) {
    for (int k = 0; k < 6; k++) {
      double[] r = valueIteration(job, v, pol, until, job.effUpper, true);
      if (r == null || r[1] >= 1 - 1e-12) {
        return;
      }
      double nu =
          Math.max(job.restartCost, (job.restartCost + r[0] - r[1] * job.effUpper) / (1 - r[1]));
      if (nu >= job.effUpper * (1 - 1e-4)) {
        return;
      }
      job.effUpper = nu;
    }
  }

  /**
   * 가치 반복(가우스-자이델) — 넓힌 상태마다 v = min_a (비용 + Σ p · v') / (1 − 제자리 확률). 넓히지 않은 상태는 v = 위 경계(정제 + 아는
   * 최저), 목표는 0. 위에서 내려온다. 변화가 충분히 작거나 시간이 다 되면 멈춘다.
   */
  private static double[] valueIteration(
      Job job,
      Map<String, Double> v,
      Map<String, Integer> pol,
      long until,
      double unexplored,
      boolean fallback) {
    List<String> keys = new ArrayList<>(job.trans.keySet());
    Map<String, Integer> idx = new HashMap<>();
    for (int i = 0; i < keys.size(); i++) {
      idx.put(keys.get(i), i);
    }
    int ns = keys.size();
    // 분포 — 같은 맵 객체(다시 굴리기 표본은 상태끼리 함께 쓴다)는 한 번만(C459g)
    java.util.IdentityHashMap<Map<String, Double>, Integer> distId =
        new java.util.IdentityHashMap<>();
    List<int[]> dIdx = new ArrayList<>();
    List<double[]> dP = new ArrayList<>();
    List<Double> dBase = new ArrayList<>();
    int[][] aId = new int[ns][];
    int[][] aDist = new int[ns][];
    double[][] aSelf = new double[ns][];
    double[][] aCost = new double[ns][];
    for (int i = 0; i < ns; i++) {
      String key = keys.get(i);
      Map<Integer, Map<String, Double>> tk = job.trans.get(key);
      int na = tk.size();
      aId[i] = new int[na];
      aDist[i] = new int[na];
      aSelf[i] = new double[na];
      aCost[i] = new double[na];
      int x = 0;
      for (Map.Entry<Integer, Map<String, Double>> ta : tk.entrySet()) {
        Map<String, Double> dist = ta.getValue();
        Integer d = distId.get(dist);
        if (d == null) {
          d = dIdx.size();
          distId.put(dist, d);
          double unexp = 0;
          List<Integer> si = new ArrayList<>();
          List<Double> sp = new ArrayList<>();
          for (Map.Entry<String, Double> e : dist.entrySet()) {
            if (job.goalKey(e.getKey())) {
              continue; // 목표 = 0
            }
            Integer ni = idx.get(e.getKey());
            if (ni == null) {
              unexp += e.getValue();
            } else {
              si.add(ni);
              sp.add(e.getValue());
            }
          }
          dIdx.add(si.stream().mapToInt(Integer::intValue).toArray());
          dP.add(sp.stream().mapToDouble(Double::doubleValue).toArray());
          dBase.add(unexp);
        }
        aId[i][x] = ta.getKey();
        aDist[i][x] = d;
        aSelf[i][x] = dist.getOrDefault(key, 0d);
        aCost[i][x] = job.acts.get(ta.getKey()).cost();
        x++;
      }
    }
    int nd = dIdx.size();
    // 못 넓힌 몫(질량) — 값은 반복마다 U 를 곱한다(C471: U 가 처음 상태 값에 달림)
    double[] mass = new double[nd];
    for (int d = 0; d < nd; d++) {
      mass[d] = dBase.get(d);
    }
    // 처음 상태(다시 시작 열쇠) 값과 그 넘김 값 U 미분(C471d) — 바깥에서 U = 다시 시작 비용 + V0(U) 를 뉴턴으로 푼다(수렴한 값으로만)
    int ri = fallback && job.restartKey != null ? idx.getOrDefault(job.restartKey, -1) : -1;
    double u = unexplored;
    double[] val = new double[ns];
    double[] dval = new double[ns];
    int[] best = new int[ns];
    for (int i = 0; i < ns; i++) {
      val[i] = v.getOrDefault(keys.get(i), unexplored);
      best[i] = Integer.MIN_VALUE;
    }
    double[] dv = new double[nd];
    double[] ddv = new double[nd];
    boolean converged = false;
    double realV = Double.MAX_VALUE;
    double realD = 1;
    for (int it = 0; it < VI_SWEEPS; it++) {
      for (int d = 0; d < nd; d++) {
        int[] si = dIdx.get(d);
        double[] sp = dP.get(d);
        double sum = mass[d] * u;
        double dsum = mass[d];
        for (int y = 0; y < si.length; y++) {
          sum += sp[y] * val[si[y]];
          dsum += sp[y] * dval[si[y]];
        }
        dv[d] = sum;
        ddv[d] = dsum;
      }
      double change = 0;
      for (int i = 0; i < ns; i++) {
        double b = Double.MAX_VALUE;
        double bd = 0;
        int ba = Integer.MIN_VALUE;
        for (int x = 0; x < aId[i].length; x++) {
          double self = aSelf[i][x];
          if (self >= 0.999999) {
            continue;
          }
          double qv = (aCost[i][x] + dv[aDist[i][x]] - self * val[i]) / (1 - self);
          if (qv < b) {
            b = qv;
            bd = (ddv[aDist[i][x]] - self * dval[i]) / (1 - self);
            ba = aId[i][x];
          }
        }
        if (ba == Integer.MIN_VALUE) {
          continue;
        }
        if (i == ri) {
          // 처음 상태는 넘김(자기로 다시 시작)을 뺀 실제 수단의 값 · 기울기로 뉴턴(넘김이면 기울기 1 이라 멈췄다)
          realV = b;
          realD = bd;
        }
        // 넘김 값 이상이면 넘김 — 값이 같은 수단끼리 고리를 돌지 않게 명시적으로. 처음 상태는 넘김(자기로 다시 시작)을 못 고른다(C472b — u = 다시
        // 시작 + V0 라 실제 수단 값과 거의 같아 반올림으로 넘김이 뽑혀 모의가 새 베이스만 200만 번)
        if (fallback && i != ri && b >= u) {
          b = u;
          bd = 1;
          ba = FALLBACK;
        }
        change = Math.max(change, Math.abs(b - val[i]) / Math.max(1e-6, b));
        val[i] = b;
        dval[i] = bd;
        best[i] = ba;
      }
      if (change < 1e-5) {
        converged = true;
        break;
      }
      if ((it & 15) == 15 && System.currentTimeMillis() > until) {
        break;
      }
    }
    for (int i = 0; i < ns; i++) {
      if (best[i] != Integer.MIN_VALUE) {
        v.put(keys.get(i), val[i]);
        pol.put(keys.get(i), best[i]);
      }
    }
    return ri >= 0 && converged && realV < Double.MAX_VALUE ? new double[] {realV, realD} : null;
  }

  /** 정책으로 처음 상태에서 닿는 넓히지 않은 상태(넓이 우선) — into 에 더해 max 개까지. */
  private static List<String> frontier(
      Job job, String s0, Map<String, Integer> pol, int max, List<String> into) {
    ArrayDeque<String> q = new ArrayDeque<>(List.of(s0));
    java.util.Set<String> seen = new java.util.HashSet<>(List.of(s0));
    while (!q.isEmpty() && into.size() < max) {
      String k = q.poll();
      if (job.goalKey(k)) {
        continue;
      }
      if (!job.trans.containsKey(k)) {
        if (!into.contains(k)) {
          into.add(k);
        }
        continue;
      }
      Integer a = pol.get(k);
      if (a == null || a == FALLBACK) {
        continue;
      }
      for (String nk : job.trans.get(k).get(a).keySet()) {
        if (seen.add(nk)) {
          q.add(nk);
        }
      }
    }
    return into;
  }

  /** 수단 하나 — 시세를 모르면 비용 NaN(고려에서 뺀다). */
  private Act act(String id, Action action, String arg, String ko, String cls) {
    Map<String, Double> m = new LinkedHashMap<>();
    calc.spend(m, action, arg, cls);
    Double c = calc.cost(m);
    // 비용 0 수단(제작대 옵션 지우기 등)이 고리를 만들지 않게 최소 비용(카오스 0.001)
    return new Act(id, action, arg, ko, m, c == null ? Double.NaN : Math.max(c, MIN_COST));
  }

  private static String text(List<String> ko, List<String> en) {
    return String.join(", ", ko != null && !ko.isEmpty() ? ko : en == null ? List.of() : en);
  }

  /** 상태 설명 — "레어 · 목표 1·3 맞음 · 잡옵션 접두 1/접미 2 · 막힘 2 · 제작대 접두 잠금". */
  static String describe(String key, int n) {
    int hash = key.indexOf('#');
    if (hash >= 0) {
      return describe(key.substring(0, hash), n) + " · 각인: " + describe(key.substring(hash + 1), n);
    }
    String[] f = key.split("\\|", -1);
    String rar =
        switch (f[0].substring(0, 1)) {
          case "N" -> "일반";
          case "M" -> "매직";
          default -> "레어";
        };
    int mask = Integer.parseInt(f[1]);
    List<String> met = new ArrayList<>();
    for (int t = 0; t < n; t++) {
      if ((mask & (1 << t)) != 0) {
        met.add(String.valueOf(t + 1));
      }
    }
    String[] j = f[2].split(",");
    int blocked = Integer.parseInt(f[3]);
    List<String> bl = new ArrayList<>();
    for (int t = 0; t < n; t++) {
      if ((blocked & (1 << t)) != 0) {
        bl.add(String.valueOf(t + 1));
      }
    }
    StringBuilder sb = new StringBuilder(rar);
    if (f[0].length() > 1) {
      sb.append(
          f[0].charAt(1) == 'e'
              ? " · 작열의 총주교 우세"
              : f[0].charAt(1) == 't' ? " · 엘드리치 동률(우세 없음)" : " · 세계 포식자 우세");
    }
    sb.append(" · ").append(met.isEmpty() ? "맞은 목표 없음" : "목표 " + String.join("·", met) + " 맞음");
    if (!"0".equals(j[0]) || !"0".equals(j[1])) {
      sb.append(" · 잡옵션 접두 ").append(j[0]).append("/접미 ").append(j[1]);
    }
    if (!bl.isEmpty()) {
      sb.append(" · 목표 ").append(String.join("·", bl)).append(" 자리를 잡옵션이 막음");
    }
    String cr = f[4];
    if (!cr.isEmpty()) {
      List<String> c = new ArrayList<>();
      for (char ch : cr.toCharArray()) {
        c.add(
            switch (ch) {
              case 'T' -> "제작대 목표 옵션";
              case 'P' -> "접두어 변경 불가";
              case 'S' -> "접미어 변경 불가";
              case 'M' -> "제작 속성 3개";
              case 'F' -> "분열된 목표 옵션";
              case 'f' -> "분열된 잡옵션(접두)";
              case 'g' -> "분열된 잡옵션(접미)";
              case 'v' -> "장막 옵션";
              default -> "다른 제작 옵션";
            });
      }
      sb.append(" · ").append(String.join(", ", c));
    }
    // 바꾸기 표시(C469m — 없으면 같은 문구 줄이 결정 표에 여럿 보였다)
    if (f.length > 5 && f[5].startsWith("c")) {
      String cv = f[5].substring(1);
      sb.append(
          cv.indexOf('2') >= 0 ? " · 수확 바꾸기로 목표 +1" : cv.indexOf('1') >= 0 ? " · 수확 바꾸기 가능" : "");
    }
    // 빈 자리 조건이 있을 때의 쪽별 옵션 수(C520 — 열쇠 끝 'o접두수,접미수')
    for (int x = 5; x < f.length; x++) {
      if (f[x].startsWith("o") && f[x].indexOf(',') > 1) {
        String[] ps = f[x].substring(1).split(",");
        sb.append(" · 옵션 접두 ").append(ps[0]).append("/접미 ").append(ps[1]);
      }
    }
    return sb.toString();
  }

  private Result error(String msg) {
    return new Result(
        null, null, null, null, null, 0, 0, 0, Map.of(), Map.of(), "", List.of(), List.of(), null,
        null, null, msg, "", List.of(), List.of(), null);
  }
}
