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
      List<String> methods) {}

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
      String error) {}

  /** 백그라운드 작업 상태(C457k) — state = running | done, elapsedMs · budgetMs = 진행률, result = 끝났을 때. */
  public record JobStatus(String id, String state, long elapsedMs, long budgetMs, Result result) {}

  private record Running(long started, long budget, java.util.concurrent.Future<Result> future) {}

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
            req.item(), req.targets(), (int) budget, req.upper(), req.upperLabel(), req.methods());
    String id = java.util.UUID.randomUUID().toString();
    jobs.put(id, new Running(now, budget, pool.submit(() -> optimize(r))));
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

  static final int MAX_STATES = 2_000;
  // 다시 굴리기는 남는 옵션이 같은 상태끼리 결과 분포를 함께 써 종류가 적다 — 드문 결과(목표 둘이 한 번에)의 확률을 잡도록 크게(C457e: 1,200 이면 목표 둘
  // 매직 확률을
  // 몇 번 안 되는 성공으로 잡아 계획 82c ↔ 실제 202c)
  static final int SAMPLE_REROLL = 30_000;
  static final int SAMPLE_STEP = 400;
  static final int SIM_RUNS = 400;
  static final int SIM_STEPS = 50_000;
  static final long DEFAULT_BUDGET_MS = 8_000;

  /** 백그라운드 작업 기본 예산(C457k) — 게이트가 조회하며 기다린다. */
  static final long JOB_BUDGET_MS = 20_000;

  static final int JOB_THREADS = 2;
  static final long JOB_KEEP_MS = 10 * 60_000L;
  static final int EXPAND_PER_ROUND = 40;
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
    final List<List<Integer>> targetGroups = new ArrayList<>();
    final List<java.util.Set<String>> goalSets = new ArrayList<>();
    // 옵션 id → 채우는 목표 비트, 옵션 그룹 → 그 그룹이 막는 목표 비트(C457l)
    final Map<String, Integer> idMask = new HashMap<>();
    final Map<Integer, Integer> groupMask = new HashMap<>();
    final String lockPre;
    final String lockSuf;
    final String multi;
    final List<Act> acts;
    final Map<String, Item> rep = new LinkedHashMap<>();
    final Map<String, Map<Integer, Map<String, Double>>> trans = new HashMap<>();
    final Map<String, Map<String, Double>> rerollCache = new HashMap<>();
    final long deadline;
    final SplittableRandom rng = new SplittableRandom(457);
    double upper = 1e9;
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
        if (used[i]) {
          continue;
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
          + new String(c);
    }

    boolean goalKey(String k) {
      return Integer.parseInt(k.split("\\|")[1]) == (1 << goal.size()) - 1;
    }

    /** 수단 a 를 상태 k(대표 아이템)에 쓴 결과 분포 — 쓸 수 없으면 null. 기억 없는 다시 굴리기는 남는 옵션이 같으면 결과 분포가 같아 함께 쓴다. */
    Map<String, Double> sample(String k, int ai) {
      Item it = rep.get(k);
      Act a = acts.get(ai);
      boolean reroll = rerolls(a.action());
      String ck = null;
      if (reroll) {
        StringBuilder sb = new StringBuilder(a.id()).append('|').append(it.rarity()).append('|');
        PoeCraftEngine.kept(it).stream()
            .map(x -> x.id() + (x.crafted() ? "c" : ""))
            .sorted()
            .forEach(x -> sb.append(x).append(','));
        ck = sb.toString();
        Map<String, Double> hit = rerollCache.get(ck);
        if (hit != null) {
          return hit.isEmpty() ? null : hit;
        }
      }
      int n = reroll ? SAMPLE_REROLL : SAMPLE_STEP;
      Map<String, Double> out = new HashMap<>();
      int ok = 0;
      for (int i = 0; i < n; i++) {
        // 한 번 넓히기가 길어 예산을 넘지 않게(C457k — 9.9초) — 모자란 표본도 그대로 쓴다
        if ((i & 255) == 255 && System.currentTimeMillis() > deadline) {
          break;
        }
        PoeCraftEngine.Result r = engine.apply(it, a.action(), a.arg(), rng);
        if (r.error() != null) {
          if (i >= 20 && ok == 0) {
            break; // 이 상태엔 못 쓴다
          }
          continue;
        }
        ok++;
        String nk = key(r.item());
        rep.putIfAbsent(nk, r.item());
        out.merge(nk, 1d, Double::sum);
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
      // 결과가 그대로인 수단(같은 상태로만 감)은 버린다
      if (out.size() == 1 && out.containsKey(k)) {
        if (ck != null) {
          rerollCache.put(ck, Map.of());
        }
        return null;
      }
      if (ck != null) {
        rerollCache.put(ck, out);
      }
      return out;
    }
  }

  private static boolean rerolls(Action a) {
    return switch (a) {
      case CHAOS, ALTERATION, ESSENCE, FOSSIL, ALCHEMY, TRANSMUTE, HARVEST, SCOUR -> true;
      default -> false;
    };
  }

  public Result optimize(Request req) {
    long started = System.currentTimeMillis();
    if (req.targets() == null || req.targets().isEmpty()) {
      return error("목표 옵션을 하나 이상 고르세요");
    }
    if (req.targets().size() > 6) {
      return error("목표는 여섯 개까지입니다");
    }
    Item start = craft.normalize(req.item());
    if (start.corrupted()) {
      return error("타락한 아이템은 바꿀 수 없습니다");
    }
    long budget =
        Math.max(
            2_000, Math.min(40_000, req.budgetMs() == null ? DEFAULT_BUDGET_MS : req.budgetMs()));
    String cls = start.itemClass();
    PoeCraftService.Options o = craft.options(cls, start.variant() == null ? "" : start.variant());
    List<Target> targets = req.targets();
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
      PoeCraftDataService.Essence cur = ess.get(e.family());
      if (cur == null || e.tier() > cur.tier()) {
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
    // 화석 · 수확 — 계산기가 고르는 것과 같은 상위 조합(짧게)
    long tFossil = System.currentTimeMillis();
    List<String[]> picked = new ArrayList<>();
    if (req.methods() != null) {
      for (String m : req.methods()) {
        if (m != null && (m.startsWith("FOSSIL:") || m.startsWith("HARVEST:"))) {
          picked.add(new String[] {m, m});
        }
      }
    } else {
      for (PoeCraftCalcService.MethodResult r : calc.fossils(start, targets, 4_000)) {
        picked.add(new String[] {r.method(), r.label()});
      }
      for (PoeCraftCalcService.MethodResult r : calc.harvests(start, targets, 4_000)) {
        picked.add(new String[] {r.method(), r.label()});
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
    acts.add(act("BEAST:swap-prefix", Action.BEAST, "swap-prefix", "짐승: 접두어 추가, 무작위 접미어 제거", cls));
    acts.add(act("BEAST:swap-suffix", Action.BEAST, "swap-suffix", "짐승: 접미어 추가, 무작위 접두어 제거", cls));
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
    // 위 경계 — 정제 + 아는 가장 싼 방법. 계산기 표 최저가 없으면 카오스 · 변화(+확장) · 에센스 연타를 표본으로 잰다
    double scour =
        usable.stream()
            .filter(x -> x.action() == Action.SCOUR)
            .mapToDouble(Act::cost)
            .findFirst()
            .orElse(1);
    double known = req.upper() != null && req.upper() > 0 ? req.upper() : spamUpper(job, start);
    job.upper = known + scour;
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
    long learnEnd = started + budget * 85 / 100;
    // 탐색 안내용 낙관 값(넓히지 않은 상태 0) — 위 경계만 쓰면 모르는 곳이 늘 비싸 보여 넓히지 않는다
    Map<String, Double> vLo = new HashMap<>();
    Map<String, Integer> polLo = new HashMap<>();
    long tLearn = System.currentTimeMillis();
    expand(job, s0);
    while (System.currentTimeMillis() < learnEnd) {
      rounds++;
      valueIteration(job, v, pol, learnEnd, job.upper, true);
      valueIteration(job, vLo, polLo, learnEnd, 0, false);
      // 결정 정책이 닿는 넓히지 않은 상태(다듬기) + 낙관 정책이 닿는 것(찾기), 반씩
      List<String> frontier = frontier(job, s0, pol, EXPAND_PER_ROUND / 2, new ArrayList<>());
      frontier = frontier(job, s0, polLo, EXPAND_PER_ROUND, frontier);
      if (frontier.isEmpty()) {
        converged = true;
        break;
      }
      for (String k : frontier) {
        if (job.trans.size() >= MAX_STATES || System.currentTimeMillis() >= learnEnd) {
          capped = true;
          break;
        }
        expand(job, k);
      }
      if (capped) {
        break;
      }
    }
    valueIteration(job, v, pol, started + budget * 7 / 10, job.upper, true);
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
    int runs = 0;
    int succ = 0;
    int fallbacks = 0;
    long simEnd = started + budget;
    for (; runs < SIM_RUNS && (runs < 30 || System.currentTimeMillis() < simEnd); runs++) {
      Item it = start;
      double c = 0;
      List<Integer> path = new ArrayList<>();
      // 이 회차에 지난 상태(지나는 회차 비율 — 한 회차에 여러 번 지나도 한 번)
      java.util.Set<String> seenRun = new java.util.HashSet<>();
      boolean done = false;
      for (int step = 0; step < SIM_STEPS; step++) {
        String k = job.key(it);
        if (job.goalKey(k)) {
          done = true;
          break;
        }
        if (seenRun.add(k)) {
          visits.merge(k, 1, Integer::sum);
        }
        Integer ai = pol.get(k);
        if (ai == null && System.currentTimeMillis() < simEnd) {
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
                rest += sp.getValue() * (nv != null ? nv : job.upper);
              }
            }
            double qv = (usable.get(x).cost() + rest) / (1 - self);
            if (qv < best) {
              best = qv;
              ai = x;
            }
          }
          if (ai != null && best >= job.upper) {
            ai = FALLBACK;
          }
          if (ai != null) {
            pol.put(k, ai);
          }
        }
        Act a = ai == null || ai == FALLBACK ? null : usable.get(ai);
        PoeCraftEngine.Result r =
            a == null ? null : job.engine.apply(it, a.action(), a.arg(), job.rng);
        if (r == null || r.error() != null) {
          // '정제 → 아는 최저'(정책이거나, 근사 차이로 정책 수단을 못 쓸 때) — 그 기대 비용을 더하고 끝
          c += job.upper;
          fallbacks++;
          path.add(FALLBACK);
          done = true;
          break;
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
      }
    }
    double totalCost = runCost.stream().mapToDouble(Double::doubleValue).sum();
    Double expected = succ == 0 ? null : totalCost / succ;
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
      for (int i = 0; i < mid.size(); ) {
        int j = i;
        while (j < mid.size() && mid.get(j).equals(mid.get(i))) {
          j++;
        }
        parts.add(label(usable, mid.get(i), job.upperLabel) + (j - i > 1 ? " ×" + (j - i) : ""));
        i = j;
      }
      pathText = String.join(" → ", parts);
    }
    // 자주 지나는 상태의 결정
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
                            : label(usable, pol.get(e.getKey()), job.upperLabel),
                        Math.min(1, (double) e.getValue() / runsF),
                        v.get(e.getKey())))
            .toList();
    PoeCraftPriceService.Prices p = prices.prices();
    String note =
        "준비 "
            + (tLearn - started)
            + "ms(화석 · 수확 "
            + (tLearn - tFossil)
            + "ms) · 탐색 "
            + (tSim - tLearn)
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
            + (converged ? "(정책이 닿는 상태를 모두 넓힘)" : capped ? " · 상태 한도로 더 넓히지 못함" : " · 시간 한도로 덜 넓힘")
            + (skipped.isEmpty() ? "" : " · 시세가 없어 뺀 수단: " + String.join(", ", skipped))
            + (fallbacks > 0 ? " · 정제 → 아는 최저로 넘긴 회차 " + fallbacks + "(계산기 표 기대 비용으로 셈)" : "")
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
    Map<String, String> ko = new LinkedHashMap<>();
    Map<String, String> all = calc.spentKo(cls);
    per.keySet().forEach(n -> ko.put(n, all.getOrDefault(n, n)));
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
        succ == 0 ? "정책 모의에서 목표에 닿지 못했습니다" : null);
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
        if (a.action() == Action.ALTERATION
            && it.affixes().size() < 2
            && !PoeCraftCalcService.meets(it, job.goal)) {
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
        if (PoeCraftCalcService.meets(it, job.goal)) {
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

  /** 상태 넓히기 — 수단마다 결과 분포 표본. */
  private static void expand(Job job, String k) {
    Map<Integer, Map<String, Double>> tk = new HashMap<>();
    for (int ai = 0; ai < job.acts.size(); ai++) {
      Map<String, Double> t = job.sample(k, ai);
      if (t != null) {
        tk.put(ai, t);
      }
    }
    job.trans.put(k, tk);
  }

  /**
   * 가치 반복(가우스-자이델) — 넓힌 상태마다 v = min_a (비용 + Σ p · v') / (1 − 제자리 확률). 넓히지 않은 상태는 v = 위 경계(정제 + 아는
   * 최저), 목표는 0. 위에서 내려온다. 변화가 충분히 작거나 시간이 다 되면 멈춘다.
   */
  private static void valueIteration(
      Job job,
      Map<String, Double> v,
      Map<String, Integer> pol,
      long until,
      double unexplored,
      boolean fallback) {
    // 전이를 배열로(C457m) — 다음 상태 번호 · 확률, 넓히지 않은 몫 · 제자리 몫은 따로
    List<String> keys = new ArrayList<>(job.trans.keySet());
    Map<String, Integer> idx = new HashMap<>();
    for (int i = 0; i < keys.size(); i++) {
      idx.put(keys.get(i), i);
    }
    int ns = keys.size();
    int[][] aId = new int[ns][];
    double[][] aBase = new double[ns][];
    double[][] aSelf = new double[ns][];
    int[][][] sIdx = new int[ns][][];
    double[][][] sP = new double[ns][][];
    for (int i = 0; i < ns; i++) {
      String k = keys.get(i);
      Map<Integer, Map<String, Double>> tk = job.trans.get(k);
      int na = tk.size();
      aId[i] = new int[na];
      aBase[i] = new double[na];
      aSelf[i] = new double[na];
      sIdx[i] = new int[na][];
      sP[i] = new double[na][];
      int x = 0;
      for (Map.Entry<Integer, Map<String, Double>> ta : tk.entrySet()) {
        double self = 0;
        double unexp = 0;
        List<Integer> si = new ArrayList<>();
        List<Double> sp = new ArrayList<>();
        for (Map.Entry<String, Double> e : ta.getValue().entrySet()) {
          String nk = e.getKey();
          if (nk.equals(k)) {
            self += e.getValue();
          } else if (job.goalKey(nk)) {
            // 목표 = 0
          } else {
            Integer ni = idx.get(nk);
            if (ni == null) {
              unexp += e.getValue();
            } else {
              si.add(ni);
              sp.add(e.getValue());
            }
          }
        }
        aId[i][x] = ta.getKey();
        aBase[i][x] = job.acts.get(ta.getKey()).cost() + unexp * unexplored;
        aSelf[i][x] = self;
        sIdx[i][x] = si.stream().mapToInt(Integer::intValue).toArray();
        sP[i][x] = sp.stream().mapToDouble(Double::doubleValue).toArray();
        x++;
      }
    }
    double[] val = new double[ns];
    int[] best = new int[ns];
    for (int i = 0; i < ns; i++) {
      val[i] = v.getOrDefault(keys.get(i), unexplored);
      best[i] = Integer.MIN_VALUE;
    }
    for (int it = 0; it < VI_SWEEPS; it++) {
      double change = 0;
      for (int i = 0; i < ns; i++) {
        double b = Double.MAX_VALUE;
        int ba = Integer.MIN_VALUE;
        for (int x = 0; x < aId[i].length; x++) {
          double self = aSelf[i][x];
          if (self >= 0.999999) {
            continue;
          }
          double rest = aBase[i][x];
          int[] si = sIdx[i][x];
          double[] sp = sP[i][x];
          for (int y = 0; y < si.length; y++) {
            rest += sp[y] * val[si[y]];
          }
          double qv = rest / (1 - self);
          if (qv < b) {
            b = qv;
            ba = aId[i][x];
          }
        }
        if (ba == Integer.MIN_VALUE) {
          continue;
        }
        // 위 경계 이상이면 '정제 → 아는 최저'(늘 할 수 있다) — 값이 같은 수단끼리 고리를 돌지 않게 명시적으로
        if (fallback && b >= job.upper) {
          b = job.upper;
          ba = FALLBACK;
        }
        change = Math.max(change, Math.abs(b - val[i]) / Math.max(1e-6, b));
        val[i] = b;
        best[i] = ba;
      }
      if (change < 1e-5 || ((it & 15) == 15 && System.currentTimeMillis() > until)) {
        break;
      }
    }
    for (int i = 0; i < ns; i++) {
      if (best[i] != Integer.MIN_VALUE) {
        v.put(keys.get(i), val[i]);
        pol.put(keys.get(i), best[i]);
      }
    }
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
              default -> "다른 제작 옵션";
            });
      }
      sb.append(" · ").append(String.join(", ", c));
    }
    return sb.toString();
  }

  private Result error(String msg) {
    return new Result(
        null, null, null, null, null, 0, 0, 0, Map.of(), Map.of(), "", List.of(), List.of(), null,
        null, null, msg);
  }
}
