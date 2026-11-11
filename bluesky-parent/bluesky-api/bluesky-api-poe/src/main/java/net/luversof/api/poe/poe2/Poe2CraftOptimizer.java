package net.luversof.api.poe.poe2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.stereotype.Service;

import net.luversof.api.poe.poe2.Poe2CraftCalcService.Target;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Action;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Affix;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;

/**
 * PoE2 최적 제작 경로(10-19 C461 — 사용자 요청 "PoE2 계산기도 조합 고려" · "제작법들을 모두 조합해서, 조합 간 병행 안 되는 제작 규칙도 모두
 * 체크"). PoE1 {@code PoeCraftOptimizer}(C457~C459)와 같은 방법: 제작 과정을 상태(희귀도 · 맞은 목표 · 쪽마다 잡옵션 · 막힌 자리 ·
 * 미공개 훼손 옵션)로 보고 상태마다 쓸 수 있는 수단(화폐 × 등급 × 징조 · 에센스 · 훼손 + 드러내기 · 분열 · 새 베이스)의 결과를 같은 제작 엔진으로 표본을 떠,
 * LAO*(결정 = 위 경계 값, 탐색 = 낙관 값) · 분포 공유 가치 반복으로 상태별 최소 기대 비용과 정책을 구한 뒤 실제 아이템에 굴려 비용을 다시 잰다. 징조 · 등급
 * 조합이 막히는 규칙(같은 화폐 좌 · 우 징조, 최소 속성 레벨, 희귀도, 자리, 그룹)은 엔진이 거절해 계산에 쓰이지 않는다. 비용은 디바인.
 */
@Service
public class Poe2CraftOptimizer {

  public record Request(
      Item item,
      List<Target> targets,
      Integer budgetMs,
      // 아는 가장 싼 방법의 성공 한 번당 비용(시뮬레이터 표 최저, 디바인) — 없으면 연금 연타 표본으로 잰다
      Double upper,
      String upperLabel,
      // 재조합 성공 확률(C472, 인게임 값 1~100) — 있으면 표에 반쪽 재조합 · 최적 조합에 재조합 수단
      Integer recombChance,
      // 사용자 가격(C530) — 작업 스레드에서 건다
      Map<String, Double> prices) {

    public Request(
        Item item,
        List<Target> targets,
        Integer budgetMs,
        Double upper,
        String upperLabel,
        Integer recombChance) {
      this(item, targets, budgetMs, upper, upperLabel, recombChance, null);
    }
  }

  public record Rule(
      String state,
      String action,
      double share,
      Double remaining,
      // 영어 화면용(10-22 C561 ⑦b — PoE1 C557 짝)
      String stateEn,
      String actionEn) {}

  /** 결과 — PoE1 과 같은 모양(게이트가 같은 화면 조각을 쓴다), 비용 단위만 디바인. */
  public record Result(
      Double expectedCost,
      Double stderr,
      Double planCost,
      Double upper,
      String upperLabel,
      int states,
      int runs,
      int successes,
      Map<String, Double> perSuccess,
      Map<String, String> namesKo,
      String path,
      List<Rule> rules,
      List<String> methods,
      String league,
      String pricesAt,
      String note,
      String error,
      String diag,
      // 결과 예시(C503, PoE1 C502 짝) — 성공 회차 중 싼 편 · 중간 · 비싼 편의 마지막 아이템
      List<Showcase> showcase,
      // 성공 회차 비용 분포(C505) — 25% · 50% · 90% 지점(못 닿은 회차 비용은 안 넣음, 기대 비용과 다를 수 있다)
      List<Double> costQuantiles,
      // 영어 화면용(C561 — 게이트는 PoE1 과 같은 이름 칸으로 읽는다)
      String upperLabelEn,
      String pathEn,
      List<String> methodsEn,
      String noteEn,
      String errorEn,
      // 모의 성공이 적어 계획 값을 비용으로 보였을 때의 모의 값(C563), 아니면 null
      Double simCost) {}

  /**
   * 결과 예시 하나 — 그 회차 비용(디바인) · 옵션 줄. 게이트는 PoE1 과 같은 결과 모양(PoeCraft.OptimizeStatus)으로 받으므로 줄도 PoE1 줄
   * 모양으로 (원시형 칸이 비면 Jackson 3 기본값 FAIL_ON_NULL_FOR_PRIMITIVES 로 결과 전체를 못 읽었다)
   */
  public record Showcase(double cost, String rarity, List<ShowLine> lines) {}

  public record ShowLine(
      String id,
      String gen,
      String name,
      String nameKo,
      List<String> en,
      List<String> ko,
      List<String> enMin,
      List<String> koMin,
      Integer tierRank,
      int ilvl,
      boolean crafted,
      boolean fractured,
      boolean essence) {}

  public record JobStatus(String id, String state, long elapsedMs, long budgetMs, Result result) {}

  /** 수단 — 엔진 동작 · 인자(등급 · 슬러그) · 징조, 한 번 쓸 때 화폐와 비용(디바인). NEW_BASE 는 엔진 동작 없이 새 일반 베이스. */
  private record Act(
      String id,
      Action action,
      String arg,
      Set<String> omens,
      String ko,
      Map<String, Double> spend,
      double cost,
      boolean newBase,
      // 영어 이름(C561 — 데이터의 화폐 · 징조 · 에센스 · 뼈 영어 원문)
      String en) {}

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
      Double remaining,
      // 영어 화면용(C561)
      String stateEn,
      String actionEn) {}

  private final Map<String, Policy> policies = new java.util.concurrent.ConcurrentHashMap<>();

  /** 다음 수단 — 모르는 작업이면 null. */
  public Next next(String id, Item item) {
    Policy p = id == null ? null : policies.get(id);
    if (p == null || item == null) {
      return null;
    }
    String k = p.job().key(craft.normalize(item));
    int n = p.job().goal.size();
    if (p.job().goalKey(k)) {
      return new Next(true, true, describe(k, n), null, null, 0d, describeEn(k, n), null);
    }
    Integer ai = p.pol().get(k);
    if (ai == null) {
      return new Next(false, false, describe(k, n), null, null, null, describeEn(k, n), null);
    }
    return new Next(
        false,
        true,
        describe(k, n),
        label(p.usable(), ai, p.job().upperLabel),
        ai == FALLBACK ? "FALLBACK" : p.usable().get(ai).id(),
        p.v().get(k),
        describeEn(k, n),
        labelEn(p.usable(), ai, p.job().upperLabelEn));
  }

  static final int MAX_STATES = 5_000;
  static final int SAMPLE_REROLL = 20_000;
  static final int SAMPLE_STEP = 300;
  // 다시 굴리기 표본을 늘리는 기준 — 목표를 새로 맞힌 결과 수(C472d)
  static final int RARE_HITS = 40;
  // 모의 성공이 이보다 적으면 계획 값을 비용으로(C563, PoE1 과 같게)
  static final int FEW_SUCCESSES = 5;
  // 다듬기(C467, PoE1 과 같음)
  static final int SAMPLE_REFINE = 4_000;
  static final int REFINE_STATES = 300;
  static final int REFINE_ACTS = 3;
  static final int SIM_RUNS = 400;
  static final int SIM_STEPS = 50_000;
  static final int STUCK_STEPS = 200;
  // 고리 감시(C469f) — 한 회차 비용이 계획의 이 배수를 넘으면 넘김
  static final double CYCLE_FACTOR = 12;
  // 모의 하드 한도 — 예산 뒤 이만큼까지만(C472f)
  static final long SIM_GRACE_MS = 10_000;
  // 시간이 다 된 뒤 최소 회차에서 내다볼 상태 수 상한(C469l)
  static final int LATE_LOOKS = 300;
  static final long DEFAULT_BUDGET_MS = 8_000;
  static final long JOB_BUDGET_MS = 30_000;
  static final int EXPAND_PER_ROUND = 120;
  static final int VI_SWEEPS = 300;
  static final double MIN_COST = 0.00001;
  static final int SPAM_SAMPLE = 20_000;
  static final int FALLBACK = -1;

  private final Poe2CraftService craft;
  private final Poe2CraftCalcService calc;
  private final java.util.concurrent.ExecutorService pool =
      java.util.concurrent.Executors.newFixedThreadPool(
          2,
          r -> {
            Thread t = new Thread(r, "poe2-craft-optimizer");
            t.setDaemon(true);
            return t;
          });
  private final Map<String, Running> jobs = new java.util.concurrent.ConcurrentHashMap<>();

  public Poe2CraftOptimizer(Poe2CraftService craft, Poe2CraftCalcService calc) {
    this.craft = craft;
    this.calc = calc;
  }

  public String start(Request req) {
    long now = System.currentTimeMillis();
    jobs.entrySet().removeIf(e -> now - e.getValue().started() > 10 * 60_000L);
    long budget = req.budgetMs() == null ? JOB_BUDGET_MS : Math.min(40_000, req.budgetMs());
    Request r =
        new Request(
            req.item(),
            req.targets(),
            (int) budget,
            req.upper(),
            req.upperLabel(),
            req.recombChance(),
            req.prices());
    String id = java.util.UUID.randomUUID().toString();
    jobs.put(
        id,
        new Running(
            now,
            budget,
            pool.submit(
                () -> {
                  // 사용자 가격(C530) — 작업 스레드에서
                  Poe2CraftCalcService.override(r.prices());
                  try {
                    return optimize(r, id);
                  } finally {
                    Poe2CraftCalcService.override(null);
                  }
                })));
    policies.entrySet().removeIf(e -> now - e.getValue().at() > 10 * 60_000L);
    return id;
  }

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
      res =
          error(
              "계산 중 오류: " + e.getClass().getSimpleName(),
              "Calculation error: " + e.getClass().getSimpleName());
    }
    return new JobStatus(id, "done", el, run.budget(), res);
  }

  private final class Job {
    final List<Target> goal;
    // 빈 자리 조건(C521, PoE1 C520 짝): 쪽마다 남아야 할 빈 자리 [접두, 접미] · 레어 쪽당 한도. 있으면 열쇠 끝에 쪽별 옵션 수
    int[] openNeed = new int[2];
    int openCap = 3;
    final Map<String, Integer> idMask = new HashMap<>();
    final Map<Integer, Integer> groupMask = new HashMap<>();
    final List<Act> acts;
    final Item fresh;
    final Map<String, Item> rep = new java.util.concurrent.ConcurrentHashMap<>();
    final Map<String, Map<Integer, Map<String, Double>>> trans =
        new java.util.concurrent.ConcurrentHashMap<>();
    final Map<String, Map<String, Double>> rerollCache =
        new java.util.concurrent.ConcurrentHashMap<>();
    final long deadline;
    final SplittableRandom rng = new SplittableRandom(461);
    final java.util.concurrent.atomic.AtomicLong seeds =
        new java.util.concurrent.atomic.AtomicLong(4610);
    final ThreadLocal<SplittableRandom> localRng =
        ThreadLocal.withInitial(() -> new SplittableRandom(seeds.incrementAndGet()));
    double upper = 1e9;
    String upperLabel = "";
    String upperLabelEn = "";
    // 재조합 수단(C472) — id → {필요한 목표 비트, 성공 확률}
    final Map<String, double[]> recomb = new HashMap<>();
    // 다시 시작(C471) — 새 아이템 상태 열쇠 · 비용 · 가치 반복이 낸 넘김 값
    String restartKey;
    double restartCost;
    volatile double effUpper = Double.MAX_VALUE;
    final Poe2CraftEngine engine = craft.engine();

    Job(List<Target> goal, List<Act> acts, Item fresh, long deadline) {
      this.goal = goal;
      this.acts = acts;
      this.fresh = fresh;
      this.deadline = deadline;
      for (int t = 0; t < goal.size(); t++) {
        int bit = 1 << t;
        for (String id : goal.get(t).anyOf()) {
          idMask.merge(id, bit, (x, y) -> x | y);
          Poe2CraftEngine.Mod m = craft.modOf(id);
          if (m != null && m.groups() != null) {
            for (Integer g : m.groups()) {
              groupMask.merge(g, bit, (x, y) -> x | y);
            }
          }
        }
      }
    }

    /** 상태 열쇠 — 희귀도 | 맞은 목표 | 잡옵션 접두,접미 | 그룹이 막힌 목표 | 미공개 훼손(Dp · Ds) · 타락 · 축성. */
    String key(Item it) {
      int n = goal.size();
      List<Affix> af = it.affixes();
      int m = af.size();
      int[] am = new int[m];
      for (int i = 0; i < m; i++) {
        am[i] = Poe2CraftEngine.unrevealed(af.get(i)) ? 0 : idMask.getOrDefault(af.get(i).id(), 0);
      }
      int full = 1 << n;
      boolean[][] dp = new boolean[m + 1][full];
      dp[0][0] = true;
      for (int i = 0; i < m; i++) {
        for (int s = 0; s < full; s++) {
          if (!dp[i][s]) {
            continue;
          }
          dp[i + 1][s] = true;
          int free = am[i] & ~s;
          while (free != 0) {
            int bit = free & -free;
            dp[i + 1][s | bit] = true;
            free &= free - 1;
          }
        }
      }
      int mask = 0;
      for (int s = 0; s < full; s++) {
        if (dp[m][s] && Integer.bitCount(s) > Integer.bitCount(mask)) {
          mask = s;
        }
      }
      boolean[] used = new boolean[m];
      int cur = mask;
      for (int i = m - 1; i >= 0; i--) {
        if (dp[i][cur]) {
          continue;
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
      StringBuilder ex = new StringBuilder();
      for (int i = 0; i < m; i++) {
        Affix a = af.get(i);
        if (Poe2CraftEngine.unrevealed(a)) {
          // 미공개 훼손 — 쪽 · 다시 봄(r, 심연의 메아리는 한 번) · 드러낼 셋에 목표가 있나(h)(C463)
          ex.append("prefix".equals(a.gen()) ? "Dp" : "Ds");
          if (a.id().contains("~")) {
            ex.append('r');
          }
          if (engine.revealOffers(it).stream().anyMatch(idMask::containsKey)) {
            ex.append('h');
          }
          continue;
        }
        if (used[i]) {
          continue;
        }
        if ("prefix".equals(a.gen())) {
          jp++;
        } else if ("suffix".equals(a.gen())) {
          js++;
        }
        if (a.groups() != null) {
          for (Integer g : a.groups()) {
            blocked |= groupMask.getOrDefault(g, 0);
          }
        }
      }
      blocked &= ~mask;
      if (it.corrupted()) {
        ex.append('C');
      }
      if (it.sanctified()) {
        ex.append('S');
      }
      return it.rarity().name().charAt(0)
          + "|"
          + mask
          + "|"
          + jp
          + ","
          + js
          + "|"
          + blocked
          + "|"
          + ex
          + openBits(it);
    }

    /** 빈 자리 조건이 있으면 '|o접두수,접미수'(C521). */
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
      if (!Poe2CraftCalcService.meets(it, goal)) {
        return false;
      }
      if (openNeed[0] + openNeed[1] == 0) {
        return true;
      }
      int[] free = Poe2CraftCalcService.openFree(it);
      return free[0] >= openNeed[0] && free[1] >= openNeed[1];
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
      // 빈 자리 조건(C521) — 열쇠 끝 '|o접두수,접미수', 매직 쪽당 1 · 그 밖 레어 한도
      int o = k.lastIndexOf("|o");
      if (o < 0) {
        return false;
      }
      String[] ps = k.substring(o + 2).split(",");
      int cap = k.charAt(0) == 'M' ? 1 : openCap;
      return cap - Integer.parseInt(ps[0]) >= openNeed[0]
          && cap - Integer.parseInt(ps[1]) >= openNeed[1];
    }

    /** 드러내기 고르기 — 제시 셋 중 목표에 드는 것, 없으면 무작위. 제시가 없으면 null. */
    String revealPick(Item it) {
      List<String> offers = engine.revealOffers(it);
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

    /** 수단 하나 적용 — 새 베이스 · 드러내기 고르기를 포함. 못 쓰면 null. */
    Item apply(Act a, Item it, java.util.random.RandomGenerator r) {
      if (recomb.containsKey(a.id())) {
        return null; // 재조합은 모의에서 따로(C472)
      }
      if (a.newBase()) {
        return fresh;
      }
      String arg = a.arg();
      if (a.action() == Action.REVEAL) {
        arg = revealPick(it);
        if (arg == null) {
          return null;
        }
      }
      Poe2CraftEngine.Result res = engine.apply(it, a.action(), arg, a.omens(), r);
      return res.error() != null ? null : res.item();
    }

    /** 상태마다 같은 분포를 함께 쓰는 수단인가(새 베이스 · 일반 아이템의 진화 · 연금) — 다듬기에서 뺀다. */
    boolean shared(String k, Act a) {
      Item it = rep.get(k);
      return a.newBase()
          || (it != null
              && it.rarity() == Rarity.NORMAL
              && (a.action() == Action.TRANSMUTE || a.action() == Action.ALCHEMY));
    }

    Map<String, Double> sample(String k, int ai) {
      return sample(k, ai, SAMPLE_STEP);
    }

    /** step = 한 걸음 수단의 표본 수(다듬기는 더 많이). */
    Map<String, Double> sample(String k, int ai, int step) {
      Item it = rep.get(k);
      Act a = acts.get(ai);
      double[] rc = recomb.get(a.id());
      if (rc != null) {
        // 재조합(C472) — 필요한 쪽 목표를 다 채운 매직 · 레어만, 성공 = 완성 · 실패 = 새 베이스
        int bar = k.indexOf('|');
        int mask = Integer.parseInt(k.substring(bar + 1, k.indexOf('|', bar + 1)));
        int need = (int) rc[0];
        if ((mask & need) != need || it.rarity() == Rarity.NORMAL || goalKey(k)) {
          return null;
        }
        String goalK = "R|" + ((1 << goal.size()) - 1) + "|0,0|0|";
        return rc[1] >= 1 ? Map.of(goalK, 1d) : Map.of(goalK, rc[1], restartKey, 1 - rc[1]);
      }
      // 기억 없는 다시 굴리기 — 일반 아이템에서 진화 · 연금, 새 베이스(결과가 아이템 상태에 안 달림)
      boolean reroll =
          a.newBase()
              || (it.rarity() == Rarity.NORMAL
                  && (a.action() == Action.TRANSMUTE || a.action() == Action.ALCHEMY));
      String ck = null;
      if (reroll) {
        StringBuilder sb = new StringBuilder(a.id()).append('|');
        if (!a.newBase()) {
          it.affixes().stream()
              .filter(Affix::fractured)
              .map(Affix::id)
              .sorted()
              .forEach(x -> sb.append(x).append(','));
        }
        ck = sb.toString();
        Map<String, Double> hit = rerollCache.get(ck);
        if (hit != null) {
          // '제자리뿐'은 상태마다 판정(C461b — 처음 뜬 상태 기준으로 저장하면 다른 상태에서도 빠졌다)
          return hit.isEmpty() || (hit.size() == 1 && hit.containsKey(k)) ? null : hit;
        }
      }
      int n = a.newBase() ? 1 : reroll ? SAMPLE_REROLL : step;
      int curMask = maskOf(k);
      int newHits = 0;
      Map<String, Double> out = new HashMap<>();
      int ok = 0;
      for (int i = 0; i < n; i++) {
        if ((i & 255) == 255 && System.currentTimeMillis() > deadline) {
          break;
        }
        Item r = apply(a, it, localRng.get());
        if (r == null) {
          if (i >= 20 && ok == 0) {
            break;
          }
          continue;
        }
        ok++;
        String nk = key(r);
        rep.putIfAbsent(nk, r);
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
      // 드문 결과 깎기(C563 ⑧, PoE1 PoeCraftOptimizer.shrinkProgress 설명)
      net.luversof.api.poe.service.PoeCraftOptimizer.shrinkProgress(
          out, this::maskOf, curMask, okD, net.luversof.api.poe.service.PoeCraftOptimizer.LCB_Z);
      if (ck != null) {
        rerollCache.put(ck, out);
      }
      // 제자리뿐이면 이 상태에선 버린다 — 분포 저장과 따로(C461b)
      if (out.size() == 1 && out.containsKey(k)) {
        return null;
      }
      return out;
    }
  }

  public Result optimize(Request req) {
    return optimize(req, null);
  }

  /** jobId 가 있으면 끝난 정책을 보관한다(C464 — 에뮬레이터 따라 하기). */
  private Result optimize(Request req, String jobId) {
    long started = System.currentTimeMillis();
    // 빈 자리 조건(C521)은 옵션 목표와 나눠 Job 에 — 옵션 목표가 하나는 있어야
    int[] openNeed =
        Poe2CraftCalcService.openNeed(req.targets() == null ? List.of() : req.targets());
    List<Target> realTargets =
        Poe2CraftCalcService.real(req.targets() == null ? List.of() : req.targets());
    if (realTargets.isEmpty()) {
      return error("목표 옵션을 하나 이상 고르세요", "Choose at least one target modifier");
    }
    if (realTargets.size() > 6) {
      return error("목표는 여섯 개까지입니다", "Up to six targets");
    }
    Item start = craft.normalize(req.item());
    // 쪽마다 자리 검사(C471e) — 한쪽(접두/접미)에만 붙는 목표가 그쪽 자리 수보다 많으면 동시에 채울 수 없다(접미 저항 넷 — 정책이 넘김뿐인 1e9 결과를
    // '모의 400/400 성공'으로 보였다)
    int onlyPre = 0;
    int onlySuf = 0;
    for (Target t : realTargets) {
      java.util.Set<String> gens = new java.util.HashSet<>();
      for (String id : t.anyOf()) {
        Poe2CraftEngine.Mod md = craft.modOf(id);
        String g = md == null ? null : md.gen();
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
    int perSide = Poe2CraftEngine.maxPerGen(start, Rarity.RARE);
    // 빈 자리 조건도 그 쪽 자리를 쓴다(C521)
    if (onlyPre + openNeed[0] > perSide || onlySuf + openNeed[1] > perSide) {
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
              + "자리",
          "The targets cannot all fit — prefix targets "
              + onlyPre
              + (openNeed[0] > 0 ? " (+" + openNeed[0] + " open)" : "")
              + " · suffix targets "
              + onlySuf
              + (openNeed[1] > 0 ? " (+" + openNeed[1] + " open)" : "")
              + ", a rare has "
              + perSide
              + " slots per side");
    }
    if (start.corrupted() || start.sanctified()) {
      return error("타락 · 축성된 아이템은 바꿀 수 없습니다", "Corrupted or sanctified items cannot be modified");
    }
    long budget =
        Math.max(
            2_000, Math.min(40_000, req.budgetMs() == null ? DEFAULT_BUDGET_MS : req.budgetMs()));
    String pk = start.poolKey();
    Poe2CraftCalcService.Names names = calc.names(pk);
    Poe2CraftService.Options o = craft.options();
    List<Target> targets = realTargets;
    Set<String> targetIds = new java.util.HashSet<>();
    targets.forEach(t -> targetIds.addAll(t.anyOf()));
    Map<String, String> ko = new HashMap<>();
    Map<String, String> omenKo = new HashMap<>();
    Map<String, String> omenEn = new HashMap<>();
    (o.omens() == null ? List.<Poe2CraftService.Omen>of() : o.omens())
        .forEach(
            x -> {
              omenKo.put(x.key(), x.nameKo());
              omenEn.put(x.key(), x.name());
              ko.put(x.name(), x.nameKo());
            });
    List<Act> acts = new ArrayList<>();
    // 화폐 × 등급 × 징조(같은 화폐의 좌 · 우 징조를 함께 거는 등 막히는 조합은 엔진이 거절)
    Map<Action, List<List<String>>> omenSets = new java.util.EnumMap<>(Action.class);
    omenSets.put(Action.TRANSMUTE, List.of(List.of()));
    omenSets.put(Action.AUGMENTATION, List.of(List.of()));
    omenSets.put(
        Action.REGAL,
        List.of(
            List.of(),
            List.of("SinistralCoronation"),
            List.of("DextralCoronation"),
            List.of("HomogenisingCoronation")));
    omenSets.put(
        Action.ALCHEMY, List.of(List.of(), List.of("SinistralAlchemy"), List.of("DextralAlchemy")));
    omenSets.put(
        Action.EXALT,
        List.of(
            List.of(),
            List.of("SinistralExaltation"),
            List.of("DextralExaltation"),
            List.of("GreaterExaltation"),
            List.of("HomogenisingExaltation")));
    omenSets.put(
        Action.CHAOS,
        List.of(
            List.of(),
            List.of("SinistralErasure"),
            List.of("DextralErasure"),
            List.of("Whittling")));
    omenSets.put(
        Action.ANNUL,
        List.of(
            List.of(),
            List.of("SinistralAnnulment"),
            List.of("DextralAnnulment"),
            List.of("GreaterAnnulment")));
    omenSets.put(Action.FRACTURE, List.of(List.of()));
    for (Poe2CraftService.Currency c :
        o.currencies() == null ? List.<Poe2CraftService.Currency>of() : o.currencies()) {
      Action act;
      try {
        act = Action.valueOf(c.action());
      } catch (RuntimeException e) {
        continue;
      }
      List<List<String>> sets = omenSets.get(act);
      if (sets == null) {
        continue;
      }
      ko.put(c.name(), c.nameKo());
      String tier = c.tier() == null ? "" : c.tier();
      for (List<String> om : sets) {
        String omText =
            om.isEmpty()
                ? ""
                : " + "
                    + String.join(" + ", om.stream().map(x -> omenKo.getOrDefault(x, x)).toList());
        String omTextEn =
            om.isEmpty()
                ? ""
                : " + "
                    + String.join(" + ", om.stream().map(x -> omenEn.getOrDefault(x, x)).toList());
        acts.add(
            act(
                act.name() + ":" + tier + ":" + String.join(",", om),
                act,
                tier,
                om,
                c.nameKo() + omText,
                c.name() + omTextEn,
                names,
                false));
      }
    }
    // 에센스 — 보장 옵션이 목표에 드는 것(보통: 매직 → 레어, 완벽 · 타락: 레어에서 하나 지우고)
    for (Poe2CraftService.EssenceOption e : craft.essences(pk)) {
      if (e.modId() == null || !targetIds.contains(e.modId())) {
        continue;
      }
      ko.put(e.name(), e.nameKo());
      acts.add(
          act(
              "ESSENCE:" + e.slug(),
              Action.ESSENCE,
              e.slug(),
              List.of(),
              e.nameKo(),
              e.name(),
              names,
              false));
      if ("perfect".equals(e.kind())) {
        for (String side : List.of("SinistralCrystallisation", "DextralCrystallisation")) {
          acts.add(
              act(
                  "ESSENCE:" + e.slug() + ":" + side,
                  Action.ESSENCE,
                  e.slug(),
                  List.of(side),
                  e.nameKo() + " + " + omenKo.getOrDefault(side, side),
                  e.name() + " + " + omenEn.getOrDefault(side, side),
                  names,
                  false));
        }
      }
    }
    // 훼손 + 드러내기 — 목표에 훼손된 옵션이 있을 때(보존된 뼈, 좌/우 강령 징조)
    Set<String> deseIds = new java.util.HashSet<>();
    Set<String> liches = new java.util.LinkedHashSet<>();
    craft
        .desecratedOf(pk)
        .forEach(
            d -> {
              deseIds.add(d.mod().id());
              if (targetIds.contains(d.mod().id()) && d.lich() != null) {
                String om =
                    switch (d.lich()) {
                      case "ulaman" -> "Sovereign";
                      case "amanamu" -> "Liege";
                      case "kurgal" -> "Blackblooded";
                      default -> null;
                    };
                if (om != null) {
                  liches.add(om);
                }
              }
            });
    if (targetIds.stream().anyMatch(deseIds::contains)) {
      String cls = pk.split("[|]")[0];
      for (Poe2CraftService.BoneInfo b :
          o.bones() == null ? List.<Poe2CraftService.BoneInfo>of() : o.bones()) {
        if (b.classes() == null || !b.classes().contains(cls) || b.breach()) {
          continue;
        }
        ko.put(b.name(), b.nameKo());
        acts.add(
            act(
                "DESECRATE:" + b.slug(),
                Action.DESECRATE,
                b.slug(),
                List.of(),
                "훼손(" + b.nameKo() + ")",
                "Desecrate (" + b.name() + ")",
                names,
                false));
        // 군주 징조(C463) — 목표 훼손 옵션의 군주와 같은 것(무기 · 장신구 뼈만 받는 규칙은 엔진이 거절)
        for (String lich : liches) {
          acts.add(
              act(
                  "DESECRATE:" + b.slug() + ":" + lich,
                  Action.DESECRATE,
                  b.slug(),
                  List.of(lich),
                  "훼손(" + b.nameKo() + ") + " + omenKo.getOrDefault(lich, lich),
                  "Desecrate (" + b.name() + ") + " + omenEn.getOrDefault(lich, lich),
                  names,
                  false));
        }
        for (String side : List.of("SinistralNecromancy", "DextralNecromancy")) {
          acts.add(
              act(
                  "DESECRATE:" + b.slug() + ":" + side,
                  Action.DESECRATE,
                  b.slug(),
                  List.of(side),
                  "훼손(" + b.nameKo() + ") + " + omenKo.getOrDefault(side, side),
                  "Desecrate (" + b.name() + ") + " + omenEn.getOrDefault(side, side),
                  names,
                  false));
        }
      }
      acts.add(
          new Act(
              "REVEAL",
              Action.REVEAL,
              null,
              Set.of(),
              "드러내기(목표 먼저)",
              Map.of(),
              MIN_COST,
              false,
              "Reveal (targets first)"));
      // 심연의 메아리(C463) — 드러낼 셋을 한 번 다시
      acts.add(
          act(
              "REVEAL_REROLL:AbyssalEchoes",
              Action.REVEAL_REROLL,
              null,
              List.of("AbyssalEchoes"),
              omenKo.getOrDefault("AbyssalEchoes", "심연의 메아리") + "(셋 다시)",
              omenEn.getOrDefault("AbyssalEchoes", "Omen of Abyssal Echoes")
                  + " (reroll the three)",
              names,
              false));
    }
    // 새 베이스 — 베이스 값은 '가격 직접 입력'의 베이스 아이템(10-23 Q20b, 없으면 0 — 시뮬레이터 표와 같은 가정)
    Item fresh = new Item(pk, start.ilvl(), Rarity.NORMAL, List.of(), false);
    double bp = calc.basePrice();
    acts.add(
        new Act(
            "NEW_BASE",
            null,
            null,
            Set.of(),
            bp > 0 ? "새 베이스" : "새 베이스(베이스 값 제외)",
            bp > 0 ? Map.of(Poe2CraftCalcService.BASE_ITEM, 1d) : Map.of(),
            Math.max(MIN_COST, bp),
            true,
            bp > 0 ? "New base" : "New base (base cost excluded)"));
    // 재조합기(C472) — 한쪽(접두/접미) 목표를 다 채운 아이템에 다른 쪽 반쪽(계산기의 가장 싼 방법으로 따로 만듦)을 합친다. 성공 = 완성, 실패 = 둘 다
    // 잃음(새 베이스). 확률은 공식 비공개라 사용자가 넣은 인게임 값
    Map<String, double[]> recombInfo = new HashMap<>();
    Integer rcc = req.recombChance();
    if (rcc != null && rcc >= 1 && rcc <= 100 && !Poe2CraftEngine.jewel(pk)) {
      List<Target> preT = new ArrayList<>();
      List<Target> sufT = new ArrayList<>();
      int preMask = 0;
      int sufMask = 0;
      for (int t = 0; t < targets.size(); t++) {
        Poe2CraftEngine.Mod m = craft.modOf(targets.get(t).anyOf().get(0));
        if (m != null && "suffix".equals(m.gen())) {
          sufT.add(targets.get(t));
          sufMask |= 1 << t;
        } else {
          preT.add(targets.get(t));
          preMask |= 1 << t;
        }
      }
      if (!preT.isEmpty() && !sufT.isEmpty()) {
        double p = rcc / 100.0;
        Double omen =
            calc.prices().divine() == null
                ? null
                : calc.prices().divine().get(Poe2CraftCalcService.RECOMB_OMEN);
        for (boolean sufHalf : new boolean[] {true, false}) {
          Poe2CraftCalcService.Method h =
              calc.half(
                  fresh,
                  sufHalf ? sufT : preT,
                  sufHalf ? "DextralExaltation" : "SinistralExaltation",
                  2_000);
          if (h == null || h.successes() == 0 || h.costPerSuccess() == null) {
            continue;
          }
          String side = sufHalf ? "suf" : "pre";
          String name =
              "재조합기: "
                  + (sufHalf ? "접미" : "접두")
                  + " 반쪽("
                  + h.label()
                  + ")을 따로 만들어 합치기 · 성공 "
                  + rcc
                  + "%";
          String nameEn =
              "Recombinator: make the "
                  + (sufHalf ? "suffix" : "prefix")
                  + " half ("
                  + (h.labelEn() == null ? h.label() : h.labelEn())
                  + ") separately and combine · success "
                  + rcc
                  + "%";
          acts.add(
              new Act(
                  "RECOMB:" + side,
                  null,
                  null,
                  Set.of(),
                  name,
                  h.perSuccess(),
                  // 실패하면 둘 다 잃고 새 베이스부터(Q20b — 그 베이스 값 기대치)
                  Math.max(h.costPerSuccess() + (1 - p) * bp, MIN_COST),
                  false,
                  nameEn));
          recombInfo.put("RECOMB:" + side, new double[] {sufHalf ? preMask : sufMask, p});
          if (omen != null) {
            Map<String, Double> sp = new LinkedHashMap<>(h.perSuccess());
            sp.merge(Poe2CraftCalcService.RECOMB_OMEN, 1d, Double::sum);
            acts.add(
                new Act(
                    "RECOMB:" + side + ":lucky",
                    null,
                    null,
                    Set.of(),
                    name + " + 재조합의 징조",
                    sp,
                    Math.max(h.costPerSuccess() + omen + (1 - p) * (1 - p) * bp, MIN_COST),
                    false,
                    nameEn + " + Omen of Recombination"));
            recombInfo.put(
                "RECOMB:" + side + ":lucky",
                new double[] {sufHalf ? preMask : sufMask, 1 - (1 - p) * (1 - p)});
          }
        }
      }
    }
    List<String> skipped = new ArrayList<>();
    List<Act> usable = new ArrayList<>();
    for (Act a : acts) {
      if (Double.isNaN(a.cost())) {
        skipped.add(a.ko());
      } else if (usable.stream().noneMatch(x -> x.id().equals(a.id()))) {
        usable.add(a);
      }
    }
    Job job = new Job(targets, usable, fresh, started + budget * 92 / 100);
    job.openNeed = openNeed;
    job.openCap = perSide;
    job.recomb.putAll(recombInfo);
    double known = req.upper() != null && req.upper() > 0 ? req.upper() : spamUpper(job);
    if (req.upper() == null) {
      // 시뮬레이터 표의 최저도 위 경계로(C462) — 새 베이스에서(넘김 = 새 베이스 → 그 방법)
      try {
        Poe2CraftCalcService.Response t =
            // 빈 자리 조건까지 넣은 표(C540, PoE1 짝)
            calc.calc(
                new Poe2CraftCalcService.Request(fresh, req.targets(), null, req.recombChance()));
        Poe2CraftCalcService.Method top =
            t.results().stream()
                .filter(x -> x.costPerSuccess() != null)
                .min(Comparator.comparingDouble(Poe2CraftCalcService.Method::costPerSuccess))
                .orElse(null);
        if (top != null && top.costPerSuccess() < known) {
          known = top.costPerSuccess();
          job.upperLabel = top.label();
          job.upperLabelEn = top.labelEn() == null ? top.label() : top.labelEn();
        }
      } catch (RuntimeException e) {
        // 표가 실패해도 연금 위 경계로
      }
    }
    job.upper = known;
    job.restartKey = job.key(fresh);
    // 넘김 = 새 베이스부터(10-23 Q23 검토 — 베이스 값을 넣어도 넘김은 공짜라 정책이 NEW_BASE 대신 넘김을 골라 계획 비용에서 베이스가 빠졌다)
    job.restartCost = Math.max(MIN_COST, calc.basePrice());
    // 넘김 값 시작 — 위 경계가 아주 크면(아는 방법 없음 1e9) 1e7 에서(기울기를 빼는 자릿수 손실, 근보다 작으면 뉴턴이 한 번에 넘어 위에서 내려온다)
    job.effUpper = Math.min(job.upper, 1e7);
    if (req.upper() != null && req.upper() > 0) {
      job.upperLabel =
          req.upperLabel() == null || req.upperLabel().isBlank()
              ? "시뮬레이터 표의 가장 싼 방법"
              : req.upperLabel();
      job.upperLabelEn =
          req.upperLabel() == null || req.upperLabel().isBlank()
              ? "the cheapest method in the simulator table"
              : req.upperLabel();
    }
    final double cap = job.upper;
    usable.removeIf(x -> !x.newBase() && x.cost() >= cap);
    String s0 = job.key(start);
    job.rep.put(s0, start);
    Map<String, Double> v = new HashMap<>();
    Map<String, Integer> pol = new HashMap<>();
    Map<String, Double> vLo = new HashMap<>();
    Map<String, Integer> polLo = new HashMap<>();
    int rounds = 0;
    boolean capped = false;
    boolean converged = false;
    long learnEnd = started + budget * 75 / 100;
    long tLearn = System.currentTimeMillis();
    expand(job, s0);
    while (System.currentTimeMillis() < learnEnd) {
      rounds++;
      decide(job, v, pol, learnEnd);
      valueIteration(job, vLo, polLo, learnEnd, 0, false);
      List<String> frontier = frontier(job, s0, pol, EXPAND_PER_ROUND / 2, new ArrayList<>());
      frontier = frontier(job, s0, polLo, EXPAND_PER_ROUND, frontier);
      if (frontier.isEmpty()) {
        converged = true;
        break;
      }
      final long until = learnEnd;
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
      return error(
          "목표까지 가는 길을 찾지 못했습니다(시간 안에 넓힌 상태 " + job.trans.size() + "개)",
          "No path to the targets was found (" + job.trans.size() + " states expanded in time)");
    }
    // 새 베이스에서 시작하면 첫 베이스 값도(10-23 Q28 — 계산기 표 · 위 경계는 첫 시도부터 베이스를 넣는데 계획 · 모의엔 없어 그만큼 싸 보였다)
    double firstBase = start.affixes().isEmpty() ? calc.basePrice() : 0;
    planCost = planCost + firstBase;
    // 정책을 실제 아이템에 굴려 비용 재기
    Map<String, Double> spent = new LinkedHashMap<>();
    Map<String, Integer> visits = new HashMap<>();
    List<List<Integer>> paths = new ArrayList<>();
    List<Double> runCost = new ArrayList<>();
    // 성공 회차마다 앞서 못 닿은 회차 비용까지(결과 예시 비용 — 분위수와 같은 기준, Q37)
    List<Double> runCostCarry = new ArrayList<>();
    // 목표에 실제로 닿은 회차의 비용(C540) — 분포(운 좋으면 · 보통 · 운 나쁘면)는 이것만. 넘김 · 고리 감시 회차는 '아는 최저'의 기대 비용을 덩어리로
    // 더해 분포처럼 보였다
    List<Double> reachedCost = new ArrayList<>();
    // 성공 회차의 마지막 아이템(C503 결과 예시) — runCost 와 같은 순서
    List<Poe2CraftEngine.Item> finals = new ArrayList<>();
    int runs = 0;
    int succ = 0;
    int fallbacks = 0;
    int cycles = 0;
    int policyRestarts = 0;
    int unfinished = 0;
    int newBaseAct = -1;
    for (int i = 0; i < usable.size(); i++) {
      if (usable.get(i).newBase()) {
        newBaseAct = i;
      }
    }
    // 넘김 이름(C471)
    String fbLabel = job.effUpper < job.upper ? "처음부터 다시(이 조합)" : job.upperLabel;
    String fbLabelEn = job.effUpper < job.upper ? "start over (this combo)" : job.upperLabelEn;
    // 시간 뒤 내다본 상태 수(C469l — 상한 LATE_LOOKS, 없으면 모의가 20초)
    int lateLooks = 0;
    int replaced = 0;
    Map<String, Integer> failedActs = new HashMap<>();
    int stuckStates = 0;
    double failCost = 0;
    // 못 닿은 회차 비용을 다음 성공 회차에 얹는다(10-23 Q31 — 분위수가 성공 회차 비용만 봐 평균 19.5c 옆에 '운 좋으면 · 보통 · 운 나쁘면' 2.0c
    // 셋이 나왔다)
    double carry = 0;
    String diag = "";
    long simEnd = started + budget;
    // 하드 한도(C472f) — 최소 30회를 채우느라 예산을 크게 넘기지 않게(다시 시작이 많은 목표에서 2분 넘게 걸렸다)
    long simHard = simEnd + SIM_GRACE_MS;
    for (;
        runs < SIM_RUNS
            && (runs < 30 || System.currentTimeMillis() < simEnd)
            && System.currentTimeMillis() < simHard;
        runs++) {
      Item it = start;
      double c = firstBase;
      if (firstBase > 0) {
        spent.merge(Poe2CraftCalcService.BASE_ITEM, 1d, Double::sum);
      }
      List<Integer> path = new ArrayList<>();
      Set<String> seenRun = new java.util.HashSet<>();
      ArrayDeque<String> trace = new ArrayDeque<>();
      boolean done = false;
      int sameRun = 0;
      int steps = 0;
      int restartRun = 0;
      // 목표에 실제로 닿았나(C536) — 넘김(아는 최저로) 회차의 마지막 아이템은 목표 아이템이 아니라 결과 예시에서 뺀다
      boolean reached = false;
      // 재조합으로 끝났나(C540) — 목표 도달이지만 실제 아이템은 없다(분포엔 넣고 결과 예시엔 뺀다)
      boolean recombined = false;
      for (int step = 0; step < SIM_STEPS; step++) {
        steps = step;
        if ((step & 4095) == 4095 && System.currentTimeMillis() > simHard) {
          break; // 하드 한도 — 못 끝낸 회차(쓴 비용은 실패 비용으로)
        }
        String k = job.key(it);
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
          c += Math.min(job.upper, job.effUpper);
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
        // 넘김을 실제로(C471c) — 처음부터 다시가 위 경계보다 싸면 새 베이스로 바꿔 이 정책을 이어 간다(기대값을 더하면 모의가 계획 값을 그대로
        // 돌려줄 뿐 확인이 안 된다)
        if (ai != null
            && ai == FALLBACK
            && newBaseAct >= 0
            && restartRun < 5_000
            && job.effUpper < job.upper) {
          ai = newBaseAct;
          restartRun++;
          policyRestarts++;
        }
        if (ai != null && ai == FALLBACK && restartRun >= 5_000 && job.effUpper < job.upper) {
          // 끝내지 못한 회차(C472c) — 다시 시작은 서로 독립인 시도라 쓴 비용만 실패 비용으로 더하고 성공으로 세지 않는다(전체 비용 / 성공 수). 기대값을
          // 더하면 계획 값이 그대로 돌아와 확인이 안 됐다
          unfinished++;
          break;
        }
        Act a = ai == null || ai == FALLBACK ? null : usable.get(ai);
        double[] rcb = a == null ? null : job.recomb.get(a.id());
        if (rcb != null) {
          // 재조합(C472) — 반쪽 비용을 더하고 성공이면 완성, 실패면 새 베이스로
          c += a.cost();
          a.spend().forEach((nm, x) -> spent.merge(nm, x, Double::sum));
          if (path.size() < 2_000) {
            path.add(ai);
          }
          if (job.rng.nextDouble() < rcb[1]) {
            done = true;
            recombined = true;
            break;
          }
          // 실패 — 둘 다 잃고 새 베이스(Q23 검토: 비용엔 기대값 (1 − p)·베이스가 있는데 쓴 것엔 없었다)
          if (calc.basePrice() > 0) {
            spent.merge(Poe2CraftCalcService.BASE_ITEM, 1d, Double::sum);
          }
          it = job.fresh;
          continue;
        }
        Item r = a == null ? null : job.apply(a, it, job.rng);
        // 확률적 실패는 같은 수단을 다시(계획은 성공만 센 분포), 그래도 안 되면 이 아이템에 통하는 수단 중 Q 최소(C470 — PoE1 C469 와 같음)
        for (int retry = 0; retry < 10 && r == null && a != null && !a.newBase(); retry++) {
          r = job.apply(a, it, job.rng);
        }
        if (r == null && a != null && job.trans.get(k) != null) {
          failedActs.merge(a.ko(), 1, Integer::sum);
          List<double[]> qs = new ArrayList<>();
          for (Map.Entry<Integer, Map<String, Double>> e : job.trans.get(k).entrySet()) {
            if (e.getKey().equals(ai)) {
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
            double q = (usable.get(e.getKey()).cost() + rest) / (1 - self);
            if (q < job.effUpper) {
              qs.add(new double[] {q, e.getKey()});
            }
          }
          qs.sort(Comparator.comparingDouble(x -> x[0]));
          for (double[] qa : qs) {
            Act x = usable.get((int) qa[1]);
            Item xr = job.apply(x, it, job.rng);
            if (xr != null) {
              a = x;
              ai = (int) qa[1];
              r = xr;
              replaced++;
              break;
            }
          }
        }
        if (r == null) {
          c += Math.min(job.upper, job.effUpper);
          fallbacks++;
          path.add(FALLBACK);
          done = true;
          break;
        }
        if (job.key(r).equals(k)) {
          if (++sameRun > STUCK_STEPS) {
            pol.put(k, FALLBACK);
            stuckStates++;
          }
        } else {
          sameRun = 0;
        }
        it = r;
        c += a.cost();
        a.spend().forEach((nm, x) -> spent.merge(nm, x, Double::sum));
        if (path.size() < 2_000) {
          path.add(ai);
        }
      }
      if (done) {
        succ++;
        paths.add(path);
        runCost.add(c);
        runCostCarry.add(c + carry);
        finals.add(reached ? it : null);
        if (reached || recombined) {
          reachedCost.add(c + carry);
          carry = 0;
        }
      } else {
        failCost += c;
        carry += c;
        if (diag.isEmpty()) {
          diag = "걸음 " + steps + " · 비용 " + c + " · " + String.join(" ; ", trace);
        }
      }
    }
    double totalCost = runCost.stream().mapToDouble(Double::doubleValue).sum();
    Double expected = succ == 0 ? null : (totalCost + failCost) / succ;
    Map<String, Double> per = new LinkedHashMap<>();
    final int succF = Math.max(1, succ);
    spent.forEach((nm, x) -> per.put(nm, x / succF));
    String pathText = "";
    String pathTextEn = "";
    if (!paths.isEmpty()) {
      List<Integer> order = new ArrayList<>();
      for (int i = 0; i < paths.size(); i++) {
        order.add(i);
      }
      order.sort(Comparator.comparingDouble(runCost::get));
      List<Integer> mid = paths.get(order.get(order.size() / 2));
      List<String> parts = new ArrayList<>();
      List<String> partsEn = new ArrayList<>();
      // 다시 시작(새 베이스)으로 길어지지 않게 — 마지막 다시 시작 뒤만 보이고 그 앞은 횟수로(C461c)
      int last = -1;
      int restarts = 0;
      for (int i = 0; i < mid.size(); i++) {
        if (mid.get(i) != FALLBACK && usable.get(mid.get(i)).newBase()) {
          last = i;
          restarts++;
        }
      }
      if (restarts > 0) {
        parts.add("새 베이스로 다시 시작 ×" + restarts);
        partsEn.add("restart with a new base ×" + restarts);
        mid = mid.subList(last + 1, mid.size());
      }
      for (int i = 0; i < mid.size(); ) {
        int j = i;
        while (j < mid.size() && mid.get(j).equals(mid.get(i))) {
          j++;
        }
        parts.add(label(usable, mid.get(i), fbLabel) + (j - i > 1 ? " ×" + (j - i) : ""));
        partsEn.add(labelEn(usable, mid.get(i), fbLabelEn) + (j - i > 1 ? " ×" + (j - i) : ""));
        i = j;
      }
      pathText = String.join(" → ", parts);
      pathTextEn = String.join(" → ", partsEn);
    }
    final int runsF = Math.max(1, runs);
    List<Rule> rules =
        visits.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(12)
            .map(
                e ->
                    new Rule(
                        describe(e.getKey(), targets.size()),
                        pol.get(e.getKey()) == null
                            ? "(모름)"
                            : label(usable, pol.get(e.getKey()), fbLabel),
                        Math.min(1, (double) e.getValue() / runsF),
                        v.get(e.getKey()),
                        describeEn(e.getKey(), targets.size()),
                        pol.get(e.getKey()) == null
                            ? "(unknown)"
                            : labelEn(usable, pol.get(e.getKey()), fbLabelEn)))
            .toList();
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
    // 모의 성공이 적으면(C563 ⑧) 계획 값을 비용으로 — 성공 1~4번이면 모의 값의 상대 오차가 50~100% 라 같은 요청도 실행마다 몇 배씩 흔들렸다(성공 한
    // 번에
    // 다시 시작 수백 번인 연타 정책). 계획 값은 수만 번 표본의 상태 모형(드문 결과는 신뢰 하한, C563)이라 더 안정적이다. 모의 값은 simCost 로 함께
    Double simCost = null;
    boolean unverified = false;
    if (succ > 0 && succ < FEW_SUCCESSES && planCost != null && planCost > 0 && expected != null) {
      simCost = expected;
      expected = planCost;
      se = null;
    } else if (succ == 0
        && planCost != null
        && planCost > 0
        && totalCost + failCost < 3 * planCost) {
      // 성공 0번이어도 쓴 비용이 계획 값의 3배가 안 되면(기대 성공 < 3 — 10-22 측정 9회 · 성공 0 · 계획 3,639c) 정책이 틀렸다는 근거가 못 된다
      // →
      // 계획 값을 비용으로(검증 전, 화면에 알림). 3배 넘게 쓰고도 0번이면 지금처럼 오류(정책이 실제 아이템에 안 통함)
      expected = planCost;
      se = null;
      unverified = true;
    }
    if (jobId != null) {
      policies.put(
          jobId,
          new Policy(
              System.currentTimeMillis(), job, new HashMap<>(pol), new HashMap<>(v), usable));
    }
    Map<String, String> namesKo = new LinkedHashMap<>();
    per.keySet().forEach(nm -> namesKo.put(nm, ko.getOrDefault(nm, nm)));
    String note =
        "탐색 "
            + (tSim - tLearn)
            + "ms · 모의 "
            + (System.currentTimeMillis() - tSim)
            + "ms · 상태 "
            + job.trans.size()
            + "개 · 수단 "
            + usable.size()
            + "개 · 탐색 "
            + rounds
            + "단계"
            + (refined > 0 ? " · 다듬기 " + refined + "수단(" + tRefine + "ms)" : "")
            + (converged
                ? "(정책이 닿는 상태를 모두 넓힘)"
                : capped ? " · 상태 한도(" + MAX_STATES + "개)로 더 넓히지 못함" : " · 시간 한도로 덜 넓힘")
            + (skipped.isEmpty() ? "" : " · 시세가 없어 뺀 수단 " + skipped.size() + "개")
            + (fallbacks > 0 ? " · " + fbLabel + "로 넘긴 회차 " + fallbacks : "")
            + (policyRestarts > 0 ? " · 새 베이스로 처음부터 " + policyRestarts + "번" : "")
            + (unfinished > 0
                ? " · 다시 시작 5,000번에 못 끝낸 회차 " + unfinished + "(쓴 비용은 넣고 성공으로 안 셈)"
                : "")
            + (stuckStates > 0 ? " · 제자리 걸음으로 넘긴 상태 " + stuckStates : "")
            + (cycles > 0 ? " · 상태 사이를 돌아 넘긴 회차 " + cycles : "")
            + (replaced > 0 ? " · 정책 수단이 이 아이템에 안 통해 바꾼 걸음 " + replaced : "")
            + " · 정책 모의 "
            + runs
            + "회(성공 "
            + succ
            + ")";
    String noteEn =
        "search "
            + (tSim - tLearn)
            + "ms · simulation "
            + (System.currentTimeMillis() - tSim)
            + "ms · "
            + job.trans.size()
            + " states · "
            + usable.size()
            + " actions · "
            + rounds
            + " search rounds"
            + (refined > 0 ? " · refined " + refined + " actions (" + tRefine + "ms)" : "")
            + (converged
                ? " (every state the policy reaches was expanded)"
                : capped
                    ? " · stopped expanding at the state limit (" + MAX_STATES + ")"
                    : " · expansion cut short by the time limit")
            + (skipped.isEmpty() ? "" : " · " + skipped.size() + " actions without prices left out")
            + (fallbacks > 0 ? " · runs handed off to " + fbLabelEn + ": " + fallbacks : "")
            + (policyRestarts > 0
                ? " · restarts from scratch with a new base: " + policyRestarts
                : "")
            + (unfinished > 0
                ? " · runs not finished within 5,000 restarts: "
                    + unfinished
                    + " (cost counted, not counted as success)"
                : "")
            + (stuckStates > 0 ? " · states handed off for not progressing: " + stuckStates : "")
            + (cycles > 0 ? " · runs handed off for cycling between states: " + cycles : "")
            + (replaced > 0
                ? " · steps replaced because the policy action did not apply: " + replaced
                : "")
            + " · policy simulation "
            + runs
            + " runs ("
            + succ
            + " successes)";
    if (unverified) {
      note += " · 모의 회차가 아직 목표에 닿지 못해 비용은 계획 값(검증 전)";
      noteEn +=
          " · no simulated run reached the target yet, so the cost shown is the unverified plan value";
    }
    if (calc.basePrice() <= 0 && pathText.contains("새 베이스")) {
      note += " · 새 베이스 값은 빠져 있습니다('가격 직접 입력'의 베이스 아이템에 넣으면 포함)";
      noteEn +=
          " · new base items are not costed (set 'Base item' in Custom prices to include them)";
    }
    if (simCost != null) {
      note += " · 모의 성공 " + succ + "번뿐이라 비용은 계획 값(모의 값 " + String.format("%.2f", simCost) + "div)";
      noteEn +=
          " · only "
              + succ
              + " simulated successes, so the cost shown is the plan value (simulated "
              + String.format("%.2f", simCost)
              + "div)";
    }
    Poe2CraftCalcService.Prices p = calc.prices();
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
      ord.sort(java.util.Comparator.comparingDouble(runCost::get));
      java.util.LinkedHashSet<Integer> pick = new java.util.LinkedHashSet<>();
      for (double q : new double[] {0.25, 0.5, 0.75}) {
        pick.add(ord.get(Math.min(ord.size() - 1, (int) Math.floor(q * ord.size()))));
      }
      for (int i : pick) {
        Poe2CraftService.View fv = craft.view(finals.get(i));
        List<ShowLine> sl = new ArrayList<>();
        for (Poe2CraftService.Line l : fv.lines()) {
          sl.add(
              new ShowLine(
                  l.id(),
                  l.gen(),
                  l.name(),
                  l.nameKo(),
                  l.en(),
                  l.ko(),
                  List.of(),
                  List.of(),
                  l.tierRank(),
                  l.level(),
                  false,
                  l.fractured(),
                  false));
        }
        showcase.add(new Showcase(runCostCarry.get(i), fv.item().rarity().name(), sl));
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
        namesKo,
        pathText,
        rules,
        usable.stream().map(Act::ko).toList(),
        p.league(),
        p.fetchedAt(),
        note,
        succ == 0 && expected == null
            ? "정책 모의에서 목표에 닿지 못했습니다"
                + (unfinished > 0
                    ? " — 새 베이스로 처음부터 다시를 "
                        + policyRestarts
                        + "번 해도 목표 0번이라 계획 값은 확인되지 않았습니다(드문 결과에 기댄 값)"
                        + (job.upper < 1e8 ? ". 아는 가장 싼 방법: " + job.upperLabel : "")
                    : "")
            : null,
        failedActs.isEmpty()
            ? diag
            : "안 통한 정책 수단 " + failedActs + (diag.isEmpty() ? "" : " · " + diag),
        showcase,
        quant,
        job.upperLabelEn,
        pathTextEn,
        usable.stream().map(Act::en).toList(),
        noteEn,
        succ == 0 && expected == null
            ? "No run reached the targets in the policy simulation"
                + (unfinished > 0
                    ? " — even after "
                        + policyRestarts
                        + " restarts with a new base there were 0 successes, so the planned value is"
                        + " unconfirmed (it relies on rare outcomes)"
                        + (job.upper < 1e8 ? ". Cheapest known method: " + job.upperLabelEn : "")
                    : "")
            : null,
        simCost);
  }

  private static String label(List<Act> usable, int ai, String upperLabel) {
    return ai == FALLBACK ? "새 베이스 → " + upperLabel : usable.get(ai).ko();
  }

  /** label 의 영어 판(C561). */
  private static String labelEn(List<Act> usable, int ai, String upperLabelEn) {
    return ai == FALLBACK ? "New base → " + upperLabelEn : usable.get(ai).en();
  }

  /** 연금 연타 위 경계 — 새 베이스마다 연금(징조 없음, 기본 등급) 한 번, 성공 한 번당 비용. 한 번도 안 나오면 1e9. */
  private double spamUpper(Job job) {
    Act alch =
        job.acts.stream()
            .filter(a -> a.action() == Action.ALCHEMY && a.omens().isEmpty() && "".equals(a.arg()))
            .findFirst()
            .orElse(null);
    if (alch == null) {
      return 1e9;
    }
    int hits = 0;
    for (int i = 0; i < SPAM_SAMPLE; i++) {
      Item it = job.apply(alch, job.fresh, job.rng);
      if (it != null && job.met(it)) {
        hits++;
      }
    }
    if (hits == 0) {
      return 1e9;
    }
    job.upperLabel = alch.ko() + " 연타(새 베이스마다)";
    job.upperLabelEn = alch.en() + " spam (new base each time)";
    // 시도마다 새 베이스 값도(Q23 검토 — 계산기 표의 연금 연타와 같게, 빼면 위 경계가 수십 배 낮아 정상 수단이 잘렸다)
    return (alch.cost() + calc.basePrice()) * SPAM_SAMPLE / hits;
  }

  /**
   * 다듬기(C467) — 정책이 처음 상태에서 닿는 넓힌 상태(넓이 우선, REFINE_STATES 개까지)마다 한 걸음 수단 중 Q 가 낮은 REFINE_ACTS 개를
   * SAMPLE_REFINE 번으로 다시 떠 전이를 바꾸고 가치 반복을 다시. 두 번(바뀐 정책이 새로 고른 수단도). 다시 굴리기 · 새 베이스(공유 분포)는 그대로. 다시
   * 뜬 수단 수.
   */
  private static int refine(
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
          if (job.shared(k, job.acts.get(e.getKey())) || done.contains(k + "#" + e.getKey())) {
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

  /** 가치 반복(PoE1 C459g 와 같음) — 같은 분포는 한 번만 합하고, 위 경계 이상이면 '새 베이스 → 아는 최저'. */
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
              continue;
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
        double sum = dBase.get(d) * u;
        double dsum = dBase.get(d);
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

  private static List<String> frontier(
      Job job, String s0, Map<String, Integer> pol, int max, List<String> into) {
    ArrayDeque<String> q = new ArrayDeque<>(List.of(s0));
    Set<String> seen = new java.util.HashSet<>(List.of(s0));
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

  /** 수단 하나 — 시세를 모르는 화폐 · 징조가 끼면 비용 NaN(고려에서 뺀다). */
  private Act act(
      String id,
      Action action,
      String arg,
      List<String> omens,
      String koLabel,
      String enLabel,
      Poe2CraftCalcService.Names names,
      boolean newBase) {
    Map<String, Double> m =
        calc.stepNames(new Poe2CraftCalcService.SpentStep(action.name(), arg, omens), names);
    Double c = m.isEmpty() ? null : calc.cost(m);
    return new Act(
        id,
        action,
        arg,
        Set.copyOf(omens),
        koLabel,
        m,
        c == null ? Double.NaN : Math.max(c, MIN_COST),
        newBase,
        enLabel);
  }

  /** describe 의 영어 판(10-22 C561). 같은 열쇠 규칙. */
  static String describeEn(String key, int n) {
    String[] f = key.split("\\|", -1);
    String rar =
        switch (f[0]) {
          case "N" -> "Normal";
          case "M" -> "Magic";
          default -> "Rare";
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
    sb.append(" · ")
        .append(met.isEmpty() ? "no targets hit" : "targets " + String.join("·", met) + " hit");
    if (!"0".equals(j[0]) || !"0".equals(j[1])) {
      sb.append(" · other mods prefix ").append(j[0]).append("/suffix ").append(j[1]);
    }
    if (!bl.isEmpty()) {
      sb.append(" · target ").append(String.join("·", bl)).append(" slot blocked by other mods");
    }
    if (f[4].contains("Dp")) {
      sb.append(" · unrevealed desecrated prefix");
    }
    if (f[4].contains("Ds")) {
      sb.append(" · unrevealed desecrated suffix");
    }
    return sb.toString();
  }

  /** 상태 설명 — "레어 · 목표 1·3 맞음 · 잡옵션 접두 1/접미 2 · 미공개 훼손 접두". */
  static String describe(String key, int n) {
    String[] f = key.split("\\|", -1);
    String rar =
        switch (f[0]) {
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
    sb.append(" · ").append(met.isEmpty() ? "맞은 목표 없음" : "목표 " + String.join("·", met) + " 맞음");
    if (!"0".equals(j[0]) || !"0".equals(j[1])) {
      sb.append(" · 잡옵션 접두 ").append(j[0]).append("/접미 ").append(j[1]);
    }
    if (!bl.isEmpty()) {
      sb.append(" · 목표 ").append(String.join("·", bl)).append(" 자리를 잡옵션이 막음");
    }
    if (f[4].contains("Dp")) {
      sb.append(" · 미공개 훼손 접두");
    }
    if (f[4].contains("Ds")) {
      sb.append(" · 미공개 훼손 접미");
    }
    return sb.toString();
  }

  private Result error(String msg, String en) {
    return new Result(
        null, null, null, null, null, 0, 0, 0, Map.of(), Map.of(), "", List.of(), List.of(), null,
        null, null, msg, "", List.of(), List.of(), null, "", List.of(), null, en, null);
  }
}
