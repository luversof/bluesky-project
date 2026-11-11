package net.luversof.api.poe.poe2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.stereotype.Service;

/**
 * PoE2 제작 시뮬레이터(10-15 C352, P5) — 목표 옵션 묶음 → 방법별 성공률 · 성공 한 번당 화폐 개수. PoE2 옵션 가중치는 비공개라 균등 가정(참고용).
 *
 * <ul>
 *   <li>카오스 반복(기본 · 상위 · 완벽): 연금 한 번 → 카오스를 목표까지(PoE2 카오스는 옵션 하나만 바꿔 상태가 남는다 — 회차 단위 모의, 최대 횟수)
 *   <li>엑잘 쌓기(기본 · 완벽): 진화 → (확장) → 제왕 → 엑잘(목표를 채우거나 막히면 멈춤, 최대 6옵션), 매번 새 베이스(시도마다 독립)
 *   <li>연금 연타: 새 베이스에 연금 한 번(독립)
 * </ul>
 */
@Service
public class Poe2CraftCalcService {

  public record Target(List<String> anyOf) {}

  /** recombChance = 반쪽 재조합(C409)에 쓸 재조합 성공 확률(인게임 화면 값 1~100, 없으면 그 방법은 뺀다). */
  public record Request(
      Poe2CraftEngine.Item item,
      List<Target> targets,
      Integer runs,
      Integer recombChance,
      // 사용자 가격(C530, PoE1 C529 짝) — 이름 → 디바인, 없으면 시세
      Map<String, Double> prices) {

    public Request(Poe2CraftEngine.Item item, List<Target> targets, Integer runs) {
      this(item, targets, runs, null, null);
    }

    public Request(
        Poe2CraftEngine.Item item, List<Target> targets, Integer runs, Integer recombChance) {
      this(item, targets, runs, recombChance, null);
    }
  }

  /** 방법 하나 — successRate = 회차(또는 시도) 성공률, perSuccess = 성공 한 번당 화폐 개수(이름 → 평균). */
  public record Method(
      String method,
      String label,
      int runs,
      int successes,
      double successRate,
      Map<String, Double> perSuccess,
      // 성공 한 번당 비용(디바인, poe.ninja 시세 — 시세 없는 화폐가 있으면 null)
      Double costPerSuccess,
      String note,
      // 영어 화면용(10-22 C561 ⑦b — PoE1 C557 짝). 없으면 null(게이트가 한국어로)
      String labelEn,
      String noteEn) {

    public Method(
        String method,
        String label,
        int runs,
        int successes,
        double successRate,
        Map<String, Double> perSuccess,
        Double costPerSuccess,
        String note) {
      this(
          method,
          label,
          runs,
          successes,
          successRate,
          perSuccess,
          costPerSuccess,
          note,
          null,
          null);
    }
  }

  public record Response(
      List<Method> results, String league, String pricesAt, Double exaltPerDivine, String error) {}

  /**
   * 전략 단계(C355) — action = 화폐 동작 | SEQ(묶음), arg = 등급 · 에센스 · 뼈 슬러그 · 드러낼 옵션(없으면 목표에 맞는 것), omens =
   * 이 단계 화폐에 걸 징조, until = 채울 목표 번호(없으면 한 번), max = 최대 반복.
   */
  public record Step(
      String action,
      String arg,
      List<String> omens,
      List<Integer> until,
      Integer max,
      List<Step> sub) {}

  public record StrategyRequest(
      Poe2CraftEngine.Item item,
      List<Target> targets,
      List<Step> steps,
      Integer runs,
      // 사용자 가격(10-23 Q20a, PoE1 짝)
      Map<String, Double> prices) {

    public StrategyRequest(
        Poe2CraftEngine.Item item, List<Target> targets, List<Step> steps, Integer runs) {
      this(item, targets, steps, runs, null);
    }
  }

  public record StepStat(String action, String arg, double avgUses, double reached) {}

  /** 전략 결과 — perRun = 회차당 평균 개수(화폐 · 에센스 · 뼈 · 징조), 비용은 디바인(시세 없는 것이 있으면 null). */
  public record StrategyResult(
      int runs,
      int successes,
      double successRate,
      Map<String, Double> perRun,
      Double costPerRun,
      Double expectedCost,
      List<StepStat> steps,
      String error,
      // 성공 예시(10-22 Q17, PoE1 Q16 짝 — craftofexile 시뮬레이터 'Store Items'). 처음 STRATEGY_SAMPLES 개
      List<StrategySample> samples) {

    public StrategyResult(
        int runs,
        int successes,
        double successRate,
        Map<String, Double> perRun,
        Double costPerRun,
        Double expectedCost,
        List<StepStat> steps,
        String error) {
      this(runs, successes, successRate, perRun, costPerRun, expectedCost, steps, error, List.of());
    }
  }

  /** 성공 예시 한 개 — 아이템(제작 화면 state) · 표시 줄. */
  public record StrategySample(Poe2CraftEngine.Item item, List<Poe2CraftService.Line> lines) {}

  static final int STRATEGY_SAMPLES = 5;
  static final int DEFAULT_RUNS = 300;
  static final int CHAOS_MAX = 1000;
  // 게이트 API 대기 10초(10-15 실측: 어려운 목표에서 방법마다 150만 번 → 시간 초과) — 방법마다 30만 번, 전체 벽시계 6초
  static final long OPS_BUDGET = 300_000L;
  static final long WALL_MS = 6_000L;

  private final Poe2CraftService craft;
  private final java.nio.file.Path pricesFile;
  // 이번 계산의 벽시계 마감(요청마다 다시) — 넘으면 그때까지의 회차 · 시도로 결과를 낸다
  // 요청 스레드마다(동시 요청이 서로의 마감을 덮어쓰지 않게)
  private final ThreadLocal<Long> deadline = ThreadLocal.withInitial(() -> Long.MAX_VALUE);

  public Poe2CraftCalcService(
      Poe2CraftService craft,
      @org.springframework.beans.factory.annotation.Value(
              "${poe2.data-dir:${user.home}/.poe-gamedata/poe2}")
          String dataDir) {
    this.craft = craft;
    this.pricesFile = java.nio.file.Path.of(dataDir, "ninja", "craft-prices2.json");
  }

  public record Prices(String league, String fetchedAt, String unit, Map<String, Double> divine) {}

  /** 사용자 가격(10-21 C530, PoE1 C529 짝) — 이 요청(스레드) 동안 이름 → 디바인을 시세보다 먼저(prices() 가 합친다). */
  private static final ThreadLocal<Map<String, Double>> OVERRIDE = new ThreadLocal<>();

  /** 사용자 가격 걸기 — null · 빈 맵이면 푼다. 이름 80자 · 값 0 초과 1천만 미만 · 60개까지만. */
  public static void override(Map<String, Double> prices) {
    if (prices == null || prices.isEmpty()) {
      OVERRIDE.remove();
      return;
    }
    Map<String, Double> ok = new java.util.HashMap<>();
    for (Map.Entry<String, Double> e : prices.entrySet()) {
      Double v = e.getValue();
      if (e.getKey() != null
          && e.getKey().length() <= 80
          && v != null
          && Double.isFinite(v)
          && v > 0
          && v < 1e7
          && ok.size() < 60) {
        ok.put(e.getKey(), v);
      }
    }
    if (ok.isEmpty()) {
      OVERRIDE.remove();
    } else {
      OVERRIDE.set(Map.copyOf(ok));
    }
  }

  /**
   * 에뮬레이터에서 쓴 단계 하나(C366) — action = 화폐(FORESEE = 히네코라의 머리카락), arg = 등급 · 에센스 · 뼈, omens = 그때 쓰인 징조
   * 키. times = 같은 화폐를 연달아 쓴 횟수(C508 목표까지 연타, 없으면 1).
   */
  public record SpentStep(String action, String arg, List<String> omens, Integer times) {

    public SpentStep(String action, String arg, List<String> omens) {
      this(action, arg, omens, null);
    }
  }

  public record SpentRequest(
      String poolKey,
      List<SpentStep> steps,
      // 사용자 가격(C530)
      Map<String, Double> prices) {

    public SpentRequest(String poolKey, List<SpentStep> steps) {
      this(poolKey, steps, null);
    }
  }

  /** 화폐 한 줄 — 개수 · 단가(디바인, 시세 없으면 null) · 합계. */
  public record SpentLine(
      String name, String nameKo, double count, Double unitDivine, Double subtotalDivine) {}

  public record SpentResult(
      List<SpentLine> lines,
      double totalDivine,
      int missing,
      String league,
      String pricesAt,
      // 요청 단계마다(C453) — 같은 순서
      List<StepCost> steps) {}

  /** 한 단계 비용(C453) — 쓴 화폐 이름(개수 붙임) · 한국어 이름 · 디바인(시세 없는 화폐가 끼면 null). */
  public record StepCost(List<String> names, List<String> namesKo, Double divine) {}

  /** 쓴 화폐 집계(C366) — 전략 모의와 같은 이름 대응(Names), 드러내기 · 직접 편집은 비용 없음. 처음 쓴 순. */
  public SpentResult spent(SpentRequest req) {
    Names names = names(req.poolKey());
    Map<String, Double> m = new LinkedHashMap<>();
    // 단계별(C453 — 에뮬레이터 단계 기록에 그 단계 비용)
    List<Map<String, Double>> perStep = new ArrayList<>();
    for (SpentStep s : req.steps() == null ? List.<SpentStep>of() : req.steps()) {
      Map<String, Double> sm = stepNames(s, names);
      if (s != null && s.times() != null && s.times() > 1) {
        // 목표까지 연타(C508) — 한 단계에 여러 번
        sm.replaceAll((k, v) -> v * s.times());
      }
      perStep.add(sm);
      sm.forEach((k, v) -> m.merge(k, v, Double::sum));
    }
    Map<String, Double> d = prices().divine() == null ? Map.of() : prices().divine();
    // 한국어 이름(C380) — 화폐 · 징조 · 에센스 · 뼈 + 고정
    Map<String, String> ko = new java.util.HashMap<>();
    ko.put("Hinekora's Lock", "히네코라의 머리카락");
    ko.put("Divine Orb", "신성한 오브");
    ko.put("Vaal Orb", "바알 오브");
    Poe2CraftService.Options o = craft.options();
    if (o.currencies() != null) {
      o.currencies().forEach(c -> ko.putIfAbsent(c.name(), c.nameKo()));
    }
    if (o.omens() != null) {
      o.omens().forEach(x -> ko.putIfAbsent(x.name(), x.nameKo()));
    }
    if (o.bones() != null) {
      o.bones().forEach(b -> ko.putIfAbsent(b.name(), b.nameKo()));
    }
    craft.essences(req.poolKey()).forEach(e -> ko.putIfAbsent(e.name(), e.nameKo()));
    List<SpentLine> lines = new ArrayList<>();
    double total = 0;
    int missing = 0;
    for (Map.Entry<String, Double> e : m.entrySet()) {
      Double p = d.get(e.getKey());
      Double sub = p == null ? null : p * e.getValue();
      if (sub == null) {
        missing++;
      } else {
        total += sub;
      }
      lines.add(new SpentLine(e.getKey(), ko.get(e.getKey()), e.getValue(), p, sub));
    }
    List<StepCost> steps = new ArrayList<>();
    for (Map<String, Double> sm : perStep) {
      List<String> en = new ArrayList<>();
      List<String> kn = new ArrayList<>();
      double sum = 0;
      boolean all = true;
      for (Map.Entry<String, Double> e : sm.entrySet()) {
        String cnt =
            e.getValue() > 1
                ? " ×"
                    + (e.getValue() % 1 == 0
                        ? String.valueOf(e.getValue().longValue())
                        : String.valueOf(e.getValue()))
                : "";
        en.add(e.getKey() + cnt);
        kn.add((ko.get(e.getKey()) == null ? e.getKey() : ko.get(e.getKey())) + cnt);
        Double pr = d.get(e.getKey());
        if (pr == null) {
          all = false;
        } else {
          sum += pr * e.getValue();
        }
      }
      // 시세가 없는 화폐가 끼면 null(비용 모름), 쓴 화폐가 없으면 0(드러내기 · 직접 편집 · 바꿔 들기)
      steps.add(
          new StepCost(
              en, kn, sm.isEmpty() ? Double.valueOf(0) : all ? Double.valueOf(sum) : null));
    }
    return new SpentResult(lines, total, missing, prices().league(), prices().fetchedAt(), steps);
  }

  /** 한 단계에서 쓴 화폐 이름 → 개수(C453 — 전에 spent 루프 본문, 합계와 단계별이 같은 규칙). */
  Map<String, Double> stepNames(SpentStep s, Names names) {
    Map<String, Double> sm = new LinkedHashMap<>();
    if (s == null || s.action() == null) {
      return sm;
    }
    if ("FORESEE".equals(s.action())) {
      sm.merge("Hinekora's Lock", 1d, Double::sum);
      return sm;
    }
    if ("DIVINE".equals(s.action())) {
      // 신성한 오브(C370) — 게이트가 수치만 다시 굴린다(엔진 화폐 아님)
      sm.merge("Divine Orb", 1d, Double::sum);
      return sm;
    }
    if ("RECOMBINE".equals(s.action())) {
      // 재조합기(C407) — 유물(재조합 비용)은 시세가 없어 빼고 재조합의 징조만
      for (String o : s.omens() == null ? List.<String>of() : s.omens()) {
        sm.merge(names.omen(o), 1d, Double::sum);
      }
      return sm;
    }
    Poe2CraftEngine.Action act;
    try {
      act = Poe2CraftEngine.Action.valueOf(s.action());
    } catch (IllegalArgumentException e) {
      return sm;
    }
    if (act == Poe2CraftEngine.Action.SANCTIFY) {
      // 축성(C390) — 신성한 오브 하나(징조는 아래 omens 로)
      sm.merge("Divine Orb", 1d, Double::sum);
    } else if (act == Poe2CraftEngine.Action.VAAL) {
      // 바알 오브(C376) — 제작 화폐 표에 없다
      sm.merge("Vaal Orb", 1d, Double::sum);
    } else if (act == Poe2CraftEngine.Action.CATALYST) {
      // 기폭제(C386) — arg = 종류[*쓴 개수], 이름 = 화폐 표 CATALYST|종류
      String a = s.arg() == null ? "" : s.arg();
      double n = 1;
      int star = a.indexOf('*');
      if (star > 0) {
        try {
          n = Math.max(1, Integer.parseInt(a.substring(star + 1)));
        } catch (NumberFormatException e) {
          n = 1;
        }
        a = a.substring(0, star);
      }
      String nm = names.currency().get("CATALYST|" + a);
      if (nm != null) {
        sm.merge(nm, n, Double::sum);
      }
    } else if (act != Poe2CraftEngine.Action.MANUAL_ADD
        && act != Poe2CraftEngine.Action.MANUAL_REMOVE) {
      String nm = names.of(act, s.arg());
      if (nm != null) {
        sm.merge(nm, 1d, Double::sum);
      }
    }
    for (String o : s.omens() == null ? List.<String>of() : s.omens()) {
      sm.merge(names.omen(o), 1d, Double::sum);
    }
    return sm;
  }

  private volatile Prices prices;
  private volatile long pricesMtime = -1;

  /** 시세(디바인) — 파일이 바뀌면 다시 읽는다. 없으면 빈 시세. 사용자 가격(C530)이 걸려 있으면 합쳐서. */
  public Prices prices() {
    Prices base = filePrices();
    Map<String, Double> user = OVERRIDE.get();
    if (user == null) {
      return base;
    }
    Map<String, Double> merged =
        new java.util.HashMap<>(base.divine() == null ? Map.of() : base.divine());
    merged.putAll(user);
    return new Prices(base.league(), base.fetchedAt(), base.unit(), merged);
  }

  private Prices filePrices() {
    try {
      long m = java.nio.file.Files.getLastModifiedTime(pricesFile).toMillis();
      if (prices == null || m != pricesMtime) {
        prices =
            tools.jackson.databind.json.JsonMapper.builder()
                .build()
                .readValue(pricesFile.toFile(), Prices.class);
        pricesMtime = m;
      }
    } catch (Exception e) {
      if (prices == null) {
        prices = new Prices(null, null, "Divine Orb", Map.of());
      }
    }
    return prices;
  }

  /**
   * 베이스 아이템 가격 이름(10-23 Q20b — craftofexile 'Base cost'). 시세가 없어 '가격 직접 입력'에만 있다. PoE2 는 정제가 없어
   * 시도마다 새 베이스를 쓰는 방법이 많은데 그 값을 늘 0 으로 봤다.
   */
  public static final String BASE_ITEM = "Base item";

  /** 베이스 값(디바인) — 사용자 가격에 넣었을 때만, 없으면 0. */
  public double basePrice() {
    Map<String, Double> d = prices().divine();
    Double v = d == null ? null : d.get(BASE_ITEM);
    return v == null || !(v > 0) ? 0 : v;
  }

  /** 새 베이스에서 시작한 방법 — 성공 한 번당 베이스 수(시도 수 / 성공)를 쓴 것에 더하고 비용(Q20b). 지금 아이템에서 시작이면 베이스는 이미 있다. */
  private Double costWithBase(
      Map<String, Double> per, Poe2CraftEngine.Item base, int done, int success) {
    if (success > 0 && base.affixes().isEmpty() && basePrice() > 0) {
      per.put(BASE_ITEM, (double) done / success);
    }
    return cost(per);
  }

  private String baseKo() {
    return basePrice() > 0 ? "베이스 값 포함" : "베이스 값은 빼고";
  }

  private String baseEn() {
    return basePrice() > 0 ? "base cost included" : "base cost excluded";
  }

  Double cost(Map<String, Double> per) {
    if (per.isEmpty()) {
      return null;
    }
    Map<String, Double> d = prices().divine() == null ? Map.of() : prices().divine();
    double sum = 0;
    for (Map.Entry<String, Double> e : per.entrySet()) {
      Double p = d.get(e.getKey());
      if (p == null) {
        return null;
      }
      sum += p * e.getValue();
    }
    return sum;
  }

  public Response calc(Request req) {
    if (req.targets() == null || real(req.targets()).isEmpty()) {
      // 빈 자리 조건(C521)만으로는 계산하지 않는다 — 옵션 목표와 함께
      return new Response(List.of(), null, null, null, "목표 옵션을 하나 이상 고르세요");
    }
    for (Target t : req.targets()) {
      if (t == null || t.anyOf() == null || t.anyOf().isEmpty()) {
        return new Response(List.of(), null, null, null, "빈 목표가 있습니다");
      }
    }
    // 출발점(C357) — 옵션이 있는 아이템(지금 아이템에서 시작)이면 그대로: 카오스 반복만(연금 없이), 새 베이스가 필요한 방법은 뺀다
    Poe2CraftEngine.Item given = craft.normalize(req.item());
    boolean fromItem = !given.affixes().isEmpty();
    if (given.sanctified()) {
      // 축성된 아이템(C390) — 더 바꿀 수 없다
      return new Response(
          List.of(), null, null, null, "축성된 아이템은 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)");
    }
    if (given.corrupted()) {
      // 타락한 아이템(C382) — 화폐가 모두 막힌다
      return new Response(
          List.of(),
          null,
          null,
          null,
          "타락한 아이템은 화폐로 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)");
    }
    if (fromItem && given.rarity() != Poe2CraftEngine.Rarity.RARE) {
      // 매직이면 연금이 그 아이템을 지워 버린다(10-16 검토) — 지금 아이템 시작은 레어(카오스 반복)만
      return new Response(List.of(), null, null, null, "지금 아이템에서 시작은 레어 아이템일 때만(카오스 반복) 계산합니다");
    }
    Poe2CraftEngine.Item base =
        fromItem
            ? given
            : new Poe2CraftEngine.Item(
                given.poolKey(), given.ilvl(), Poe2CraftEngine.Rarity.NORMAL, List.of(), false);
    int runs = Math.max(50, Math.min(5_000, req.runs() == null ? DEFAULT_RUNS : req.runs()));
    List<Method> out = new ArrayList<>();
    Integer rc = req.recombChance();
    // 주얼은 재조합기 대상이 아니다(C436 검토 — 엔진은 C434 부터 거절하는데 계산기는 반쪽 재조합 방법을 냈다)
    boolean recomb =
        !fromItem
            && rc != null
            && rc >= 1
            && rc <= 100
            && real(req.targets()).size() >= 2
            // 반쪽 재조합은 두 반쪽 확률을 곱해 합치는 분석식이라 빈 자리 조건(C521)을 따질 수 없다 — 그땐 뺀다
            && real(req.targets()).size() == req.targets().size()
            && !Poe2CraftEngine.jewel(given.poolKey());
    // 반쪽 재조합은 반쪽마다 방법 셋을 더 굴려 몫을 둘 더 준다(게이트 10초 안)
    // 훼손 목표(C420) — 목표에 훼손된 속성이 있으면 훼손 방법(변형 최대 넷)에 몫을 둘 더
    Set<String> deseIds =
        craft.desecratedOf(given.poolKey()).stream()
            .map(d -> d.mod().id())
            .collect(java.util.stream.Collectors.toSet());
    // 한 아이템엔 훼손 하나(이미 훼손된 아이템은 다시 훼손 못 함) — 훼손 목표가 둘 이상이면 늘 0% 라 계산하지 않는다(C426 검토)
    long deseTargets =
        req.targets().stream().filter(t -> t.anyOf().stream().anyMatch(deseIds::contains)).count();
    boolean dese = !fromItem && deseTargets == 1;
    long slice = WALL_MS / (fromItem ? 3 : 8 + (recomb ? 2 : 0) + (dese ? 2 : 0));
    for (String tier : List.of("", "greater", "perfect")) {
      deadline.set(System.currentTimeMillis() + slice);
      out.add(chaosSpam(base, req.targets(), tier, runs));
    }
    if (!fromItem) {
      deadline.set(System.currentTimeMillis() + slice);
      out.add(exaltSlam(base, req.targets(), "", runs * 25));
      deadline.set(System.currentTimeMillis() + slice);
      out.add(exaltSlam(base, req.targets(), "perfect", runs * 25));
      deadline.set(System.currentTimeMillis() + slice);
      out.add(alchemySpam(base, req.targets(), runs * 25));
      deadline.set(System.currentTimeMillis() + slice);
      out.addAll(essenceSlams(base, req.targets(), runs * 10));
      deadline.set(System.currentTimeMillis() + slice);
      Method cat = catalysingSlam(base, req.targets(), runs * 25);
      if (cat != null) {
        out.add(cat);
      }
      if (recomb) {
        out.addAll(recombHalves(base, req.targets(), rc, runs, slice * 2));
      }
      if (dese) {
        out.addAll(desecrateSlams(base, req.targets(), runs * 25, slice * 2));
      }
    }
    out.removeIf(m -> m.runs() == 0);
    out.sort(
        java.util.Comparator.comparing(
                (Method m) -> m.costPerSuccess() == null ? Double.MAX_VALUE : m.costPerSuccess())
            .thenComparing(m -> -m.successRate()));
    Prices p = prices();
    Double ex = p.divine() == null ? null : p.divine().get("Exalted Orb");
    return new Response(
        out, p.league(), p.fetchedAt(), ex == null || ex <= 0 ? null : 1 / ex, null);
  }

  private Method chaosSpam(Poe2CraftEngine.Item base, List<Target> targets, String tier, int runs) {
    Poe2CraftEngine engine = craft.engine();
    SplittableRandom rng = new SplittableRandom(("chaos" + tier).hashCode());
    int success = 0;
    long chaos = 0;
    long ops = 0;
    int done = 0;
    for (int r = 0;
        r < runs && ops < OPS_BUDGET && System.currentTimeMillis() < deadline.get();
        r++, done++) {
      // 지금 아이템(레어)에서 시작이면 연금 없이
      Poe2CraftEngine.Item it =
          base.rarity() == Poe2CraftEngine.Rarity.RARE
              ? base
              : engine.apply(base, Poe2CraftEngine.Action.ALCHEMY, null, Set.of(), rng).item();
      int n = 0;
      while (!meets(it, targets) && n < CHAOS_MAX) {
        Poe2CraftEngine.Result cr =
            engine.apply(it, Poe2CraftEngine.Action.CHAOS, tier, Set.of(), rng);
        if (cr.error() != null) {
          break; // 타락 · 모두 분열 · 붙일 옵션 없음 — 더 굴려도 같다
        }
        it = cr.item();
        n++;
      }
      ops += n + 1;
      chaos += n;
      if (meets(it, targets)) {
        success++;
      }
    }
    String name = tierName("Chaos Orb", tier);
    Map<String, Double> per = new LinkedHashMap<>();
    if (success > 0) {
      if (base.rarity() != Poe2CraftEngine.Rarity.RARE) {
        per.put("Orb of Alchemy", (double) done / success);
      }
      per.put(name, (double) chaos / success);
    }
    return new Method(
        "CHAOS" + (tier.isEmpty() ? "" : ":" + tier),
        "연금 → " + tierKo(tier) + "카오스 반복(최대 " + CHAOS_MAX + ")",
        done,
        success,
        done == 0 ? 0 : (double) success / done,
        per,
        costWithBase(per, base, done, success),
        "회차 " + done + "번 · 카오스는 옵션 하나만 바꿔 상태가 남는다(회차 단위)",
        "Alchemy → " + tierEn(tier) + "Chaos Orb repeated (up to " + CHAOS_MAX + ")",
        done + " runs · Chaos changes one modifier at a time, so the item carries over (per run)");
  }

  /**
   * 더 엑잘해서 못 채운 목표를 채울 수 있나(C363 · C369 검토 보강) — 목표마다 붙을 수 있는 후보(빈 쪽 · 아이템 레벨 · 화폐 등급 최소 레벨 · 이미 있는
   * 옵션과 같은 그룹 아님)가 있어야 하고, 한쪽 후보만 있는 목표 수가 그쪽 빈 자리 수를 넘으면 안 된다. 하나라도 막혔으면 접는다. 목표마다 후보는 있는데 서로 다른
   * 옵션으로 못 나눈 경우(meets 거짓)는 옛 동작대로 계속.
   */
  private final Map<String, Set<String>> eligibleAtMinCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  private Set<String> eligibleAtMin(Poe2CraftEngine.Item item, int minLevel) {
    return eligibleAtMinCache.computeIfAbsent(
        item.poolKey() + "|" + item.ilvl() + "|" + minLevel,
        k ->
            craft
                .engine()
                .eligible(
                    new Poe2CraftEngine.Item(
                        item.poolKey(), item.ilvl(), Poe2CraftEngine.Rarity.RARE, List.of(), false),
                    Poe2CraftEngine.Rarity.RARE,
                    minLevel,
                    null,
                    false)
                .stream()
                .map(Poe2CraftEngine.Mod::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
  }

  private boolean canStillMeet(Poe2CraftEngine.Item item, List<Target> targets, int minLevel) {
    Set<String> have =
        item.affixes().stream()
            .map(Poe2CraftEngine.Affix::id)
            .collect(java.util.stream.Collectors.toSet());
    Set<Integer> groups = new java.util.HashSet<>();
    item.affixes().stream().filter(a -> a.groups() != null).forEach(a -> groups.addAll(a.groups()));
    int max = Poe2CraftEngine.maxPerGen(item, Poe2CraftEngine.Rarity.RARE);
    int openPre = max - (int) item.affixes().stream().filter(x -> "prefix".equals(x.gen())).count();
    int openSuf = max - (int) item.affixes().stream().filter(x -> "suffix".equals(x.gen())).count();
    // 빈 자리 조건(C521) — 그 쪽 자리를 비워 둬야 하니 처음부터 요구에 더한다
    int[] openNeed = openNeed(targets);
    int needPre = openNeed[0];
    int needSuf = openNeed[1];
    int unmet = openNeed[0] + openNeed[1];
    // 최소 속성 레벨이 있으면 엔진과 같은 판정(유형 전부가 아래면 최고 등급은 붙는다 — C418 검토: 예외를 몰라 완벽 엑잘을 일찍 접었다).
    // 예외는 아이템 상태와 무관해(같은 유형 = 같은 그룹 · 쪽) 빈 레어 기준으로 풀 · 아이템 레벨 · 최소 레벨마다 한 번만
    Set<String> elig = minLevel > 0 ? eligibleAtMin(item, minLevel) : null;
    for (Target t : real(targets)) {
      if (t.anyOf().stream().anyMatch(have::contains)) {
        continue;
      }
      unmet++;
      boolean pre = false;
      boolean suf = false;
      for (String id : t.anyOf()) {
        Poe2CraftEngine.Mod m = craft.modOf(id);
        if (m == null
            || m.level() > item.ilvl()
            || (elig != null && !elig.contains(id))
            || (m.groups() != null && m.groups().stream().anyMatch(groups::contains))) {
          continue;
        }
        if ("prefix".equals(m.gen()) && openPre > 0) {
          pre = true;
        } else if ("suffix".equals(m.gen()) && openSuf > 0) {
          suf = true;
        }
      }
      if (!pre && !suf) {
        return false;
      }
      if (pre && !suf) {
        needPre++;
      } else if (suf && !pre) {
        needSuf++;
      }
    }
    return needPre <= openPre && needSuf <= openSuf && unmet <= openPre + openSuf;
  }

  private Method exaltSlam(
      Poe2CraftEngine.Item base, List<Target> targets, String tier, int tries) {
    return exaltSlam(base, targets, tier, tries, null);
  }

  /** sideOmen = 엑잘마다 걸 좌측/우측 찬미 징조 키(반쪽 재조합 C410 — 반쪽 목표가 접두뿐 · 접미뿐일 때), 없으면 null. */
  private Method exaltSlam(
      Poe2CraftEngine.Item base, List<Target> targets, String tier, int tries, String sideOmen) {
    Poe2CraftEngine engine = craft.engine();
    SplittableRandom rng =
        new SplittableRandom(("exalt" + tier + (sideOmen == null ? "" : sideOmen)).hashCode());
    Set<String> exOmens = sideOmen == null ? Set.of() : Set.of(sideOmen);
    int success = 0;
    long aug = 0;
    long ex = 0;
    int done = 0;
    for (int i = 0;
        i < tries && ((i & 255) != 0 || System.currentTimeMillis() < deadline.get());
        i++, done++) {
      Poe2CraftEngine.Item it =
          engine.apply(base, Poe2CraftEngine.Action.TRANSMUTE, tier, Set.of(), rng).item();
      Poe2CraftEngine.Result a =
          engine.apply(it, Poe2CraftEngine.Action.AUGMENTATION, tier, Set.of(), rng);
      if (a.error() == null) {
        it = a.item();
        aug++;
      }
      it = engine.apply(it, Poe2CraftEngine.Action.REGAL, tier, Set.of(), rng).item();
      // 목표를 채우면 멈추고, 못 채운 목표 쪽 자리가 다 차면 접는다(10-16 C363 — 6옵션까지 무조건 엑잘해 비용을 부풀렸다. 성공률은 같다)
      while (it.affixes().size() < 6
          && !meets(it, targets)
          && canStillMeet(
              it, targets, Poe2CraftEngine.minLevel(Poe2CraftEngine.Action.EXALT, tier))) {
        Poe2CraftEngine.Result e =
            engine.apply(it, Poe2CraftEngine.Action.EXALT, tier, exOmens, rng);
        if (e.error() != null) {
          break;
        }
        it = e.item();
        ex++;
      }
      if (meets(it, targets)) {
        success++;
      }
    }
    Map<String, Double> per = new LinkedHashMap<>();
    if (success > 0) {
      per.put(tierName("Orb of Transmutation", tier), (double) done / success);
      per.put(tierName("Orb of Augmentation", tier), (double) aug / success);
      per.put(tierName("Regal Orb", tier), (double) done / success);
      per.put(tierName("Exalted Orb", tier), (double) ex / success);
      if (sideOmen != null) {
        per.put(names(base.poolKey()).omen(sideOmen), (double) ex / success);
      }
    }
    String sideKo =
        sideOmen == null ? "" : sideOmen.startsWith("Sinistral") ? "(좌측 찬미)" : "(우측 찬미)";
    String sideEn =
        sideOmen == null
            ? ""
            : sideOmen.startsWith("Sinistral")
                ? " (Sinistral Exaltation)"
                : " (Dextral Exaltation)";
    return new Method(
        "EXALT" + (tier.isEmpty() ? "" : ":" + tier) + (sideOmen == null ? "" : ":" + sideOmen),
        tierKo(tier) + "진화 → 확장 → 제왕 → 엑잘" + sideKo + "(목표까지, 최대 6옵션)",
        done,
        success,
        (double) success / Math.max(1, done),
        per,
        costWithBase(per, base, done, success),
        "시도마다 새 베이스(" + baseKo() + ") · 목표를 채우거나 목표 쪽 자리가 막히면 멈춤",
        tierEn(tier)
            + "Transmute → Augment → Regal → Exalt"
            + sideEn
            + " (until the targets, up to 6 modifiers)",
        "New base each try ("
            + baseEn()
            + ") · stops when the targets are met or the target side is full");
  }

  /**
   * 기폭제 + 촉진하는 찬미 엑잘(C386) — 장신구(보통 기폭제) · 주얼(제련된): 진화 → 확장 → 제왕 → (기폭제 20% → 촉진하는 찬미 엑잘)을 목표까지(최대
   * 6옵션). 기폭제 종류 = 목표 옵션 태그에 맞는 것마다 따로 굴려 성공 한 번당 비용이 가장 낮은(시세 없으면 성공률 높은) 하나. 맞는 종류가 없으면 null.
   */
  private Method catalysingSlam(Poe2CraftEngine.Item base, List<Target> targets, int tries) {
    boolean jewel = base.poolKey() != null && base.poolKey().startsWith("Jewel|");
    List<String> keys = new ArrayList<>();
    for (String k : new java.util.TreeSet<>(Poe2CraftEngine.CATALYST_TAGS.keySet())) {
      String key = jewel ? "jewel:" + k : k;
      if (!Poe2CraftEngine.catalystFits(base.poolKey(), key)) {
        continue;
      }
      List<String> want = Poe2CraftEngine.CATALYST_TAGS.get(k);
      boolean hit =
          targets.stream()
              .flatMap(t -> t.anyOf().stream())
              .map(craft::modOf)
              .anyMatch(
                  m -> m != null && m.tags() != null && m.tags().stream().anyMatch(want::contains));
      if (hit) {
        keys.add(key);
      }
    }
    if (keys.isEmpty()) {
      return null;
    }
    Map<String, String> catName = new java.util.HashMap<>();
    for (Poe2CraftService.Currency c : craft.options().currencies()) {
      if ("CATALYST".equals(c.action())) {
        catName.put(c.tier(), c.name());
      }
    }
    String omenName = names(base.poolKey()).omen("CatalysingExaltation");
    Poe2CraftEngine engine = craft.engine();
    long perKey = Math.max(1, tries / keys.size());
    Method best = null;
    for (String key : keys) {
      SplittableRandom rng = new SplittableRandom(("catalysing" + key).hashCode());
      int success = 0;
      long aug = 0;
      long ex = 0;
      int done = 0;
      for (int i = 0;
          i < perKey && ((i & 255) != 0 || System.currentTimeMillis() < deadline.get());
          i++, done++) {
        Poe2CraftEngine.Item it =
            engine.apply(base, Poe2CraftEngine.Action.TRANSMUTE, "", Set.of(), rng).item();
        Poe2CraftEngine.Result a =
            engine.apply(it, Poe2CraftEngine.Action.AUGMENTATION, "", Set.of(), rng);
        if (a.error() == null) {
          it = a.item();
          aug++;
        }
        it = engine.apply(it, Poe2CraftEngine.Action.REGAL, "", Set.of(), rng).item();
        while (it.affixes().size() < 6
            && !meets(it, targets)
            && canStillMeet(
                it, targets, Poe2CraftEngine.minLevel(Poe2CraftEngine.Action.EXALT, ""))) {
          Poe2CraftEngine.Result e =
              engine.apply(
                  it.withQuality(key + ":" + Poe2CraftEngine.MAX_QUALITY),
                  Poe2CraftEngine.Action.EXALT,
                  "",
                  Set.of("CatalysingExaltation"),
                  rng);
          if (e.error() != null) {
            break;
          }
          it = e.item();
          ex++;
        }
        if (meets(it, targets)) {
          success++;
        }
      }
      if (done == 0) {
        continue;
      }
      Map<String, Double> per = new LinkedHashMap<>();
      if (success > 0) {
        per.put("Orb of Transmutation", (double) done / success);
        per.put("Orb of Augmentation", (double) aug / success);
        per.put("Regal Orb", (double) done / success);
        per.put("Exalted Orb", (double) ex / success);
        per.put(omenName, (double) ex / success);
        per.put(
            catName.getOrDefault(key, key), (double) ex * Poe2CraftEngine.MAX_QUALITY / success);
      }
      Method m =
          new Method(
              "CATALYSING:" + key,
              "진화 → 확장 → 제왕 → (기폭제 20% + 촉진하는 찬미) 엑잘 — " + catName.getOrDefault(key, key),
              done,
              success,
              (double) success / done,
              per,
              costWithBase(per, base, done, success),
              "엑잘마다 기폭제 20개 · 징조 하나(퀄리티를 모두 소모) · 같은 종류 가중치 ×5(poe2wiki, 균등 가정 위에)",
              "Transmute → Augment → Regal → (20% catalyst + Omen of Catalysing Exaltation) Exalt — "
                  + catName.getOrDefault(key, key),
              "20 catalysts per Exalt · one omen (consumes all quality) · same-type weighting ×5"
                  + " (poe2wiki, on top of the uniform assumption)");
      if (best == null
          || (m.costPerSuccess() != null
              && (best.costPerSuccess() == null || m.costPerSuccess() < best.costPerSuccess()))
          || (m.costPerSuccess() == null
              && best.costPerSuccess() == null
              && m.successRate() > best.successRate())) {
        best = m;
      }
    }
    return best;
  }

  /**
   * 에센스 쌓기(C353) — 목표 옵션을 보장하는 보통 에센스마다: 진화 → (확장) → 에센스(매직 → 레어 + 보장) → 엑잘을 6옵션까지(목표를 채우거나 막히면 멈춤,
   * C363). 시도마다 새 베이스. 가장 나은 둘.
   */
  private List<Method> essenceSlams(Poe2CraftEngine.Item base, List<Target> targets, int tries) {
    Poe2CraftEngine engine = craft.engine();
    java.util.Set<String> wanted = new java.util.HashSet<>();
    for (Target t : targets) {
      wanted.addAll(t.anyOf());
    }
    List<Method> all = new ArrayList<>();
    for (Poe2CraftService.EssenceOption e : craft.essences(base.poolKey())) {
      if (!"normal".equals(e.kind()) || !wanted.contains(e.modId())) {
        continue;
      }
      SplittableRandom rng = new SplittableRandom(e.slug().hashCode());
      int success = 0;
      int done = 0;
      long essUsed = 0;
      long aug = 0;
      long ex = 0;
      for (int i = 0;
          i < tries && ((i & 127) != 0 || System.currentTimeMillis() < deadline.get());
          i++, done++) {
        Poe2CraftEngine.Item it =
            engine.apply(base, Poe2CraftEngine.Action.TRANSMUTE, null, Set.of(), rng).item();
        Poe2CraftEngine.Result a =
            engine.apply(it, Poe2CraftEngine.Action.AUGMENTATION, null, Set.of(), rng);
        if (a.error() == null) {
          it = a.item();
          aug++;
        }
        Poe2CraftEngine.Result es =
            engine.apply(it, Poe2CraftEngine.Action.ESSENCE, e.slug(), Set.of(), rng);
        if (es.error() != null) {
          continue;
        }
        it = es.item();
        essUsed++; // 실패한 에센스는 안 쓰인다(10-16 검토 — done/success 로 세면 과대)
        while (it.affixes().size() < 6 && !meets(it, targets) && canStillMeet(it, targets, 0)) {
          Poe2CraftEngine.Result x =
              engine.apply(it, Poe2CraftEngine.Action.EXALT, null, Set.of(), rng);
          if (x.error() != null) {
            break;
          }
          it = x.item();
          ex++;
        }
        if (meets(it, targets)) {
          success++;
        }
      }
      Map<String, Double> per = new LinkedHashMap<>();
      if (success > 0) {
        per.put("Orb of Transmutation", (double) done / success);
        per.put("Orb of Augmentation", (double) aug / success);
        per.put(e.name(), (double) essUsed / success);
        per.put("Exalted Orb", (double) ex / success);
      }
      all.add(
          new Method(
              "ESSENCE:" + e.slug(),
              e.nameKo() + " — 진화 → 확장 → 에센스 → 엑잘(목표까지, 최대 6옵션)",
              done,
              success,
              (double) success / Math.max(1, done),
              per,
              costWithBase(per, base, done, success),
              "보장: " + String.join(", ", e.ko() == null || e.ko().isEmpty() ? e.en() : e.ko()),
              e.name()
                  + " — Transmute → Augment → Essence → Exalt (until the targets, up to 6 modifiers)",
              "Guaranteed: " + String.join(", ", e.en() == null ? List.of() : e.en())));
    }
    all.sort(
        java.util.Comparator.comparing(
            (Method m) -> m.costPerSuccess() == null ? Double.MAX_VALUE : m.costPerSuccess()));
    return all.subList(0, Math.min(2, all.size()));
  }

  private Method alchemySpam(Poe2CraftEngine.Item base, List<Target> targets, int tries) {
    Poe2CraftEngine engine = craft.engine();
    SplittableRandom rng = new SplittableRandom(7);
    int success = 0;
    int done = 0;
    for (int i = 0;
        i < tries && ((i & 255) != 0 || System.currentTimeMillis() < deadline.get());
        i++, done++) {
      if (meets(
          engine.apply(base, Poe2CraftEngine.Action.ALCHEMY, null, Set.of(), rng).item(),
          targets)) {
        success++;
      }
    }
    Map<String, Double> per = new LinkedHashMap<>();
    if (success > 0) {
      per.put("Orb of Alchemy", (double) done / success);
    }
    return new Method(
        "ALCHEMY",
        "연금 연타(새 베이스마다)",
        done,
        success,
        (double) success / Math.max(1, done),
        per,
        costWithBase(per, base, done, success),
        "4옵션 · " + baseKo(),
        "Orb of Alchemy spam (new base each time)",
        "4 modifiers · " + baseEn());
  }

  /**
   * 반쪽 재조합(10-17 C409) — 목표를 접두 · 접미로(한쪽뿐이면 앞뒤 반으로) 나눠 두 아이템을 따로 만들고, 재조합기로 목표 옵션만 골라 합친다. 반쪽마다 카오스
   * · 엑잘 · 연금 중 가장 싼 방법을 반쪽 목표로 새로 굴려, 비용 = (반쪽 A + 반쪽 B) / 재조합 성공 확률. 확률은 공식 비공개라 인게임 화면 값을 받는다(고른
   * 옵션마다 다르다). 재조합의 징조 시세가 있으면 행운(1 − (1 − p)²) 변형도 — 징조는 시도마다 하나.
   */
  private List<Method> recombHalves(
      Poe2CraftEngine.Item base, List<Target> targets, int chance, int runs, long budget) {
    List<Target> a = new ArrayList<>();
    List<Target> b = new ArrayList<>();
    for (Target t : targets) {
      Poe2CraftEngine.Mod m = craft.modOf(t.anyOf().get(0));
      (m != null && "suffix".equals(m.gen()) ? b : a).add(t);
    }
    String split = "접두 · 접미";
    if (a.isEmpty() || b.isEmpty()) {
      List<Target> all = new ArrayList<>(targets);
      a = new ArrayList<>(all.subList(0, (all.size() + 1) / 2));
      b = new ArrayList<>(all.subList((all.size() + 1) / 2, all.size()));
      split = "앞뒤 반";
    }
    boolean byGen = "접두 · 접미".equals(split);
    // 반쪽 = 접두 · 접미 나눔은 목표 첫 옵션의 접두/접미로(게이트 목표는 계열 단위라 섞이지 않는다)
    Method ma = cheapestHalf(base, a, runs, budget / 2, byGen ? "SinistralExaltation" : null);
    Method mb = cheapestHalf(base, b, runs, budget / 2, byGen ? "DextralExaltation" : null);
    double p = chance / 100.0;
    List<Method> out = new ArrayList<>();
    out.add(combineHalves(ma, mb, p, false, chance, split, 0));
    Map<String, Double> d = prices().divine() == null ? Map.of() : prices().divine();
    if (d.containsKey(RECOMB_OMEN)) {
      out.add(combineHalves(ma, mb, p, true, chance, split, d.get(RECOMB_OMEN)));
    }
    return out;
  }

  static final String RECOMB_OMEN = "Omen of Recombination";

  /**
   * 훼손 방법(C420) — 새 베이스마다 연금 → 보존된 뼈로 훼손 → 드러낼 셋에서 목표 먼저. 변형: 심연의 메아리(셋에 목표가 없을 때만 다시 보기) · 군주
   * 징조(훼손 목표가 한 군주뿐이고 무기 · 장신구 뼈일 때 — 셋을 그 군주 옵션으로) · 둘 다.
   */
  private List<Method> desecrateSlams(
      Poe2CraftEngine.Item base, List<Target> targets, int tries, long budget) {
    String cls = base.poolKey() == null ? "" : base.poolKey().split("[|]")[0];
    Poe2CraftService.Options o = craft.options();
    Poe2CraftService.BoneInfo bone =
        (o.bones() == null ? List.<Poe2CraftService.BoneInfo>of() : o.bones())
            .stream()
                .filter(
                    b ->
                        b.classes() != null
                            && b.classes().contains(cls)
                            && b.maxIlvl() == 0
                            && b.minModLevel() == 0
                            && !b.breach())
                .findFirst()
                .orElse(null);
    if (bone == null) {
      return List.of();
    }
    Map<String, String> lichOf = new java.util.HashMap<>();
    craft.desecratedOf(base.poolKey()).forEach(d -> lichOf.put(d.mod().id(), d.lich()));
    Set<String> liches = new java.util.HashSet<>();
    for (Target t : targets) {
      for (String id : t.anyOf()) {
        if (lichOf.containsKey(id) && lichOf.get(id) != null) {
          liches.add(lichOf.get(id));
        }
      }
    }
    String lichOmen =
        liches.size() != 1 || !(bone.slug().contains("Weapon") || bone.slug().contains("Jewellery"))
            ? null
            : switch (liches.iterator().next()) {
              case "ulaman" -> "Sovereign";
              case "amanamu" -> "Liege";
              case "kurgal" -> "Blackblooded";
              default -> null;
            };
    List<List<String>> variants = new ArrayList<>();
    variants.add(List.of());
    variants.add(List.of("AbyssalEchoes"));
    if (lichOmen != null) {
      variants.add(List.of(lichOmen));
      variants.add(List.of(lichOmen, "AbyssalEchoes"));
    }
    Names names = names(base.poolKey());
    Map<String, String> omenKo = new java.util.HashMap<>();
    (o.omens() == null ? List.<Poe2CraftService.Omen>of() : o.omens())
        .forEach(x -> omenKo.put(x.key(), x.nameKo()));
    List<Method> out = new ArrayList<>();
    for (List<String> v : variants) {
      deadline.set(System.currentTimeMillis() + budget / variants.size());
      out.add(desecrateSlam(base, targets, tries, bone, v, names, omenKo));
    }
    return out;
  }

  private Method desecrateSlam(
      Poe2CraftEngine.Item base,
      List<Target> targets,
      int tries,
      Poe2CraftService.BoneInfo bone,
      List<String> omens,
      Names names,
      Map<String, String> omenKo) {
    Poe2CraftEngine engine = craft.engine();
    SplittableRandom rng = new SplittableRandom(("dese" + omens).hashCode());
    boolean echoes = omens.contains("AbyssalEchoes");
    Set<String> desOmens = new java.util.HashSet<>(omens);
    desOmens.remove("AbyssalEchoes");
    Map<String, Double> used = new LinkedHashMap<>();
    int success = 0;
    int done = 0;
    for (int i = 0;
        i < tries && ((i & 255) != 0 || System.currentTimeMillis() < deadline.get());
        i++, done++) {
      Poe2CraftEngine.Item it =
          engine.apply(base, Poe2CraftEngine.Action.ALCHEMY, null, Set.of(), rng).item();
      Poe2CraftEngine.Result d =
          engine.apply(it, Poe2CraftEngine.Action.DESECRATE, bone.slug(), desOmens, rng);
      if (d.error() != null) {
        continue;
      }
      d.omensUsed().forEach(k -> used.merge(k, 1d, Double::sum));
      it = d.item();
      if (echoes && !offersHitTarget(engine, it, targets)) {
        Poe2CraftEngine.Result rr =
            engine.apply(
                it, Poe2CraftEngine.Action.REVEAL_REROLL, null, Set.of("AbyssalEchoes"), rng);
        if (rr.error() == null) {
          it = rr.item();
          used.merge("AbyssalEchoes", 1d, Double::sum);
        }
      }
      String arg = revealChoice(engine, it, targets);
      if (arg == null) {
        continue;
      }
      it = engine.apply(it, Poe2CraftEngine.Action.REVEAL, arg, Set.of(), rng).item();
      if (meets(it, targets)) {
        success++;
      }
    }
    Map<String, Double> per = new LinkedHashMap<>();
    if (success > 0) {
      per.put("Orb of Alchemy", (double) done / success);
      per.put(bone.name(), (double) done / success);
      final int s = success;
      used.forEach((k, n) -> per.put(names.omen(k), n / s));
    }
    String suffix =
        omens.isEmpty()
            ? ""
            : " + "
                + String.join(" + ", omens.stream().map(k -> omenKo.getOrDefault(k, k)).toList());
    String suffixEn =
        omens.isEmpty() ? "" : " + " + String.join(" + ", omens.stream().map(names::omen).toList());
    return new Method(
        "DESECRATE" + (omens.isEmpty() ? "" : ":" + String.join(",", omens)),
        "연금 → 훼손(" + bone.nameKo() + ") → 드러내기(목표 먼저)" + suffix,
        done,
        success,
        (double) success / Math.max(1, done),
        per,
        costWithBase(per, base, done, success),
        "새 베이스마다 · 셋에 목표가 있으면 그것 · 심연의 메아리는 셋에 목표가 없을 때만 씀 · " + baseKo(),
        "Alchemy → Desecrate (" + bone.name() + ") → Reveal (targets first)" + suffixEn,
        "New base each time · takes a target if one of the three offered is a target · Omen of"
            + " Abyssal Echoes only when none is · "
            + baseEn());
  }

  private String genOf(Target t) {
    if (isOpen(t)) {
      return OPEN_SUFFIX.equals(t.anyOf().get(0)) ? "suffix" : "prefix";
    }
    Poe2CraftEngine.Mod m = craft.modOf(t.anyOf().get(0));
    return m == null ? "prefix" : m.gen();
  }

  /** 매직 반쪽(C409 검토) — 새 베이스마다 진화(목표 못 채우면 확장), 최대 2옵션. */
  private Method magicSpam(Poe2CraftEngine.Item base, List<Target> targets, int tries) {
    Poe2CraftEngine engine = craft.engine();
    SplittableRandom rng = new SplittableRandom("magic".hashCode());
    int success = 0;
    long aug = 0;
    int done = 0;
    for (int i = 0;
        i < tries && ((i & 255) != 0 || System.currentTimeMillis() < deadline.get());
        i++, done++) {
      Poe2CraftEngine.Item it =
          engine.apply(base, Poe2CraftEngine.Action.TRANSMUTE, null, Set.of(), rng).item();
      if (!meets(it, targets)) {
        Poe2CraftEngine.Result a =
            engine.apply(it, Poe2CraftEngine.Action.AUGMENTATION, null, Set.of(), rng);
        if (a.error() == null) {
          it = a.item();
          aug++;
        }
      }
      if (meets(it, targets)) {
        success++;
      }
    }
    Map<String, Double> per = new LinkedHashMap<>();
    if (success > 0) {
      per.put("Orb of Transmutation", (double) done / success);
      per.put("Orb of Augmentation", (double) aug / success);
    }
    return new Method(
        "MAGIC",
        "진화 → 확장(매직, 새 베이스마다)",
        done,
        success,
        (double) success / Math.max(1, done),
        per,
        costWithBase(per, base, done, success),
        "매직 2옵션까지 · " + baseKo(),
        "Transmute → Augment (magic, new base each time)",
        "Up to 2 modifiers on a magic item · " + baseEn());
  }

  /**
   * 반쪽 목표를 채우는 가장 싼 방법(카오스 기본 · 엑잘 기본 · 연금, 반쪽이 접두뿐 · 접미뿐이면 그쪽 찬미 징조 엑잘도 — C410) — 시세 없는 것은 성공률로만
   * 비교.
   */
  private Method cheapestHalf(
      Poe2CraftEngine.Item base, List<Target> half, int runs, long budget, String sideOmen) {
    List<Method> cand = new ArrayList<>();
    long each = budget / (sideOmen == null ? 3 : 4);
    deadline.set(System.currentTimeMillis() + each);
    cand.add(chaosSpam(base, half, "", runs));
    deadline.set(System.currentTimeMillis() + each);
    cand.add(exaltSlam(base, half, "", runs * 25));
    deadline.set(System.currentTimeMillis() + each);
    cand.add(alchemySpam(base, half, runs * 25));
    if (sideOmen != null) {
      deadline.set(System.currentTimeMillis() + each);
      cand.add(exaltSlam(base, half, "", runs * 25, sideOmen));
    }
    // 매직 반쪽(C409 검토) — 재조합은 매직도 받는다: 접두 · 접미 목표가 각각 하나 이하면 진화 → 확장
    long pre = half.stream().filter(t -> !"suffix".equals(genOf(t))).count();
    if (pre <= 1 && half.size() - pre <= 1) {
      deadline.set(System.currentTimeMillis() + each);
      cand.add(magicSpam(base, half, runs * 25));
    }
    return cand.stream()
        .filter(m -> m.successes() > 0)
        .min(
            java.util.Comparator.comparing(
                    (Method m) ->
                        m.costPerSuccess() == null ? Double.MAX_VALUE : m.costPerSuccess())
                .thenComparing(m -> -m.successRate()))
        .orElse(cand.get(0));
  }

  /** 반쪽 하나를 채우는 가장 싼 방법(최적 조합 재조합 수단 C472) — 시간 budget 안. */
  Method half(Poe2CraftEngine.Item base, List<Target> half, String sideOmen, long budget) {
    return cheapestHalf(base, half, 400, budget, sideOmen);
  }

  /** 두 반쪽 + 재조합 — 성공 한 번당 개수 = (A + B) / 성공 확률(행운이면 1 − (1 − p)², 징조 하나 / 시도). */
  static Method combineHalves(
      Method a, Method b, double p, boolean lucky, int chance, String split, double omenPrice) {
    double eff = lucky ? 1 - (1 - p) * (1 - p) : p;
    boolean ok = a.successes() > 0 && b.successes() > 0;
    Map<String, Double> per = new LinkedHashMap<>();
    if (ok) {
      a.perSuccess().forEach((k, v) -> per.merge(k, v / eff, Double::sum));
      b.perSuccess().forEach((k, v) -> per.merge(k, v / eff, Double::sum));
      if (lucky) {
        per.merge(RECOMB_OMEN, 1 / eff, Double::sum);
      }
    }
    Double cost =
        !ok || a.costPerSuccess() == null || b.costPerSuccess() == null
            ? null
            : (a.costPerSuccess() + b.costPerSuccess()) / eff + (lucky ? omenPrice / eff : 0);
    return new Method(
        lucky ? "RECOMB_HALVES_LUCKY" : "RECOMB_HALVES",
        "반쪽 재조합(" + split + ", 재조합 " + chance + "%" + (lucky ? " + 재조합의 징조" : "") + ")",
        a.runs() + b.runs(),
        Math.min(a.successes(), b.successes()),
        // 반쪽을 한 번도 못 만들면 성공률도 0(C409 검토 — 입력값이 보여 될 법한 방법처럼 보였다)
        ok ? eff : 0,
        per,
        cost,
        "반쪽 1: "
            + a.label()
            + " · 반쪽 2: "
            + b.label()
            + " · 성공률 칸 = 재조합 성공 확률(입력값"
            + (lucky ? ", 행운" : "")
            + "), 두 아이템은 실패해도 소모 · 유물(재조합 비용)은 시세 없어 뺌",
        "Half recombination ("
            + ("접두 · 접미".equals(split) ? "prefix · suffix" : "split in half")
            + ", recombine "
            + chance
            + "%"
            + (lucky ? " + Omen of Recombination" : "")
            + ")",
        "Half 1: "
            + (a.labelEn() == null ? a.label() : a.labelEn())
            + " · Half 2: "
            + (b.labelEn() == null ? b.label() : b.labelEn())
            + " · the success column = recombination chance (your input"
            + (lucky ? ", lucky" : "")
            + "), both items are consumed even on failure · the relic (recombination cost) has no"
            + " price and is excluded");
  }

  private static String tierName(String name, String tier) {
    return "greater".equals(tier)
        ? "Greater " + name
        : "perfect".equals(tier) ? "Perfect " + name : name;
  }

  /** tierKo 의 영어 판(C561). */
  private static String tierEn(String tier) {
    return "greater".equals(tier) ? "Greater " : "perfect".equals(tier) ? "Perfect " : "";
  }

  private static String tierKo(String tier) {
    return "greater".equals(tier) ? "상위 " : "perfect".equals(tier) ? "완벽한 " : "";
  }

  // ─────────────────────────── 다단계 전략(C355) ───────────────────────────

  public StrategyResult strategy(StrategyRequest req) {
    if (req.targets() == null || real(req.targets()).isEmpty()) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "목표 옵션을 하나 이상 고르세요");
    }
    if (req.steps() == null || req.steps().isEmpty()) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "단계를 하나 이상 넣으세요");
    }
    String bad = validate(req.steps());
    if (bad != null) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "모르는 화폐: " + bad);
    }
    Poe2CraftEngine.Item base = craft.normalize(req.item());
    int runs = Math.max(20, Math.min(3_000, req.runs() == null ? 300 : req.runs()));
    deadline.set(System.currentTimeMillis() + WALL_MS);
    Names names = names(base.poolKey());
    Map<String, Double> total = new LinkedHashMap<>();
    double[] uses = new double[req.steps().size()];
    int[] reached = new int[req.steps().size()];
    int success = 0;
    int done = 0;
    List<Poe2CraftEngine.Item> hits = new ArrayList<>();
    SplittableRandom rng = new SplittableRandom(42);
    for (int r = 0; r < runs && System.currentTimeMillis() < deadline.get(); r++, done++) {
      Poe2CraftEngine.Item it = base;
      boolean aborted = false;
      // 마지막으로 바뀐 아이템 — 중단돼도 그때까지 목표를 채웠으면 성공으로 센다(10-16 검토)
      Poe2CraftEngine.Item[] last = {base};
      // 목표를 다 채우면 그 회차는 끝(10-23 Q18, PoE1 짝 — 남은 '한 번' 단계를 쓰지 않는다)
      for (int k = 0; k < req.steps().size() && !aborted && !meets(it, req.targets()); k++) {
        reached[k]++;
        Step st = req.steps().get(k);
        int[] used = {0};
        Poe2CraftEngine.Item next = runStep(it, st, req.targets(), names, total, rng, used, last);
        uses[k] += used[0];
        if (next == null) {
          aborted = true;
          it = last[0];
        } else {
          it = next;
        }
      }
      if (meets(it, req.targets())) {
        success++;
        if (hits.size() < STRATEGY_SAMPLES) {
          hits.add(it);
        }
      }
    }
    Map<String, Double> per = new LinkedHashMap<>();
    for (Map.Entry<String, Double> e : total.entrySet()) {
      per.put(e.getKey(), e.getValue() / Math.max(1, done));
    }
    // 새 베이스에서 시작하면 회차마다 베이스 하나(Q20b)
    if (done > 0 && base.affixes().isEmpty() && basePrice() > 0) {
      per.put(BASE_ITEM, 1.0);
    }
    Double cpr = cost(per);
    double rate = done == 0 ? 0 : (double) success / done;
    List<StepStat> stats = new ArrayList<>();
    for (int k = 0; k < req.steps().size(); k++) {
      stats.add(
          new StepStat(
              req.steps().get(k).action(),
              req.steps().get(k).arg(),
              reached[k] == 0 ? 0 : uses[k] / reached[k],
              done == 0 ? 0 : (double) reached[k] / done));
    }
    return new StrategyResult(
        done,
        success,
        rate,
        per,
        cpr,
        cpr == null || success == 0 ? null : cpr / rate,
        stats,
        null,
        hits.stream().map(h -> new StrategySample(h, craft.view(h).lines())).toList());
  }

  private static String validate(List<Step> steps) {
    for (Step st : steps) {
      if (st == null) {
        return "(빈 단계)";
      }
      if ("SEQ".equals(st.action())) {
        if (st.sub() == null || st.sub().isEmpty()) {
          return "SEQ(하위 단계 없음)";
        }
        String b = validate(st.sub());
        if (b != null) {
          return b;
        }
        continue;
      }
      try {
        Poe2CraftEngine.Action.valueOf(st.action());
      } catch (RuntimeException e) {
        return st.action();
      }
    }
    return null;
  }

  /** 단계 하나 — 목표(until)를 채울 때까지 max 번(없으면 한 번). 실패(화폐 못 씀)면 null(회차 중단). used[0] = 반복 수. */
  private Poe2CraftEngine.Item runStep(
      Poe2CraftEngine.Item it,
      Step st,
      List<Target> targets,
      Names names,
      Map<String, Double> total,
      SplittableRandom rng,
      int[] used,
      Poe2CraftEngine.Item[] last) {
    List<Target> goal = null;
    if (st.until() != null && !st.until().isEmpty()) {
      goal = new ArrayList<>();
      for (Integer i : st.until()) {
        if (i != null && i >= 0 && i < targets.size()) {
          goal.add(targets.get(i));
        }
      }
    }
    int max =
        goal == null ? 1 : Math.max(1, Math.min(CHAOS_MAX, st.max() == null ? 300 : st.max()));
    Poe2CraftEngine engine = craft.engine();
    for (int n = 0; n < max; n++) {
      if (goal != null && meets(it, goal)) {
        break;
      }
      // 묶음 안 반복까지 합치면 한 회차가 수십만 번이 될 수 있다(10-16 검토) — 마감이 지나면 회차 중단
      if (System.currentTimeMillis() > deadline.get()) {
        return null;
      }
      if ("SEQ".equals(st.action())) {
        for (Step sub : st.sub()) {
          Poe2CraftEngine.Item x = runStep(it, sub, targets, names, total, rng, new int[1], last);
          if (x == null) {
            return null;
          }
          it = x;
        }
      } else {
        Poe2CraftEngine.Action act = Poe2CraftEngine.Action.valueOf(st.action());
        String arg = st.arg();
        Set<String> om =
            st.omens() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(st.omens());
        if (act == Poe2CraftEngine.Action.REVEAL && arg == null) {
          // 심연 메아리의 징조(C411) — 셋에 목표가 없으면 한 번 다시 보고(징조 소모) 고른다, 있으면 징조는 남는다
          if (om.contains("AbyssalEchoes") && !offersHitTarget(engine, it, targets)) {
            Poe2CraftEngine.Result rr =
                engine.apply(it, Poe2CraftEngine.Action.REVEAL_REROLL, null, om, rng);
            if (rr.error() == null) {
              it = rr.item();
              for (String o : rr.omensUsed()) {
                total.merge(names.omen(o), 1d, Double::sum);
              }
            }
          }
          om.remove("AbyssalEchoes");
          arg = revealChoice(engine, it, targets);
        }
        Poe2CraftEngine.Result r = engine.apply(it, act, arg, om, rng);
        if (r.error() != null) {
          return null;
        }
        String nm = names.of(act, arg);
        if (nm != null) {
          // 기폭제 "*20" 은 쓴 개수만큼(C389 — note "catalyst:…:개수")
          double cnt = 1;
          if (act == Poe2CraftEngine.Action.CATALYST && r.note() != null) {
            try {
              cnt = Double.parseDouble(r.note().substring(r.note().lastIndexOf(':') + 1));
            } catch (NumberFormatException e) {
              cnt = 1;
            }
          }
          total.merge(nm, cnt, Double::sum);
        }
        for (String o : r.omensUsed()) {
          total.merge(names.omen(o), 1d, Double::sum);
        }
        it = r.item();
        last[0] = it;
      }
      used[0]++;
    }
    return it;
  }

  /** 드러낼 셋에 목표 옵션이 있는가(C411). */
  private static boolean offersHitTarget(
      Poe2CraftEngine engine, Poe2CraftEngine.Item it, List<Target> targets) {
    for (String o : engine.revealOffers(it)) {
      for (Target t : targets) {
        if (t.anyOf().contains(o)) {
          return true;
        }
      }
    }
    return false;
  }

  /** 드러낼 셋에서 목표에 든 것을 먼저, 없으면 첫 번째. */
  private static String revealChoice(
      Poe2CraftEngine engine, Poe2CraftEngine.Item it, List<Target> targets) {
    List<String> offers = engine.revealOffers(it);
    for (String o : offers) {
      for (Target t : targets) {
        if (t.anyOf().contains(o)) {
          return o;
        }
      }
    }
    return offers.isEmpty() ? null : offers.get(0);
  }

  /** 화폐 · 에센스 · 뼈 · 징조 영어 이름(시세 키). */
  record Names(Map<String, String> currency, Map<String, String> other, Map<String, String> omens) {
    String of(Poe2CraftEngine.Action act, String arg) {
      return switch (act) {
        case ESSENCE, DESECRATE -> arg == null ? null : other.get(arg);
        // 기폭제(C389 검토 — 전략 단계에서 이름을 못 찾아 공짜로 셌다), "*N" 은 떼고 이름만
        case CATALYST ->
            arg == null
                ? null
                : currency.get(
                    "CATALYST|" + (arg.contains("*") ? arg.substring(0, arg.indexOf('*')) : arg));
        case REVEAL, REVEAL_REROLL -> null;
        default ->
            currency.get(
                act.name()
                    + "|"
                    + (arg == null
                            || !("greater".equalsIgnoreCase(arg) || "perfect".equalsIgnoreCase(arg))
                        ? ""
                        : arg.toLowerCase()));
      };
    }

    String omen(String key) {
      return omens.getOrDefault(key, key);
    }
  }

  Names names(String poolKey) {
    Poe2CraftService.Options o = craft.options();
    Map<String, String> cur = new java.util.HashMap<>();
    for (Poe2CraftService.Currency c : o.currencies()) {
      cur.put(c.action() + "|" + (c.tier() == null ? "" : c.tier()), c.name());
    }
    Map<String, String> other = new java.util.HashMap<>();
    for (Poe2CraftService.EssenceOption e : craft.essences(poolKey)) {
      other.put(e.slug(), e.name());
    }
    for (Poe2CraftService.BoneInfo b : o.bones()) {
      other.put(b.slug(), b.name());
    }
    Map<String, String> om = new java.util.HashMap<>();
    for (Poe2CraftService.Omen x : o.omens()) {
      om.put(x.key(), x.name());
    }
    return new Names(cur, other, om);
  }

  /**
   * 목표까지 연타(10-20 C508, PoE1 C506 짝) — 목표를 모두 채우거나 최대 횟수까지 같은 화폐(arg = 등급 · 에센스), 징조는 쓰지 않는다(첫 번에
   * 사라지므로).
   */
  public record SpamRequest(
      Poe2CraftEngine.Item item, String action, String arg, List<Target> targets, Integer max) {}

  public record SpamResult(
      Poe2CraftService.View view, int tries, boolean met, List<String> added, String error) {}

  public SpamResult spam(SpamRequest req) {
    Poe2CraftEngine.Item start = craft.normalize(req.item());
    List<Target> targets = req.targets() == null ? List.of() : req.targets();
    int max = Math.max(1, Math.min(5000, req.max() == null ? 1000 : req.max()));
    Poe2CraftEngine.Action act;
    try {
      act = Poe2CraftEngine.Action.valueOf(req.action());
    } catch (RuntimeException e) {
      return new SpamResult(craft.view(start), 0, false, List.of(), "모르는 화폐");
    }
    java.util.random.RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    Poe2CraftEngine.Item cur = start;
    int tries = 0;
    boolean met = !targets.isEmpty() && meets(cur, targets);
    while (!met && tries < max) {
      Poe2CraftEngine.Result r = craft.engine().apply(cur, act, req.arg(), java.util.Set.of(), rng);
      if (r.error() != null) {
        if (tries == 0) {
          return new SpamResult(craft.view(start), 0, false, List.of(), r.error());
        }
        break;
      }
      cur = r.item();
      tries++;
      met = !targets.isEmpty() && meets(cur, targets);
    }
    java.util.Set<String> before = new java.util.HashSet<>();
    start.affixes().forEach(a -> before.add(a.id()));
    List<String> added =
        cur.affixes().stream()
            .map(Poe2CraftEngine.Affix::id)
            .filter(id -> !before.contains(id))
            .toList();
    return new SpamResult(craft.view(cur), tries, met, added, null);
  }

  /**
   * 목표 적중 확률(10-20 C500, PoE1 C494 짝) — omens = 지금 켠 징조, actions 의 arg = 화폐 등급(greater · perfect) ·
   * 에센스 · 뼈.
   */
  public record HitRequest(
      Poe2CraftEngine.Item item,
      List<Target> targets,
      List<HitAction> actions,
      List<String> omens,
      Integer samples) {}

  public record HitAction(String action, String arg) {}

  public record Hit(String action, String arg, Double chance, String error) {}

  /** 화폐 한 번(지금 징조와 함께)으로 목표를 모두 채울 확률 — 같은 제작 엔진 표본, 전체 2만 번 안. */
  public List<Hit> hitChance(HitRequest req) {
    Poe2CraftEngine.Item start = craft.normalize(req.item());
    List<Target> targets = req.targets() == null ? List.of() : req.targets();
    List<HitAction> actions =
        req.actions() == null
            ? List.of()
            // 무인증 API 입력 한도(C540) — 화폐 수가 끝없으면 화폐마다 표본 하한 100 이라 엔진 적용이 한없이 는다
            : req.actions().subList(0, Math.min(200, req.actions().size()));
    java.util.Set<String> omens =
        req.omens() == null ? java.util.Set.of() : new java.util.HashSet<>(req.omens());
    int n = Math.max(100, Math.min(4000, req.samples() == null ? 1000 : req.samples()));
    if (!actions.isEmpty()) {
      n = Math.max(100, Math.min(n, 20_000 / actions.size()));
    }
    java.util.random.RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    List<Hit> out = new java.util.ArrayList<>();
    for (HitAction ha : actions) {
      Poe2CraftEngine.Action act;
      try {
        act = Poe2CraftEngine.Action.valueOf(ha.action());
      } catch (RuntimeException e) {
        out.add(new Hit(ha.action(), ha.arg(), null, "모르는 화폐"));
        continue;
      }
      int hits = 0;
      String error = null;
      for (int i = 0; i < n; i++) {
        Poe2CraftEngine.Result r = craft.engine().apply(start, act, ha.arg(), omens, rng);
        if (r.error() != null) {
          error = r.error();
          break;
        }
        if (!targets.isEmpty() && meets(r.item(), targets)) {
          hits++;
        }
      }
      out.add(new Hit(ha.action(), ha.arg(), error == null ? (double) hits / n : null, error));
    }
    return out;
  }

  /** 빈 자리 조건(10-20 C521, PoE1 C519 짝 · craftofexile 계산기 'open affix') — 목표 하나 = 그 쪽 빈 자리 하나. */
  static final String OPEN_PREFIX = "@open:prefix";

  static final String OPEN_SUFFIX = "@open:suffix";

  static boolean isOpen(Target t) {
    return t != null
        && t.anyOf() != null
        && t.anyOf().size() == 1
        && (OPEN_PREFIX.equals(t.anyOf().get(0)) || OPEN_SUFFIX.equals(t.anyOf().get(0)));
  }

  /** 옵션 목표만(빈 자리 조건 뺌). */
  static List<Target> real(List<Target> targets) {
    return targets.stream().filter(t -> !isOpen(t)).toList();
  }

  /** 요구 빈 자리 [접두, 접미]. */
  static int[] openNeed(List<Target> targets) {
    int[] need = new int[2];
    for (Target t : targets) {
      if (isOpen(t)) {
        need[OPEN_PREFIX.equals(t.anyOf().get(0)) ? 0 : 1]++;
      }
    }
    return need;
  }

  /** 지금 빈 자리 [접두, 접미] — 희귀도 한도(매직 1 · 레어 3 · 주얼 2) − 그 쪽 옵션 수(미공개 훼손 포함). */
  static int[] openFree(Poe2CraftEngine.Item item) {
    int cap = Poe2CraftEngine.maxPerGen(item, item.rarity());
    int p = 0;
    int s = 0;
    for (Poe2CraftEngine.Affix a : item.affixes()) {
      if ("prefix".equals(a.gen())) {
        p++;
      } else if ("suffix".equals(a.gen())) {
        s++;
      }
    }
    return new int[] {Math.max(0, cap - p), Math.max(0, cap - s)};
  }

  /** 결과 분포 요청(10-21 C534, PoE1 C533 짝) — arg = 화폐 등급(greater · perfect) 또는 에센스, omens = 켠 징조. */
  public record DistRequest(
      Poe2CraftEngine.Item item, String action, String arg, List<String> omens, Integer samples) {}

  public record DistLine(String family, String gen, double share, double top) {}

  public record DistResult(
      int samples, List<DistLine> lines, Map<Integer, Double> affixes, String error) {}

  /** 결과 분포(C534) — 화폐 한 번의 결과에 어떤 계열이 몇 % 로 붙나 · 옵션 개수 분포. */
  public DistResult distribution(DistRequest req) {
    Poe2CraftEngine.Item start = craft.normalize(req.item());
    Poe2CraftEngine.Action act;
    try {
      act = Poe2CraftEngine.Action.valueOf(req.action());
    } catch (RuntimeException e) {
      return new DistResult(0, List.of(), Map.of(), "모르는 화폐");
    }
    java.util.Set<String> omens =
        req.omens() == null ? java.util.Set.of() : new java.util.HashSet<>(req.omens());
    int n = Math.max(100, Math.min(20_000, req.samples() == null ? 5_000 : req.samples()));
    java.util.random.RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    Map<String, int[]> fam = new java.util.HashMap<>();
    Map<Integer, Integer> count = new java.util.TreeMap<>();
    int done = 0;
    for (int i = 0; i < n; i++) {
      Poe2CraftEngine.Result r = craft.engine().apply(start, act, req.arg(), omens, rng);
      if (r.error() != null) {
        if (done == 0) {
          return new DistResult(0, List.of(), Map.of(), r.error());
        }
        break;
      }
      Poe2CraftEngine.Item it = r.item();
      done++;
      java.util.Set<String> seen = new java.util.HashSet<>();
      for (Poe2CraftEngine.Affix a : it.affixes()) {
        String key = a.gen() + "|" + craft.familyOf(a.id());
        if (!seen.add(key)) {
          continue;
        }
        int[] c = fam.computeIfAbsent(key, k -> new int[2]);
        c[0]++;
        Integer t = craft.tierRank(it.poolKey(), a.id());
        if (t != null && t == 1) {
          c[1]++;
        }
      }
      count.merge(it.affixes().size(), 1, Integer::sum);
    }
    int total = done;
    List<DistLine> lines =
        fam.entrySet().stream()
            .map(
                e ->
                    new DistLine(
                        e.getKey().substring(e.getKey().indexOf('|') + 1),
                        e.getKey().substring(0, e.getKey().indexOf('|')),
                        (double) e.getValue()[0] / total,
                        (double) e.getValue()[1] / total))
            .sorted(java.util.Comparator.comparingDouble(DistLine::share).reversed())
            .toList();
    Map<Integer, Double> affixes = new java.util.TreeMap<>();
    count.forEach((k, v) -> affixes.put(k, (double) v / total));
    return new DistResult(total, lines, affixes, null);
  }

  /** 목표마다 서로 다른 옵션으로 채우는가(작은 이분 매칭). */
  static boolean meets(Poe2CraftEngine.Item item, List<Target> all) {
    int[] need = openNeed(all);
    if (need[0] + need[1] > 0) {
      int[] free = openFree(item);
      if (free[0] < need[0] || free[1] < need[1]) {
        return false;
      }
    }
    List<Target> targets = real(all);
    List<String> ids = item.affixes().stream().map(Poe2CraftEngine.Affix::id).toList();
    int[] owner = new int[ids.size()];
    java.util.Arrays.fill(owner, -1);
    for (int t = 0; t < targets.size(); t++) {
      if (!assign(t, targets, ids, owner, new boolean[ids.size()])) {
        return false;
      }
    }
    return true;
  }

  private static boolean assign(
      int t, List<Target> targets, List<String> ids, int[] owner, boolean[] seen) {
    for (int i = 0; i < ids.size(); i++) {
      if (!seen[i] && targets.get(t).anyOf().contains(ids.get(i))) {
        seen[i] = true;
        if (owner[i] < 0 || assign(owner[i], targets, ids, owner, seen)) {
          owner[i] = t;
          return true;
        }
      }
    }
    return false;
  }
}
