package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoeCraftEngine.Action;
import net.luversof.api.poe.service.PoeCraftEngine.Item;
import net.luversof.api.poe.service.PoeCraftEngine.Rarity;

/**
 * 제작 계산기(10-15 C336 — 사용자 요청 "특정 아이템을 만들기 위해 가장 적은 비용을 쓰는 걸 계산해 주는 시뮬레이터"). 목표 옵션 묶음(묶음마다 받아들일 옵션
 * id 들 — 예: 생명력 T1~T3)을 정하면 방법마다 한 번 시도의 성공 확률을 엔진으로 몬테카를로 추정하고, 기대 시도 수 = 1/확률, 기대 비용 = 시도당 화폐 ×
 * poe.ninja 단가로 비용 순 정렬한다.
 *
 * <p>방법(시도 한 번 = 이 단위를 성공할 때까지 되풀이):
 *
 * <ul>
 *   <li>CHAOS — 레어에 카오스 1개(일반이면 처음 한 번 연금, 비용에 안 셈)
 *   <li>ALT — 매직에 변경 1개, 옵션이 하나면 확장 1개 더(목표는 접두 1 · 접미 1 이하)
 *   <li>ESSENCE — 그 에센스 1개(보장 옵션 + 나머지는 카오스처럼)
 * </ul>
 */
@Service
public class PoeCraftCalcService {

  /** 목표 하나 — anyOf 중 하나가 붙으면 충족(서로 다른 목표는 서로 다른 옵션으로 채워야 한다). */
  public record Target(List<String> anyOf) {}

  public record Request(
      Item item,
      List<Target> targets,
      List<String> methods,
      String essence,
      Integer trials,
      // 사용자 가격(C529) — 이름 → 카오스, 없으면 시세
      Map<String, Double> prices) {

    public Request(
        Item item, List<Target> targets, List<String> methods, String essence, Integer trials) {
      this(item, targets, methods, essence, trials, null);
    }
  }

  /**
   * 방법 하나의 결과 — chance = 시도 한 번 성공 확률(hits 0 이면 0 이고 upperBound = 1/trials 미만이라는 뜻), perAttempt =
   * 시도당 화폐 개수(이름 → 평균 개수), costPerAttempt · expectedCost = 카오스(시세가 없으면 null).
   */
  public record MethodResult(
      String method,
      String label,
      int trials,
      int hits,
      double chance,
      Double expectedAttempts,
      Map<String, Double> perAttempt,
      Double costPerAttempt,
      Double expectedCost,
      String note,
      // 영어 화면용(10-21 C557 — 영어 화면 계산기 표 · 최적 조합에 한국어 라벨이 섞였다). 없으면 null(게이트가 한국어로)
      String labelEn,
      String noteEn) {}

  public record Response(
      List<MethodResult> results,
      String league,
      String pricesAt,
      String error,
      // 섬뜩한 고정 속성 목표(C549) — 명시 옵션 표와 따로 잰 비용(더해서 본다), 목표가 없으면 null
      EldritchResult eldritch,
      // error 의 영어 판(C557)
      String errorEn) {

    public Response(List<MethodResult> results, String league, String pricesAt, String error) {
      this(results, league, pricesAt, error, null, null);
    }

    public Response(
        List<MethodResult> results,
        String league,
        String pricesAt,
        String error,
        EldritchResult eldritch) {
      this(results, league, pricesAt, error, eldritch, null);
    }

    /** 오류만(영어 판과 함께, C557). */
    static Response error(String error, String errorEn) {
      return new Response(List.of(), null, null, error, null, errorEn);
    }
  }

  /**
   * 섬뜩한 고정 속성 목표 비용(10-21 C549 사용자 요청 "엘드리치 옵션도 목표로 — 최종 아이템에 붙는 옵션 기준으로 최소 비용") — 고정 속성은 명시 옵션을 다시
   * 굴려도 남고 섬뜩한 불씨 · 영액은 그쪽 고정 속성만 바꿔, 명시 옵션 제작과 따로 재서 더한다. cost = 쪽마다 (화폐 한 개 값 / 그 고정 속성이 나올 확률)의
   * 합, 목표 등급 이상 화폐 등급(하급 1 ~ 특출난 4) 중 가장 싼 것. error 면 못 함(종류 · 영향력 · 같은 쪽 둘 · 등급 5~6).
   */
  public record EldritchResult(
      Double cost,
      List<EldritchStep> steps,
      String error,
      // error 의 영어 판(C557)
      String errorEn) {

    public EldritchResult(Double cost, List<EldritchStep> steps, String error) {
      this(cost, steps, error, null);
    }
  }

  /** 쪽 하나 — currencyTier = 쓸 화폐 등급, chance = 한 개로 그 고정 속성(목표 등급 이상)이 나올 확률, attempts = 1/chance. */
  public record EldritchStep(
      String side,
      String family,
      List<String> en,
      List<String> ko,
      int minTier,
      int currencyTier,
      String currency,
      String currencyKo,
      double chance,
      double attempts,
      Double cost) {}

  /**
   * 에뮬레이터에서 쓴 단계 하나(C365) — action = 엔진 화폐 이름 또는 FORESEE(히네코라의 머리카락), arg = 화폐 인자. times = 같은 화폐를
   * 연달아 쓴 횟수(C506 목표까지 연타, 없으면 1).
   */
  public record SpentStep(String action, String arg, Integer times) {

    public SpentStep(String action, String arg) {
      this(action, arg, null);
    }
  }

  public record SpentRequest(
      String itemClass,
      List<SpentStep> steps,
      // 사용자 가격(C529)
      Map<String, Double> prices) {

    public SpentRequest(String itemClass, List<SpentStep> steps) {
      this(itemClass, steps, null);
    }
  }

  /** 화폐 한 줄 — 개수 · 단가(카오스, 시세 없으면 null) · 합계(개수 × 단가). */
  public record SpentLine(
      String name, String nameKo, double count, Double unitChaos, Double subtotalChaos) {}

  /** 야수 제작 인자 → 쓴 화폐 표 이름(C387, BestiaryRecipes 설명 — 한국어는 SPENT_KO). */
  static final Map<String, String> BEAST_RECIPE =
      Map.ofEntries(
          Map.entry("swap-suffix", "Bestiary: Add a Suffix, Remove a Random Prefix"),
          Map.entry("swap-prefix", "Bestiary: Add a Prefix, Remove a Random Suffix"),
          Map.entry("influence:shaper", "Bestiary: Add a Mod to a Shaper Item"),
          Map.entry("influence:elder", "Bestiary: Add a Mod to an Elder Item"),
          Map.entry("influence:eyrie", "Bestiary: Add a Mod to a Redeemer Item"),
          Map.entry("influence:basilisk", "Bestiary: Add a Mod to a Hunter Item"),
          Map.entry("influence:crusader", "Bestiary: Add a Mod to a Crusader Item"),
          Map.entry("influence:adjudicator", "Bestiary: Add a Mod to a Warlord Item"),
          // 위상 스킬(C388)
          Map.entry("aspect:cat", "Bestiary: Level 30 Aspect of the Cat skill"),
          Map.entry("aspect:avian", "Bestiary: Level 30 Aspect of the Avian skill"),
          Map.entry("aspect:spider", "Bestiary: Level 30 Aspect of the Spider skill"),
          Map.entry("aspect:crab", "Bestiary: Level 30 Aspect of the Crab skill"),
          // 각인(C468 — 최적 조합)
          Map.entry("imprint", "Bestiary: Imprint"));

  /** 쓴 화폐 한국어 이름 — 데이터(화폐 설명 · 에센스 · 화석 · 공명기 · 작업대 비용)에 없는 것(BaseItemTypes 한국어, C380). */
  static final Map<String, String> SPENT_KO =
      Map.ofEntries(
          Map.entry("Bestiary: Add a Suffix, Remove a Random Prefix", "야수 제작: 접미어 추가, 무작위 접두어 제거"),
          Map.entry("Bestiary: Add a Prefix, Remove a Random Suffix", "야수 제작: 접두어 추가, 무작위 접미어 제거"),
          Map.entry("Bestiary: Add a Mod to a Shaper Item", "야수 제작: 쉐이퍼 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to an Elder Item", "야수 제작: 엘더 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Redeemer Item", "야수 제작: 대속자 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Hunter Item", "야수 제작: 사냥꾼 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Crusader Item", "야수 제작: 십자군 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Warlord Item", "야수 제작: 전쟁군주 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Level 30 Aspect of the Cat skill", "야수 제작: 30레벨 고양이의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Avian skill", "야수 제작: 30레벨 새의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Spider skill", "야수 제작: 30레벨 거미의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Crab skill", "야수 제작: 30레벨 게의 위상 스킬"),
          Map.entry("Bestiary: Imprint", "야수 제작: 각인"),
          Map.entry("Orb of Transmutation", "진화의 오브"),
          Map.entry("Orb of Alteration", "변화의 오브"),
          Map.entry("Orb of Augmentation", "확장의 오브"),
          Map.entry("Regal Orb", "제왕의 오브"),
          Map.entry("Orb of Alchemy", "연금술의 오브"),
          Map.entry("Chaos Orb", "카오스 오브"),
          Map.entry("Exalted Orb", "엑잘티드 오브"),
          Map.entry("Orb of Annulment", "소멸의 오브"),
          Map.entry("Orb of Scouring", "정제의 오브"),
          Map.entry("Fracturing Orb", "분열의 오브"),
          Map.entry("Divine Orb", "신성한 오브"),
          Map.entry("Orb of Conflict", "갈등의 오브"),
          Map.entry("Foulborn Orb of Augmentation", "삿된 확장의 오브"),
          Map.entry("Foulborn Regal Orb", "삿된 제왕의 오브"),
          Map.entry("Foulborn Exalted Orb", "삿된 엑잘티드 오브"),
          Map.entry("Blessed Orb", "축복의 오브"),
          Map.entry("Vaal Orb", "바알 오브"),
          Map.entry("Hinekora's Lock", "히네코라의 머리카락"),
          Map.entry("Wild Crystallised Lifeforce", "야생 생기 결정"),
          Map.entry("Vivid Crystallised Lifeforce", "혈기 생기 결정"),
          Map.entry("Primal Crystallised Lifeforce", "원시 생기 결정"),
          Map.entry("Sacred Crystallised Lifeforce", "신성한 생기 결정"));

  /** totalChaos = 시세 있는 것만 더한 값, missing = 시세 없는 화폐 수. divineChaos = 디바인 한 개의 카오스 값. */
  public record SpentResult(
      List<SpentLine> lines,
      double totalChaos,
      int missing,
      Double divineChaos,
      String league,
      String pricesAt,
      // 단계별(C455 — 에뮬레이터 단계 기록에 그 단계 비용, 요청 steps 와 같은 순서)
      List<StepCost> steps) {}

  /** 한 단계에서 쓴 화폐(이름 " ×개수") · 그 단계 비용(카오스 — 시세 없는 화폐가 끼면 null, 쓴 화폐가 없으면 0). */
  public record StepCost(List<String> names, List<String> namesKo, Double chaos) {}

  static final int DEFAULT_TRIALS = 50_000;
  static final int MAX_TRIALS = 1_000_000;

  /** 성공 표본이 이보다 적으면 시도를 늘려 다시(C374). */
  static final int LOW_HITS = 30;

  /** 다시 계산 배수(C378 — 고정). */
  static final int RERUN_FACTOR = 4;

  /** 기본 방법 + 다시 계산에 쓰는 시간 예산(게이트 대기 10초 — 뒤에 화석 · 수확 등 자동 비교가 남아 있다). */
  static final long RERUN_BUDGET_MS = 2_000L;

  private final PoeCraftService craft;
  private final PoeCraftPriceService prices;

  public PoeCraftCalcService(PoeCraftService craft, PoeCraftPriceService prices) {
    this.craft = craft;
    this.prices = prices;
  }

  public Response calc(Request req) {
    // 섬뜩한 고정 속성 목표(C549) — 따로 재서 붙이고, 명시 옵션 목표만 방법 표로
    List<Target> all = req.targets() == null ? List.of() : req.targets();
    List<Target> eld = all.stream().filter(this::eldritchTarget).toList();
    if (!eld.isEmpty()) {
      EldritchResult er = eldritchPlan(craft.normalize(req.item()), eld);
      List<Target> rest = all.stream().filter(t -> !eldritchTarget(t)).toList();
      if (real(rest).isEmpty()) {
        PoeCraftPriceService.Prices p = prices.prices();
        return new Response(List.of(), p.league(), p.fetchedAt(), null, er);
      }
      Response r =
          calc(
              new Request(
                  req.item(), rest, req.methods(), req.essence(), req.trials(), req.prices()));
      return new Response(r.results(), r.league(), r.pricesAt(), r.error(), er, r.errorEn());
    }
    if (req.targets() == null || real(req.targets()).isEmpty()) {
      // 빈 자리 조건(C519)만으로는 계산하지 않는다 — 옵션 목표와 함께
      return Response.error("목표 옵션을 하나 이상 고르세요", "Choose at least one target modifier");
    }
    for (int i = 0; i < req.targets().size(); i++) {
      Target t = req.targets().get(i);
      if (t == null || t.anyOf() == null || t.anyOf().isEmpty()) {
        // 빈 목표면 모든 방법이 0% 로 나와 "불가능한 조합"처럼 보였다(10-15 실측 — 계열 키를 잘못 준 요청)
        return Response.error(
            "목표 " + (i + 1) + "에 옵션이 없습니다", "Target " + (i + 1) + " has no modifiers");
      }
    }
    Item item = craft.normalize(req.item());
    if (item.corrupted()) {
      // 타락한 아이템(C382) — 화폐가 모두 막혀, 이미 목표를 채운 아이템이면 '성공 100%'처럼 나왔다
      return Response.error(
          "타락한 아이템은 화폐로 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)",
          "Corrupted items cannot be changed with currency, so nothing is calculated (turn off 'Start"
              + " from the current item' to calculate from a new base)");
    }
    int trials =
        Math.max(1_000, Math.min(MAX_TRIALS, req.trials() == null ? DEFAULT_TRIALS : req.trials()));
    List<String> methods =
        req.methods() == null || req.methods().isEmpty()
            ? List.of("CHAOS", "ALT", "ESSENCE", "ALT_REGAL", "ALT_FREGAL")
            : req.methods();
    List<MethodResult> out = new ArrayList<>();
    long started = System.currentTimeMillis();
    for (String m : methods) {
      long t0 = System.currentTimeMillis();
      MethodResult r =
          run(m, item, req.targets(), req.essence(), trials, new SplittableRandom(m.hashCode()));
      long took = Math.max(1, System.currentTimeMillis() - t0);
      // 성공 표본이 적으면(30번 미만) 시도를 늘려 다시(C374 — 5만 번 중 5번이면 비용이 ±45% 흔들렸다). 걸린 시간으로 배수를 정해 예산
      // 안에서만(2~4배)
      // 배수는 4 고정(같은 요청 같은 결과 — C378 검토: 걸린 시간으로 배수를 정하면 부하에 따라 결과가 달랐다), 시간은 안전장치로만
      long left = RERUN_BUDGET_MS - (System.currentTimeMillis() - started);
      if (r != null
          && r.trials() > 0
          && r.hits() < LOW_HITS
          && trials < MAX_TRIALS
          && left >= took * RERUN_FACTOR) {
        int more = Math.min(MAX_TRIALS, trials * RERUN_FACTOR);
        MethodResult again =
            run(
                m,
                item,
                req.targets(),
                req.essence(),
                more,
                new SplittableRandom(m.hashCode() * 31L + 7));
        if (again != null && again.trials() > 0) {
          r = merge(r, again);
        }
      }
      if (r != null) {
        out.add(r);
      }
    }
    // 섞어 쓰는 제작 방법(10-15 C339) — 방법을 지정하지 않으면 화석 자동 조합 · 수확 태그 재제작 · 목표를 채우는 에센스도 비교한다
    if (req.methods() == null || req.methods().isEmpty()) {
      // 자동 비교는 시도 수 절반(화면이 기다릴 수 있게 — 10-15 실측 37초)
      int sub = Math.max(5_000, trials / 2);
      out.addAll(autoEssences(item, req.targets(), req.essence(), sub));
      out.addAll(fossils(item, req.targets(), sub));
      out.addAll(harvests(item, req.targets(), sub));
      out.addAll(eldritchChaos(item, req.targets(), sub));
      out.addAll(veiledChaos(item, req.targets(), sub));
      // 여러 방법을 섞는 조합은 최적 조합(C457 PoeCraftOptimizer)이 맡는다 — C456 의 고정 조합은 C458 에서 뺐다
    }
    out.sort(
        Comparator.comparing(
                (MethodResult r) -> r.expectedCost() == null ? Double.MAX_VALUE : r.expectedCost())
            .thenComparing(
                r -> r.expectedAttempts() == null ? Double.MAX_VALUE : r.expectedAttempts()));
    PoeCraftPriceService.Prices p = prices.prices();
    return new Response(out, p.league(), p.fetchedAt(), null);
  }

  private MethodResult run(
      String method,
      Item item,
      List<Target> targets,
      String essence,
      int trials,
      RandomGenerator rng) {
    PoeCraftEngine engine = craft.engine();
    Map<String, Double> per = new LinkedHashMap<>();
    int hits = 0;
    String note = null;
    String noteEn = null;
    switch (method) {
      case "CHAOS" -> {
        // 레어 출발점 — 일반이면 연금 한 번(비용 밖), 매직이면 제왕은 옵션이 남아 결과가 달라 여기선 정제 → 연금으로 본다
        Item start = item;
        if (item.rarity() != Rarity.RARE) {
          start = normal(item);
          if (start.rarity() == Rarity.NORMAL) {
            start = engine.apply(start, Action.ALCHEMY, null, rng).item();
            note = "일반 → 연금 한 번 뒤 카오스";
            noteEn = "Normal → one Alchemy, then Chaos";
          }
        }
        for (int i = 0; i < trials; i++) {
          if (meets(engine.apply(start, Action.CHAOS, null, rng).item(), targets)) {
            hits++;
          }
        }
        per.put("Chaos Orb", 1.0);
      }
      case "ALT_REGAL", "ALT_FREGAL" -> {
        // 변화(+확장)로 목표 둘을 매직에 맞추면 제왕 한 번(C372) — 목표가 셋일 때만. 둘을 못 맞춘 회차는 제왕을 안 쓴다
        // ALT_FREGAL(C403) = 마지막을 삿된 제왕으로(고레벨 가중 — 상위 등급 셋째 목표면 비싸도 나을 수 있다)
        Action regalAct = "ALT_FREGAL".equals(method) ? Action.FOULBORN_REGAL : Action.REGAL;
        List<Target> opt = real(targets);
        if (opt.size() != 3) {
          return null;
        }
        List<PoeCraftEngine.Affix> fr = fractured(item);
        if (fr.stream().filter(a -> "prefix".equals(a.gen())).count() > 1
            || fr.stream().filter(a -> !"prefix".equals(a.gen())).count() > 1) {
          return null;
        }
        Item start =
            new Item(
                item.itemClass(),
                item.variant(),
                item.influence(),
                item.ilvl(),
                Rarity.MAGIC,
                fr,
                false,
                item.implicits());
        List<List<Target>> pairs =
            List.of(
                List.of(opt.get(0), opt.get(1)),
                List.of(opt.get(0), opt.get(2)),
                List.of(opt.get(1), opt.get(2)));
        long augs = 0;
        long regals = 0;
        for (int i = 0; i < trials; i++) {
          Item r = engine.apply(start, Action.ALTERATION, null, rng).item();
          if (r.affixes().size() < 2) {
            PoeCraftEngine.Result a = engine.apply(r, Action.AUGMENTATION, null, rng);
            if (a.error() == null) {
              augs++;
              r = a.item();
            }
          }
          final Item mg = r;
          if (pairs.stream().anyMatch(pr -> meets(mg, pr))) {
            regals++;
            if (meets(engine.apply(r, regalAct, null, rng).item(), targets)) {
              hits++;
            }
          }
        }
        per.put("Orb of Alteration", 1.0);
        per.put("Orb of Augmentation", (double) augs / trials);
        per.put(
            regalAct == Action.FOULBORN_REGAL ? "Foulborn Regal Orb" : "Regal Orb",
            (double) regals / trials);
        // 제왕이 실패하면 레어라 변화를 못 쓴다 — 정제 → 진화로 매직에 돌려야(C378 검토, 공짜로 다시 시작해 비용이 낮았다)
        per.put("Orb of Scouring", (double) (regals - hits) / trials);
        per.put("Orb of Transmutation", (double) (regals - hits) / trials);
        note = "목표 둘이 매직에 맞은 회차만 제왕(회차 " + String.format("%.2f%%", 100.0 * regals / trials) + ")";
        noteEn =
            "Regal only when both targets hit the magic item ("
                + String.format("%.2f%%", 100.0 * regals / trials)
                + " of runs)";
      }
      case "ALT" -> {
        if (!magicFeasible(targets)) {
          return null;
        }
        // 분열 옵션은 매직에도 남는다 — 접두 · 접미 하나씩까지만 매직이 될 수 있다
        List<PoeCraftEngine.Affix> fr = fractured(item);
        if (fr.stream().filter(a -> "prefix".equals(a.gen())).count() > 1
            || fr.stream().filter(a -> !"prefix".equals(a.gen())).count() > 1) {
          return null;
        }
        Item start =
            new Item(
                item.itemClass(),
                item.variant(),
                item.influence(),
                item.ilvl(),
                Rarity.MAGIC,
                fr,
                false,
                item.implicits());
        long augs = 0;
        for (int i = 0; i < trials; i++) {
          Item r = engine.apply(start, Action.ALTERATION, null, rng).item();
          if (r.affixes().size() < 2 && !meets(r, targets)) {
            PoeCraftEngine.Result a = engine.apply(r, Action.AUGMENTATION, null, rng);
            if (a.error() == null) {
              augs++;
              r = a.item();
            }
          }
          if (meets(r, targets)) {
            hits++;
          }
        }
        per.put("Orb of Alteration", 1.0);
        per.put("Orb of Augmentation", (double) augs / trials);
        note = "매직 — 옵션이 하나로 나오면 확장";
        noteEn = "Magic — Augmentation when only one modifier rolls";
      }
      case "ESSENCE" -> {
        PoeCraftDataService.Essence e =
            essence == null
                ? null
                : craft.essences(item.itemClass()).stream()
                    .filter(x -> essence.equals(x.slug()))
                    .findFirst()
                    .orElse(null);
        if (e == null) {
          return null;
        }
        Item start = normal(item);
        // 에센스 보장 옵션은 일반 옵션과 id 가 달라(IncreasedLifeEssenceBootsGloves1 ↔ IncreasedLife8) 목표 목록에 없다 —
        // 같은 옵션 그룹이고
        //   최소 굴림이 목표 최하 등급의 최소 굴림 이상이면 그 목표를 채운 것으로 본다(10-15 실측: 탐욕 에센스가 생명력 T1~T3 에 0% 로 나왔다)
        List<Target> goal = new ArrayList<>();
        for (Target t : targets) {
          goal.add(essenceCounts(e, t) ? new Target(concat(t.anyOf(), e.modId())) : t);
        }
        for (int i = 0; i < trials; i++) {
          PoeCraftEngine.Result r = engine.apply(start, Action.ESSENCE, essence, rng);
          if (r.error() != null) {
            return new MethodResult(
                method,
                label(method, e),
                0,
                0,
                0,
                null,
                Map.of(),
                null,
                null,
                r.error(),
                labelEn(method, e),
                null);
          }
          if (meets(r.item(), goal)) {
            hits++;
          }
        }
        per.put(e.name(), 1.0);
        note = "보장 옵션: " + String.join(", ", e.ko() != null && !e.ko().isEmpty() ? e.ko() : e.en());
        noteEn = "Guaranteed: " + String.join(", ", e.en() == null ? List.of() : e.en());
      }
      default -> {
        return null;
      }
    }
    double chance = (double) hits / trials;
    Double attempts = hits == 0 ? null : 1 / chance;
    Double cost = cost(per);
    PoeCraftDataService.Essence e =
        "ESSENCE".equals(method) && essence != null
            ? craft.essences(item.itemClass()).stream()
                .filter(x -> essence.equals(x.slug()))
                .findFirst()
                .orElse(null)
            : null;
    return new MethodResult(
        method,
        label(method, e),
        trials,
        hits,
        chance,
        attempts,
        per,
        cost,
        cost == null || attempts == null ? null : cost * attempts,
        hits == 0 ? neverHit(note, trials, false) : note,
        labelEn(method, e),
        hits == 0 ? neverHit(noteEn, trials, true) : noteEn);
  }

  /** 한 번도 안 나온 줄의 설명 — 앞 설명 뒤에 '시도 N번에 한 번도 안 나옴'(영어 판 C557). */
  static String neverHit(String note, int trials, boolean en) {
    return (note == null ? "" : note + " · ")
        + (en
            ? "never hit in " + trials + " tries (chance < 1/" + trials + ")"
            : "시도 " + trials + "번에 한 번도 안 나옴(확률 < 1/" + trials + ")");
  }

  /** 첫 계산과 다시 계산을 합친다(C378) — 성공 · 시도 합, 시도당 화폐는 시도 수로 가중 평균. */
  private MethodResult merge(MethodResult a, MethodResult b) {
    int trials = a.trials() + b.trials();
    int hits = a.hits() + b.hits();
    Map<String, Double> per = new java.util.LinkedHashMap<>();
    for (MethodResult x : List.of(a, b)) {
      x.perAttempt().forEach((k, v) -> per.merge(k, v * x.trials() / trials, Double::sum));
    }
    double chance = (double) hits / trials;
    Double attempts = hits == 0 ? null : 1 / chance;
    Double cost = cost(per);
    return new MethodResult(
        b.method(),
        b.label(),
        trials,
        hits,
        chance,
        attempts,
        per,
        cost,
        cost == null || attempts == null ? null : cost * attempts,
        b.note(),
        b.labelEn(),
        b.noteEn());
  }

  /** label 의 영어 판(C557) — 게임 화폐 영어 이름. */
  private static String labelEn(String method, PoeCraftDataService.Essence e) {
    return switch (method) {
      case "CHAOS" -> "Chaos Orb spam";
      case "ALT" -> "Orb of Alteration spam (+Augmentation)";
      case "ALT_REGAL" -> "Alteration (+Aug) for two targets → Regal for the third";
      case "ALT_FREGAL" ->
          "Alteration (+Aug) for two targets → Foulborn Regal for the third (higher-level weighting)";
      case "ESSENCE" -> e == null ? "Essence spam" : e.name() + " spam";
      default -> method;
    };
  }

  private static String label(String method, PoeCraftDataService.Essence e) {
    return switch (method) {
      case "CHAOS" -> "카오스 오브 연타";
      case "ALT" -> "변화의 오브 연타(+확장)";
      case "ALT_REGAL" -> "변화(+확장)로 목표 둘 → 제왕으로 셋째";
      case "ALT_FREGAL" -> "변화(+확장)로 목표 둘 → 삿된 제왕으로 셋째(고레벨 가중)";
      case "ESSENCE" -> e == null ? "에센스 연타" : e.nameKo() + " 연타";
      default -> method;
    };
  }

  /**
   * 다단계 전략 한 단계(10-15 C339 — craftofexile 시뮬레이터처럼 단계 · 조건). until = 이 목표들(목표 번호)을 채울 때까지 반복(null 이면
   * 한 번), max = 반복 상한(넘으면 그 회차는 실패로 끝내고 처음부터).
   */
  public record Step(String action, String arg, List<Integer> until, Integer max, List<Step> sub) {}

  public record StrategyRequest(
      Item item,
      List<Target> targets,
      List<Step> steps,
      Integer trials,
      // 사용자 가격(10-23 Q20a — 전엔 전략 요청에 없어 '가격 직접 입력'이 전략 비용에 안 먹었다)
      Map<String, Double> prices) {

    public StrategyRequest(Item item, List<Target> targets, List<Step> steps, Integer trials) {
      this(item, targets, steps, trials, null);
    }
  }

  /** 단계 통계 — avgUses = 회차당 평균 사용 수(묶음이면 묶음 반복 수), reached = 이 단계까지 온 회차 비율. */
  public record StepStat(String action, String arg, double avgUses, double reached) {}

  /**
   * 전략 결과 — runs = 모의 회차, successRate = 회차가 모든 목표를 채운 비율, perRun = 회차당 평균 화폐 수, costPerRun ·
   * expectedCost(성공 한 번당 = 회차 비용 / 성공률, 카오스). 시세를 모르는 화폐가 있으면 비용 null.
   */
  public record StrategyResult(
      int runs,
      int successes,
      double successRate,
      Map<String, Double> perRun,
      Double costPerRun,
      Double expectedCost,
      List<StepStat> steps,
      String error,
      // 성공 예시(10-22 Q16 — craftofexile 시뮬레이터 'Store Items': 목표를 채운 아이템을 몇 개 남겨 같이 붙은 옵션을 본다). 처음
      // STRATEGY_SAMPLES 개
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
  public record StrategySample(Item item, List<PoeCraftService.Line> lines) {}

  static final int STRATEGY_DEFAULT_RUNS = 20_000;
  static final int STRATEGY_SAMPLES = 5;
  static final int STRATEGY_MAX_STEP = 500;

  /**
   * 기억 없는 다시 굴리기의 표본(10-15 C339) — 남는 옵션(분열 · 잠긴 쪽)이 같으면 카오스 · 변경 · 에센스 · 화석 · 수확 태그 재제작의 결과 분포가 매번
   * 같다. 그래서 반복 단계는 표본 한 번(성공 확률 p · 성공/실패 결과 몇 개)으로 회차마다 "몇 번 만에 성공"을 기하분포로 뽑는다 — 회차마다 수천 번 굴리던
   * 것(10-15 실측: 카오스 반복 회차당 2,300번 → 300만 번에 49초)을 표본 4,000번으로.
   */
  private static final class Sampler {
    double p;
    final List<Item> hits = new ArrayList<>();
    final List<Item> misses = new ArrayList<>();
    // 묶음 한 번 평균 화폐 소모(이름 → 개수)
    final Map<String, Double> perUse = new LinkedHashMap<>();
  }

  // 표본: 최소 4,000번, 성공이 40번 나올 때까지(확률이 낮으면 4,000번으론 성공 0~1번이라 p 를 못 잡았다 — 카오스 0.036% 가 0.03% 성공률로
  // 나옴),
  //   최대 30만 번
  static final int SAMPLE = 4_000;
  static final int SAMPLE_MAX = 300_000;
  static final int SAMPLE_HITS = 100;
  // 표본 비용은 회차 예산과 따로 — 표본은 같은 열쇠면 다시 쓰니 회차 수를 깎지 않게. 대신 요청 전체 상한(표본 종류 · 호출 수)
  static final int SAMPLER_KINDS_MAX = 64;
  static final long SAMPLER_OPS_MAX = 3_000_000L;
  static final int SAMPLE_KEEP = 200;

  private static boolean memoryless(PoeCraftEngine.Action act, String arg, PoeCraftService craft) {
    return switch (act) {
      case CHAOS, ALTERATION, ESSENCE, FOSSIL, ALCHEMY, TRANSMUTE, VEILED_CHAOS -> true;
      case HARVEST ->
          craft.harvests().stream()
              .anyMatch(
                  h ->
                      h.id().equals(arg)
                          && ("reroll_with_mod".equals(h.command())
                              || "reroll_with_influence_mod".equals(h.command())));
      default -> false;
    };
  }

  /**
   * 표본 열쇠 — 화폐 · 인자 · 희귀도 · 영향력 + 남는 옵션 중 목표에 드는 것(id) + 목표와 무관한 남는 옵션은 접두/접미 개수만. 무관한 옵션이 막는 옵션
   * 그룹은 무시하는 근사(그 옵션까지 열쇠에 넣으면 잠금 전략에서 회차마다 열쇠가 달라 표본을 매번 새로 떴다 — 10-15 실측 473회차에서 예산 소진).
   */
  private static String signature(
      PoeCraftEngine.Action act, String arg, Item item, java.util.Set<String> targetIds) {
    StringBuilder sb =
        new StringBuilder(act.name())
            .append('|')
            .append(arg)
            .append('|')
            .append(item.rarity())
            .append('|')
            .append(item.influence())
            .append('|');
    int otherPre = 0;
    int otherSuf = 0;
    List<String> ids = new ArrayList<>();
    for (PoeCraftEngine.Affix a : PoeCraftEngine.kept(item)) {
      if (targetIds.contains(a.id())) {
        ids.add(a.id() + (a.crafted() ? "c" : "") + (a.fractured() ? "f" : ""));
      } else if ("prefix".equals(a.gen())) {
        otherPre++;
      } else {
        otherSuf++;
      }
    }
    java.util.Collections.sort(ids);
    ids.forEach(x -> sb.append(x).append(','));
    return sb.append("|op").append(otherPre).append("|os").append(otherSuf).toString();
  }

  /** 한 회차 진행 상태 — 아이템 · 중단 여부 · 화폐 합계(회차마다 합산). */
  private static final class Run {
    Item item;
    boolean aborted;
    Map<String, Sampler> samplers;
    long[] samplerOps;
    // 엔진 호출 수(요청 전체 예산 — 확률이 낮은 반복 단계가 회차마다 수천 번 돌면 수천만 번이 됐다, 10-15 실측 3분+)
    long ops;
  }

  /** 전략 요청 하나의 엔진 호출 예산 — 넘으면 그때까지 돈 회차로 결과를 낸다. */
  static final long STRATEGY_BUDGET = 400_000L;

  /**
   * 전략에 쓰인 에센스 · 작업대 옵션이 채우는 목표에 그 옵션 id 를 더한다(10-22 Q12) — 계산기 essenceCounts · benchCounts 와 같은
   * 규칙(같은 옵션 그룹 · 같은 문구 모양 · 최소 굴림이 목표 최하 등급 최소 이상). 목표 순서 · 수는 그대로(단계의 until 이 목표 번호를 쓴다).
   */
  List<Target> equivalentGoal(Item item, List<Target> targets, List<Step> steps) {
    java.util.Set<String> ess = new java.util.HashSet<>();
    java.util.Set<String> bench = new java.util.HashSet<>();
    collectArgs(steps, ess, bench);
    if (ess.isEmpty() && bench.isEmpty()) {
      return targets;
    }
    PoeCraftService.Options o =
        craft.options(item.itemClass(), item.variant() == null ? "" : item.variant());
    List<PoeCraftDataService.Essence> es =
        (o.essences() == null ? List.<PoeCraftDataService.Essence>of() : o.essences())
            .stream().filter(e -> ess.contains(e.slug()) && e.modId() != null).toList();
    List<PoeCraftDataService.BenchCraft> bs =
        (o.bench() == null ? List.<PoeCraftDataService.BenchCraft>of() : o.bench())
            .stream().filter(b -> b.modId() != null && bench.contains(b.modId())).toList();
    List<Target> out = new ArrayList<>();
    for (Target t : targets) {
      // 무인증 API 입력 방어(Q15 검토) — 빈 목표는 그대로(연타 · 적중 확률은 앞에서 거르지 않는다)
      if (t == null || t.anyOf() == null) {
        out.add(t);
        continue;
      }
      List<String> ids = new ArrayList<>(t.anyOf());
      for (PoeCraftDataService.Essence e : es) {
        if (!ids.contains(e.modId()) && essenceCounts(e, t)) {
          ids.add(e.modId());
        }
      }
      for (PoeCraftDataService.BenchCraft b : bs) {
        if (!ids.contains(b.modId()) && benchCounts(b, t)) {
          ids.add(b.modId());
        }
      }
      out.add(new Target(ids));
    }
    return out;
  }

  /** 화폐 하나(연타 · 적중 확률, 10-22 Q13 — 같은 결함: 에센스 연타가 끝내 못 채움 · 에센스 단추 적중 0%). */
  List<Target> equivalentGoal(Item item, List<Target> targets, String action, String arg) {
    return equivalentGoal(item, targets, List.of(new Step(action, arg, null, null, null)));
  }

  private static void collectArgs(
      List<Step> steps, java.util.Set<String> ess, java.util.Set<String> bench) {
    for (Step st : steps == null ? List.<Step>of() : steps) {
      if ("ESSENCE".equals(st.action()) && st.arg() != null) {
        ess.add(st.arg());
      } else if ("BENCH".equals(st.action()) && st.arg() != null) {
        bench.add(st.arg());
      }
      collectArgs(st.sub(), ess, bench);
    }
  }

  public StrategyResult strategy(StrategyRequest req) {
    if (req.steps() == null || req.steps().isEmpty()) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "단계를 하나 이상 넣으세요");
    }
    if (req.targets() == null
        || req.targets().isEmpty()
        || req.targets().stream()
            .anyMatch(t -> t == null || t.anyOf() == null || t.anyOf().isEmpty())) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "목표 옵션을 하나 이상 고르세요");
    }
    String bad = validate(req.steps());
    if (bad != null) {
      return new StrategyResult(0, 0, 0, Map.of(), null, null, List.of(), "모르는 화폐: " + bad);
    }
    Item start = craft.normalize(req.item());
    // 에센스 · 작업대 옵션도 그 목표로(10-22 Q12 — 계산기와 같은 규칙. 전략은 목표 id 를 그대로 써서 탐욕 에센스가 생명력 T1~T3 에 0% 였다)
    List<Target> goal = equivalentGoal(start, req.targets(), req.steps());
    int runs =
        Math.max(
            500, Math.min(200_000, req.trials() == null ? STRATEGY_DEFAULT_RUNS : req.trials()));
    RandomGenerator rng = new SplittableRandom(42);
    int nSteps = req.steps().size();
    long[] uses = new long[nSteps];
    long[] reached = new long[nSteps];
    Map<String, Double> currency = new LinkedHashMap<>();
    int successes = 0;
    long ops = 0;
    int done = 0;
    Map<String, Sampler> samplers = new java.util.HashMap<>();
    long[] samplerOps = new long[1];
    List<Item> hits = new ArrayList<>();
    for (int i = 0; i < runs && ops < STRATEGY_BUDGET; i++) {
      done++;
      Run run = new Run();
      run.item = start;
      run.samplers = samplers;
      run.samplerOps = samplerOps;
      // 목표를 다 채우면 그 회차는 끝(10-23 Q18 — craftofexile 시뮬레이터의 '끝' 길. 전엔 남은 '한 번' 단계를 그대로 써서 변화 →
      // 제왕 → 엑잘 ×3 이 변화에서 이미 채운 회차에도 제왕 · 엑잘 셋을 셌다 — 생명력 T1~T3 하나에 회차 13.09c)
      for (int s = 0; s < nSteps && !run.aborted && !meets(run.item, goal); s++) {
        reached[s]++;
        uses[s] += step(run, req.steps().get(s), goal, currency, rng);
      }
      if (!run.aborted && meets(run.item, goal)) {
        successes++;
        if (hits.size() < STRATEGY_SAMPLES) {
          hits.add(run.item);
        }
      }
      ops += run.ops;
    }
    boolean cut = done < runs;
    runs = done;
    Map<String, Double> perRun = new LinkedHashMap<>();
    for (Map.Entry<String, Double> e : currency.entrySet()) {
      perRun.put(e.getKey(), e.getValue() / runs);
    }
    Double costPerRun = cost(perRun);
    double rate = (double) successes / runs;
    List<StepStat> stats = new ArrayList<>();
    for (int s = 0; s < nSteps; s++) {
      Step st = req.steps().get(s);
      stats.add(
          new StepStat(st.action(), st.arg(), (double) uses[s] / runs, (double) reached[s] / runs));
    }
    return new StrategyResult(
        runs,
        successes,
        rate,
        perRun,
        costPerRun,
        costPerRun == null || successes == 0 ? null : costPerRun / rate,
        stats,
        successes == 0
            ? "모의 " + runs + "회 중 성공 0 — 단계나 목표를 바꿔 보세요"
            : cut ? "계산량 한도로 " + runs + "회까지만 모의했습니다(오차가 큽니다)" : null,
        hits.stream().map(h -> new StrategySample(h, craft.view(h).lines())).toList());
  }

  /** 모르는 화폐 이름(묶음 안까지) — 없으면 null. 묶음은 action "SEQ". */
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
        PoeCraftEngine.Action.valueOf(st.action());
      } catch (RuntimeException e) {
        return st.action();
      }
    }
    return null;
  }

  /**
   * 단계 하나 진행 — until 이 있으면 그 목표를 채울 때까지(이미 채웠으면 건너뜀) 반복, 없으면 한 번. 쓸 수 없는 상태(오류)면 한 번 단계는 건너뛰고 반복
   * 단계는 멈춘다. 반복 단계가 상한까지 목표를 못 채우면 회차 중단. SEQ = 하위 단계들을 차례로 한 번씩(그 묶음을 반복). 반환 = 사용 수.
   */
  private int step(
      Run run, Step st, List<Target> targets, Map<String, Double> currency, RandomGenerator rng) {
    List<Target> goal = subset(targets, st.until());
    int max =
        st.max() == null
            ? STRATEGY_MAX_STEP
            : Math.max(1, Math.min(STRATEGY_MAX_STEP * 10, st.max()));
    if (goal != null && !meets(run.item, goal)) {
      // 기억 없는 반복: 한 화폐, 또는 [작업대 …, 다시 굴리기] 묶음(작업대는 정해진 결과 — 잠금 메타 + 카오스 등)
      List<Step> seq = "SEQ".equals(st.action()) ? st.sub() : List.of(st);
      if (samplable(seq)) {
        int u = sampled(run, seq, goal, max, currency, rng);
        if (u >= 0) {
          return u;
        }
      }
    }
    int n = 0;
    do {
      if (goal != null && meets(run.item, goal)) {
        break;
      }
      if ("SEQ".equals(st.action())) {
        for (Step sub : st.sub()) {
          step(run, sub, targets, currency, rng);
          if (run.aborted) {
            return n;
          }
        }
      } else {
        PoeCraftEngine.Action act = PoeCraftEngine.Action.valueOf(st.action());
        String arg = st.arg();
        if (act == PoeCraftEngine.Action.UNVEIL && arg == null) {
          arg = unveilChoice(run.item, targets, rng);
        }
        PoeCraftEngine.Result r = craft.engine().apply(run.item, act, arg, rng);
        run.ops++;
        if (r.error() != null) {
          break;
        }
        run.item = r.item();
        // 기폭제 "*20" 은 실제로 쓴 개수만큼(C395 — note "catalyst:종류:양:개수")
        String spentArg = arg;
        if (act == PoeCraftEngine.Action.CATALYST && r.note() != null && arg != null) {
          String base = arg.contains("*") ? arg.substring(0, arg.indexOf('*')) : arg;
          spentArg =
              "vaal".equals(base)
                  ? base
                  : base + "*" + r.note().substring(r.note().lastIndexOf(':') + 1);
        }
        spend(currency, act, spentArg, run.item.itemClass());
      }
      n++;
      if (goal == null) {
        break;
      }
    } while (n < max);
    if (goal != null && !meets(run.item, goal)) {
      run.aborted = true;
    }
    return n;
  }

  /**
   * 표본으로 처리할 수 있는 묶음 — [작업대 …] + 기억 없는 다시 굴리기 + [그 뒤 아무 화폐(묶음 제외)]. 다시 굴리기가 남는 옵션만 남기므로 묶음 한 번의 결과
   * 분포는 그 시점 상태(열쇠)에만 달린다(예: [접두 잠금 → 카오스], [변경 → 확장]). 다시 굴리기 위치, 없으면 -1.
   */
  private int rerollIndex(List<Step> seq) {
    if (seq == null || seq.isEmpty()) {
      return -1;
    }
    for (int i = 0; i < seq.size(); i++) {
      Step x = seq.get(i);
      if ("SEQ".equals(x.action())) {
        return -1;
      }
      PoeCraftEngine.Action a = PoeCraftEngine.Action.valueOf(x.action());
      if (memoryless(a, x.arg(), craft)) {
        for (int k = i + 1; k < seq.size(); k++) {
          if ("SEQ".equals(seq.get(k).action())) {
            return -1;
          }
        }
        return i;
      }
      if (a != PoeCraftEngine.Action.BENCH) {
        return -1;
      }
    }
    return -1;
  }

  private boolean samplable(List<Step> seq) {
    return rerollIndex(seq) >= 0;
  }

  /**
   * 기억 없는 반복 단계 — 표본으로 성공까지의 사용 수를 기하분포로 뽑는다(상한을 넘으면 그 회차 중단). 앞의 작업대가 실패하면(자리 · 그룹) -1 — 그땐 한 번씩
   * 굴리는 길로. 화폐 소모는 표본에서 잰 묶음 한 번 평균 × 사용 수(뒤 단계가 쓸 수 없어 건너뛴 몫은 빠진다 — 예: 확장은 옵션이 하나일 때만).
   */
  private int sampled(
      Run run,
      List<Step> seq,
      List<Target> goal,
      int max,
      Map<String, Double> currency,
      RandomGenerator rng) {
    int k = rerollIndex(seq);
    Item base = run.item;
    StringBuilder key = new StringBuilder();
    Map<String, Double> benchSpend = new LinkedHashMap<>();
    for (int i = 0; i < k; i++) {
      Step b = seq.get(i);
      PoeCraftEngine.Result r =
          craft.engine().apply(base, PoeCraftEngine.Action.BENCH, b.arg(), rng);
      if (r.error() != null) {
        return -1;
      }
      base = r.item();
      spend(benchSpend, PoeCraftEngine.Action.BENCH, b.arg(), base.itemClass());
      key.append("BENCH:").append(b.arg()).append(';');
    }
    Step reroll = seq.get(k);
    PoeCraftEngine.Action act = PoeCraftEngine.Action.valueOf(reroll.action());
    String arg = reroll.arg();
    for (int i = k + 1; i < seq.size(); i++) {
      key.append(seq.get(i).action()).append(':').append(seq.get(i).arg()).append(';');
    }
    java.util.Set<String> targetIds = new java.util.HashSet<>();
    goal.forEach(t -> targetIds.addAll(t.anyOf()));
    String sig = key + signature(act, arg, base, targetIds);
    Sampler sp = run.samplers.get(sig);
    if (sp == null) {
      if (run.samplers.size() >= SAMPLER_KINDS_MAX || run.samplerOps[0] >= SAMPLER_OPS_MAX) {
        return -1; // 표본 한도 — 한 번씩 굴리는 길로(회차 예산 안에서)
      }
      sp = new Sampler();
      int hits = 0;
      int ok = 0;
      Map<String, Double> spent = new LinkedHashMap<>(benchSpend);
      for (int i = 0; i < SAMPLE_MAX && (i < SAMPLE || hits < SAMPLE_HITS); i++) {
        PoeCraftEngine.Result r = craft.engine().apply(base, act, arg, rng);
        run.samplerOps[0]++;
        if (r.error() != null) {
          continue;
        }
        ok++;
        Item it = r.item();
        spend(spent, act, arg, it.itemClass());
        for (int t = k + 1; t < seq.size(); t++) {
          Step x = seq.get(t);
          PoeCraftEngine.Action xa = PoeCraftEngine.Action.valueOf(x.action());
          // 해제는 step() 처럼 목표 먼저(C427 — 표본 경로는 인자 없이 해제해 늘 실패, [장막 카오스 → 해제] 반복이 0% 였다)
          String xArgUse =
              xa == PoeCraftEngine.Action.UNVEIL && x.arg() == null
                  ? unveilChoice(it, goal, rng)
                  : x.arg();
          PoeCraftEngine.Result rx = craft.engine().apply(it, xa, xArgUse, rng);
          run.samplerOps[0]++;
          if (rx.error() == null) {
            it = rx.item();
            // 기폭제 "*20" 은 쓴 개수만큼(C397 검토 — 표본 묶음 경로도 step() 과 같게)
            String xArg = x.arg();
            if (xa == PoeCraftEngine.Action.CATALYST && rx.note() != null && xArg != null) {
              String base2 = xArg.contains("*") ? xArg.substring(0, xArg.indexOf('*')) : xArg;
              xArg =
                  "vaal".equals(base2)
                      ? base2
                      : base2 + "*" + rx.note().substring(rx.note().lastIndexOf(':') + 1);
            }
            spend(spent, xa, xArg, it.itemClass());
          }
        }
        if (meets(it, goal)) {
          hits++;
          if (sp.hits.size() < SAMPLE_KEEP) {
            sp.hits.add(it);
          }
        } else if (sp.misses.size() < SAMPLE_KEEP) {
          sp.misses.add(it);
        }
      }
      sp.p = ok == 0 ? -1 : (double) hits / ok;
      if (ok > 0) {
        final int okN = ok;
        // 작업대 비용은 묶음마다 한 번 — 표본 회수만큼 곱해 평균에 맞춘다
        benchSpend.forEach((name, v) -> spent.merge(name, v * (okN - 1), Double::sum));
        final Sampler fsp = sp;
        spent.forEach((name, v) -> fsp.perUse.put(name, v / okN));
      }
      run.samplers.put(sig, sp);
    }
    if (sp.p < 0) {
      run.aborted = true;
      return 0;
    }
    int uses;
    boolean success;
    if (sp.p <= 0) {
      uses = max;
      success = false;
    } else {
      double u = 1 - rng.nextDouble();
      long g = sp.p >= 1 ? 1 : (long) Math.floor(Math.log(u) / Math.log(1 - sp.p)) + 1;
      success = g <= max;
      uses = (int) Math.min(g, max);
    }
    final int usesF = uses;
    sp.perUse.forEach((name, v) -> currency.merge(name, v * usesF, Double::sum));
    if (success) {
      run.item = sp.hits.get(rng.nextInt(sp.hits.size()));
    } else {
      if (!sp.misses.isEmpty()) {
        run.item = sp.misses.get(rng.nextInt(sp.misses.size()));
      }
      run.aborted = true;
    }
    return uses;
  }

  /** 해제 고르기(C341) — 제시 셋 중 목표에 드는 것, 없으면 무작위(사람이 고르는 것처럼). */
  private String unveilChoice(Item item, List<Target> targets, RandomGenerator rng) {
    List<String> offers = craft.engine().unveilOffers(item);
    if (offers.isEmpty()) {
      return null;
    }
    for (String o : offers) {
      for (Target t : targets) {
        if (t.anyOf() != null && t.anyOf().contains(o)) {
          return o;
        }
      }
    }
    return offers.get(rng.nextInt(offers.size()));
  }

  private static List<Target> subset(List<Target> targets, List<Integer> idx) {
    if (idx == null) {
      return null;
    }
    List<Target> out = new ArrayList<>();
    for (Integer i : idx) {
      if (i != null && i >= 0 && i < targets.size()) {
        out.add(targets.get(i));
      }
    }
    return out;
  }

  /** 화폐 한 번 사용 → 시세 이름별 개수(작업대 · 화석 · 수확 · 정복자 포함). */
  void spend(Map<String, Double> m, PoeCraftEngine.Action act, String arg, String itemClass) {
    spend(m, act, arg, itemClass, 1);
  }

  /** count 번 사용(표본 단계) — 화폐마다 개수 × count. */
  private void spend(
      Map<String, Double> m, PoeCraftEngine.Action act, String arg, String itemClass, int count) {
    if (count <= 0) {
      return;
    }
    Map<String, Double> one = new LinkedHashMap<>();
    spendOnce(one, act, arg, itemClass);
    one.forEach((k, v) -> m.merge(k, v * count, Double::sum));
  }

  /** 에뮬레이터에서 쓴 화폐 집계(C365) — 단계마다 spendOnce, 직접 편집 · 해제 · 각인 복원은 비용 없음. 개수 많은 순이 아니라 처음 쓴 순. */
  public SpentResult spent(SpentRequest req) {
    Map<String, Double> m = new LinkedHashMap<>();
    // 단계별(C455 — 에뮬레이터 단계 기록에 그 단계 비용)
    List<Map<String, Double>> perStep = new ArrayList<>();
    for (SpentStep s : req.steps() == null ? List.<SpentStep>of() : req.steps()) {
      Map<String, Double> sm = stepNames(s, req.itemClass());
      if (s != null && s.times() != null && s.times() > 1) {
        // 목표까지 연타(C506) — 한 단계에 여러 번
        sm.replaceAll((k, v) -> v * s.times());
      }
      perStep.add(sm);
      sm.forEach((k, v) -> m.merge(k, v, Double::sum));
    }
    List<SpentLine> lines = new ArrayList<>();
    double total = 0;
    int missing = 0;
    for (Map.Entry<String, Double> e : m.entrySet()) {
      Double p = prices.chaos(e.getKey());
      Double sub = p == null ? null : p * e.getValue();
      if (sub == null) {
        missing++;
      } else {
        total += sub;
      }
      lines.add(
          new SpentLine(
              e.getKey(), spentKo(req.itemClass()).get(e.getKey()), e.getValue(), p, sub));
    }
    Map<String, String> ko = spentKo(req.itemClass());
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
        Double p = prices.chaos(e.getKey());
        if (p == null) {
          all = false;
        } else {
          sum += p * e.getValue();
        }
      }
      // 시세가 없는 화폐가 끼면 null(비용 모름), 쓴 화폐가 없으면 0(직접 편집 · 각인 복원 등)
      steps.add(
          new StepCost(
              en, kn, sm.isEmpty() ? Double.valueOf(0) : all ? Double.valueOf(sum) : null));
    }
    PoeCraftPriceService.Prices pr = prices.prices();
    return new SpentResult(
        lines, total, missing, prices.chaos("Divine Orb"), pr.league(), pr.fetchedAt(), steps);
  }

  /** 한 단계에서 쓴 화폐 이름 → 개수(C455 — 전에 spent 루프 본문, 합계와 단계별이 같은 규칙). */
  private Map<String, Double> stepNames(SpentStep s, String itemClass) {
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
    if ("BLESSED".equals(s.action())) {
      // 축복의 오브(C375) — 게이트가 고정 속성 수치만 다시 굴린다
      sm.merge("Blessed Orb", 1d, Double::sum);
      return sm;
    }
    PoeCraftEngine.Action act;
    try {
      act = PoeCraftEngine.Action.valueOf(s.action());
    } catch (IllegalArgumentException e) {
      return sm;
    }
    spendOnce(sm, act, s.arg(), itemClass);
    return sm;
  }

  /** 쓴 화폐 한국어 이름표(C380) — 고정 이름표 + 이 종류의 화폐 설명 · 에센스 · 화석 · 공명기 · 작업대 비용. */
  Map<String, String> spentKo(String itemClass) {
    Map<String, String> ko = new java.util.HashMap<>(SPENT_KO);
    if (itemClass == null) {
      return ko;
    }
    PoeCraftService.Options o = craft.options(itemClass, "");
    if (o.currencies() != null) {
      o.currencies().forEach(c -> ko.putIfAbsent(c.name(), c.nameKo()));
    }
    if (o.essences() != null) {
      o.essences().forEach(e -> ko.putIfAbsent(e.name(), e.nameKo()));
    }
    if (o.fossils() != null) {
      o.fossils().forEach(f -> ko.putIfAbsent(f.name(), f.nameKo()));
    }
    if (o.resonators() != null) {
      o.resonators().forEach(r -> ko.putIfAbsent(r.name(), r.nameKo()));
    }
    if (o.bench() != null) {
      o.bench().stream()
          .filter(b -> b.cost() != null)
          .flatMap(b -> b.cost().stream())
          .forEach(c -> ko.putIfAbsent(c.name(), c.nameKo()));
    }
    ko.values().removeIf(java.util.Objects::isNull);
    return ko;
  }

  private void spendOnce(
      Map<String, Double> m, PoeCraftEngine.Action act, String arg, String itemClass) {
    switch (act) {
      // 바알 오브 · 더럽혀진 화폐(C358 · C359)
      case VAAL -> m.merge("Vaal Orb", 1d, Double::sum);
      case TAINTED_CHAOS -> m.merge("Tainted Chaos Orb", 1d, Double::sum);
      case TAINTED_EXALT -> m.merge("Tainted Exalted Orb", 1d, Double::sum);
      case TAINTED_DIVINE -> m.merge("Tainted Divine Teardrop", 1d, Double::sum);
      case TRANSMUTE -> m.merge("Orb of Transmutation", 1d, Double::sum);
      case ALTERATION -> m.merge("Orb of Alteration", 1d, Double::sum);
      case AUGMENTATION -> m.merge("Orb of Augmentation", 1d, Double::sum);
      case REGAL -> m.merge("Regal Orb", 1d, Double::sum);
      case ALCHEMY -> m.merge("Orb of Alchemy", 1d, Double::sum);
      case CHAOS -> m.merge("Chaos Orb", 1d, Double::sum);
      case EXALT -> m.merge("Exalted Orb", 1d, Double::sum);
      case ANNUL -> m.merge("Orb of Annulment", 1d, Double::sum);
      case SCOUR -> m.merge("Orb of Scouring", 1d, Double::sum);
      case FRACTURE -> m.merge("Fracturing Orb", 1d, Double::sum);
      case VEILED_CHAOS -> m.merge("Veiled Chaos Orb", 1d, Double::sum);
      case VEILED_EXALT -> m.merge("Veiled Exalted Orb", 1d, Double::sum);
      // 엘드리치(C343) — 불씨 · 영액은 화폐 등급(arg 1~4) 이름
      case EMBER, ICHOR -> {
        String tier =
            switch (arg == null ? "" : arg.trim()) {
              case "2" -> "Greater";
              case "3" -> "Grand";
              case "4" -> "Exceptional";
              default -> "Lesser";
            };
        m.merge(
            tier + (act == PoeCraftEngine.Action.EMBER ? " Eldritch Ember" : " Eldritch Ichor"),
            1d,
            Double::sum);
      }
      case ELDRITCH_CHAOS -> m.merge("Eldritch Chaos Orb", 1d, Double::sum);
      case ELDRITCH_EXALT -> m.merge("Eldritch Exalted Orb", 1d, Double::sum);
      case ELDRITCH_ANNUL -> m.merge("Eldritch Orb of Annulment", 1d, Double::sum);
      case DOMINANCE -> m.merge("Orb of Dominance", 1d, Double::sum);
      case CONFLICT -> m.merge("Orb of Conflict", 1d, Double::sum);
      case FOULBORN_AUG -> m.merge("Foulborn Orb of Augmentation", 1d, Double::sum);
      case FOULBORN_REGAL -> m.merge("Foulborn Regal Orb", 1d, Double::sum);
      case FOULBORN_EXALT -> m.merge("Foulborn Exalted Orb", 1d, Double::sum);
      case AWAKENER -> m.merge("Awakener's Orb", 1d, Double::sum);
      case ESSENCE ->
          craft.essences(itemClass).stream()
              .filter(e -> e.slug() != null && e.slug().equals(arg))
              .findFirst()
              .ifPresent(e -> m.merge(e.name(), 1d, Double::sum));
      case FOSSIL -> {
        List<String> slugs =
            arg == null
                ? List.of()
                : java.util.Arrays.stream(arg.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .toList();
        for (String s : slugs) {
          craft.fossils().stream()
              .filter(f -> f.slug().equals(s))
              .findFirst()
              .ifPresent(f -> m.merge(f.name(), 1d, Double::sum));
        }
        m.merge(resonatorName(slugs.size()), 1d, Double::sum);
      }
      case HARVEST ->
          craft.harvests().stream()
              .filter(h -> h.id().equals(arg))
              .findFirst()
              .ifPresent(
                  h -> {
                    if (h.lifeforce() != null) {
                      m.merge(
                          h.lifeforce() + " Crystallised Lifeforce",
                          (double) h.cost(),
                          Double::sum);
                    }
                  });
      case CONQUEROR -> m.merge(CONQUEROR_ORB.getOrDefault(arg, "Exalted Orb"), 1d, Double::sum);
      case DUCAT -> {
        // 두카트(C391) — 이름은 화폐 표(action)
        String act2 = DUCAT_ACTION.get(arg == null ? "" : arg);
        PoeCraftService.Options o = craft.options(itemClass, "");
        if (act2 != null && o.currencies() != null) {
          o.currencies().stream()
              .filter(c -> act2.equals(c.action()))
              .findFirst()
              .ifPresent(c -> m.merge(c.name(), 1d, Double::sum));
        }
      }
      // 야수 제작(C387) — 화폐가 아니라 포획한 야수(레시피 표에 재료가 없어 시세 없음, 횟수만)
      case BEAST -> m.merge(BEAST_RECIPE.getOrDefault(arg, "Bestiary craft"), 1d, Double::sum);
      case CATALYST -> {
        // 기폭제(C385) — arg = 종류[*쓴 개수]
        String key = arg == null ? "" : arg;
        double n = 1;
        int star = key.indexOf('*');
        if (star > 0) {
          try {
            n = Math.max(1, Integer.parseInt(key.substring(star + 1)));
          } catch (NumberFormatException e) {
            n = 1;
          }
          key = key.substring(0, star);
        }
        String k = key;
        double cnt = n;
        PoeCraftService.Options o = craft.options(itemClass, "");
        if (o.currencies() != null) {
          o.currencies().stream()
              .filter(c -> k.equals(c.catalyst()))
              .findFirst()
              .ifPresent(c -> m.merge(c.name(), cnt, Double::sum));
        }
      }
      case BENCH -> {
        PoeCraftDataService.BenchCraft b = craft.bench(itemClass, arg);
        if (b != null && b.cost() != null) {
          b.cost().forEach(c -> m.merge(c.name(), (double) c.count(), Double::sum));
        }
      }
      default -> {}
    }
  }

  static final Map<String, String> CONQUEROR_ORB =
      Map.of(
          "crusader", "Crusader's Exalted Orb",
          "eyrie", "Redeemer's Exalted Orb",
          "basilisk", "Hunter's Exalted Orb",
          "adjudicator", "Warlord's Exalted Orb",
          "shaper", "Shaper's Exalted Orb",
          "elder", "Elder's Exalted Orb");

  /** 두카트 인자 → 화폐 동작(C391, CurrencyItems.Action) — 이름 · 한국어는 화폐 표에서. */
  static final Map<String, String> DUCAT_ACTION =
      Map.of(
          "genteel", "reroll_single_attribute_modifier",
          "rotmother", "add_deepwater_hazard_belt_mod",
          "brinehook", "add_pantheon_aspect",
          "merrick", "add_mod_and_corrupt_rare_abyss_jewel",
          "cyaxan", "reroll_rare_infamous",
          "ukatoa", "add_eldritch_implicit_amulet");

  /** 목표 옵션들의 태그 합집합(화석 · 수확 후보 고르기). */
  java.util.Set<String> targetTags(List<Target> targets) {
    java.util.Set<String> out = new java.util.HashSet<>();
    for (Target t : targets) {
      for (String id : t.anyOf()) {
        PoeCraftDataService.CraftMod m = craft.mod(id);
        if (m != null && m.t() != null) {
          out.addAll(m.t());
        }
      }
    }
    return out;
  }

  /**
   * 출발점(10-15 C349) — 분열 옵션 · 고정 속성은 남기고 나머지는 비운다(지금 아이템에서 시작 — 분열 베이스 등). 분열 옵션이 있으면 정제해도 일반이 되지
   * 않으니 레어, 없으면 일반.
   */
  static Item normal(Item item) {
    List<PoeCraftEngine.Affix> fr = fractured(item);
    return new Item(
        item.itemClass(),
        item.variant(),
        item.influence(),
        item.ilvl(),
        fr.isEmpty() ? Rarity.NORMAL : Rarity.RARE,
        fr,
        false,
        item.implicits());
  }

  private static List<PoeCraftEngine.Affix> fractured(Item item) {
    return item.affixes().stream().filter(PoeCraftEngine.Affix::fractured).toList();
  }

  /** 출발점이 일반이면 연금 한 번(비용 밖). */
  private static Item rareStart(PoeCraftEngine engine, Item item, RandomGenerator rng) {
    Item b = normal(item);
    return b.rarity() == Rarity.NORMAL ? engine.apply(b, Action.ALCHEMY, null, rng).item() : b;
  }

  private int simulate(
      java.util.function.Function<RandomGenerator, Item> attempt,
      List<Target> targets,
      int trials,
      RandomGenerator rng) {
    int hits = 0;
    for (int i = 0; i < trials; i++) {
      Item r = attempt.apply(rng);
      if (r != null && meets(r, targets)) {
        hits++;
      }
    }
    return hits;
  }

  private MethodResult result(
      String method,
      String label,
      String labelEn,
      int trials,
      int hits,
      Map<String, Double> per,
      String note,
      String noteEn) {
    double chance = (double) hits / trials;
    Double attempts = hits == 0 ? null : 1 / chance;
    Double cost = cost(per);
    return new MethodResult(
        method,
        label,
        trials,
        hits,
        chance,
        attempts,
        per,
        cost,
        cost == null || attempts == null ? null : cost * attempts,
        hits == 0 ? neverHit(note, trials, false) : note,
        labelEn,
        hits == 0 ? neverHit(noteEn, trials, true) : noteEn);
  }

  /**
   * 화석 자동 조합(C339) — 목표 태그를 올리거나 상관없는 태그를 막는 화석(태그 배수만 있는 것)을 하나씩 재고, 상위 6개로 2~4개 조합을 견줘 가장 나은 둘을
   * 낸다. 시도 한 번 = 혼돈 공명기(칸 수) + 화석들.
   */
  List<MethodResult> fossils(Item item, List<Target> targets, int trials) {
    java.util.Set<String> tags = targetTags(targets);
    List<PoeCraftDataService.Fossil> cands = new ArrayList<>();
    for (PoeCraftDataService.Fossil f : craft.fossils()) {
      if (!f.simple()) {
        continue;
      }
      boolean boosts =
          f.weights().entrySet().stream()
              .anyMatch(e -> e.getValue() > 1 && tags.contains(e.getKey()));
      boolean blocksOther =
          f.weights().entrySet().stream()
              .anyMatch(e -> e.getValue() < 1 && !tags.contains(e.getKey()));
      boolean blocksMine =
          f.weights().entrySet().stream()
              .anyMatch(e -> e.getValue() == 0 && tags.contains(e.getKey()));
      if ((boosts || blocksOther) && !blocksMine) {
        cands.add(f);
      }
    }
    Item start = normal(item);
    PoeCraftEngine engine = craft.engine();
    // 탐광(화석 전용) 옵션 목표(10-21 C549 사용자 요청 "탐광 같은 옵션도 목표로") — 그 옵션을 실제로 붙일 수 있게 하는 화석(더하기 · 강제 —
    // fossilEligible 에 그 옵션이 있는 것)은 태그와 무관하게 후보, 조합에 반드시 넣는다(전엔 태그 배수 화석만 봐 '못 닿음')
    java.util.Set<String> want = new java.util.HashSet<>();
    for (Target t : real(targets)) {
      want.addAll(t.anyOf());
    }
    want.removeAll(engine.eligible(start, Rarity.RARE).keySet());
    List<PoeCraftDataService.Fossil> enabling = new ArrayList<>();
    if (!want.isEmpty()) {
      for (PoeCraftDataService.Fossil f : craft.fossils()) {
        if (engine.fossilEligible(start, f.slug()).keySet().stream().anyMatch(want::contains)) {
          enabling.add(f);
          if (!cands.contains(f)) {
            cands.add(f);
          }
        }
      }
    }
    if (cands.isEmpty()) {
      return List.of();
    }
    // 예비 측정은 짧게(10-15 실측: 계산기 전체 37초 — 화면이 기다리기 어렵다)
    int probe = Math.max(1_500, trials / 40);
    Map<String, Integer> score = new java.util.HashMap<>();
    for (PoeCraftDataService.Fossil f : cands) {
      score.put(
          f.slug(),
          simulate(
              r -> engine.apply(start, Action.FOSSIL, f.slug(), r).item(),
              targets,
              probe,
              new SplittableRandom(f.slug().hashCode())));
    }
    // 여는 화석이 먼저(최대 다섯), 나머지 칸은 점수 순
    List<PoeCraftDataService.Fossil> top =
        new ArrayList<>(enabling.subList(0, Math.min(5, enabling.size())));
    cands.stream()
        .filter(f -> !enabling.contains(f))
        .sorted(Comparator.comparing((PoeCraftDataService.Fossil f) -> -score.get(f.slug())))
        .limit(Math.max(0, 5 - top.size()))
        .forEach(top::add);
    int must = Math.min(5, enabling.size());
    // 조합 — 1개짜리도 후보(공명기 1칸)
    List<List<PoeCraftDataService.Fossil>> combos = new ArrayList<>();
    int n = top.size();
    for (int mask = 1; mask < (1 << n); mask++) {
      if (Integer.bitCount(mask) > 4) {
        continue;
      }
      // 탐광 목표면 여는 화석 하나는 꼭(없으면 그 옵션이 안 붙는다)
      if (must > 0 && (mask & ((1 << must) - 1)) == 0) {
        continue;
      }
      List<PoeCraftDataService.Fossil> c = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) {
          c.add(top.get(i));
        }
      }
      combos.add(c);
    }
    // 조합마다 짧게 재고(비용 고려 — 기대 비용 = 시도당 비용 / 확률) 상위 둘을 다시 길게
    record Scored(List<PoeCraftDataService.Fossil> combo, double expected) {}
    List<Scored> scored = new ArrayList<>();
    for (List<PoeCraftDataService.Fossil> c : combos) {
      String slugs = String.join(",", c.stream().map(PoeCraftDataService.Fossil::slug).toList());
      int h =
          simulate(
              r -> engine.apply(start, Action.FOSSIL, slugs, r).item(),
              targets,
              probe,
              new SplittableRandom(slugs.hashCode()));
      Double per = cost(fossilCost(c));
      double exp = h == 0 ? Double.MAX_VALUE : (per == null ? 1 : per) * probe / h;
      scored.add(new Scored(c, exp));
    }
    scored.sort(Comparator.comparingDouble(Scored::expected));
    // 축성된 화석 더하기(C404) — 상위 셋(칸이 남는 것)에 축성된을 더한 조합도 잰다(고레벨 가중 — 상위 등급 목표면 칸 하나 값이 날 수 있다)
    PoeCraftDataService.Fossil sanct =
        craft.fossils().stream().filter(PoeCraftDataService.Fossil::lucky).findFirst().orElse(null);
    if (sanct != null) {
      List<Scored> extra = new ArrayList<>();
      for (Scored sc : scored.subList(0, Math.min(3, scored.size()))) {
        if (sc.combo().size() >= 4 || sc.expected() == Double.MAX_VALUE) {
          continue;
        }
        List<PoeCraftDataService.Fossil> c = new ArrayList<>(sc.combo());
        c.add(sanct);
        String slugs = String.join(",", c.stream().map(PoeCraftDataService.Fossil::slug).toList());
        int h =
            simulate(
                r -> engine.apply(start, Action.FOSSIL, slugs, r).item(),
                targets,
                probe,
                new SplittableRandom(slugs.hashCode()));
        Double per = cost(fossilCost(c));
        extra.add(new Scored(c, h == 0 ? Double.MAX_VALUE : (per == null ? 1 : per) * probe / h));
      }
      scored.addAll(extra);
      scored.sort(Comparator.comparingDouble(Scored::expected));
    }
    List<MethodResult> out = new ArrayList<>();
    for (Scored s : scored.subList(0, Math.min(2, scored.size()))) {
      if (s.expected() == Double.MAX_VALUE) {
        continue;
      }
      String slugs =
          String.join(",", s.combo().stream().map(PoeCraftDataService.Fossil::slug).toList());
      int h =
          simulate(
              r -> engine.apply(start, Action.FOSSIL, slugs, r).item(),
              targets,
              trials,
              new SplittableRandom(31L * slugs.hashCode()));
      String names =
          String.join(" + ", s.combo().stream().map(PoeCraftDataService.Fossil::nameKo).toList());
      String namesEn =
          String.join(" + ", s.combo().stream().map(PoeCraftDataService.Fossil::name).toList());
      out.add(
          result(
              "FOSSIL:" + slugs,
              // 공명기 이름은 한국어(C423 UX QA — 화면 결과에 '(Primitive Chaotic Resonator)' 가 섞였다), 비용 키는 영어
              // 그대로
              names + " (" + resonatorNameKo(s.combo().size()) + ")",
              namesEn + " (" + resonatorNameEn(s.combo().size()) + ")",
              trials,
              h,
              fossilCost(s.combo()),
              "화석 " + s.combo().size() + "개 · 태그 배수 곱",
              s.combo().size()
                  + (s.combo().size() == 1 ? " fossil" : " fossils")
                  + " · tag multipliers combined"));
    }
    return out;
  }

  /** 혼돈 공명기 영어 이름(칸 수) — 영어 화면 결과 표 라벨(C557). */
  private String resonatorNameEn(int sockets) {
    return craft.resonators().stream()
        .filter(r -> "chaotic".equals(r.kind()) && r.sockets() == sockets)
        .map(PoeCraftDataService.Resonator::name)
        .findFirst()
        .orElse("Chaotic Resonator");
  }

  /** 혼돈 공명기 한국어 이름(칸 수) — 결과 표 라벨(C423). */
  private String resonatorNameKo(int sockets) {
    return craft.resonators().stream()
        .filter(r -> "chaotic".equals(r.kind()) && r.sockets() == sockets)
        .map(r -> r.nameKo() != null && !r.nameKo().isBlank() ? r.nameKo() : r.name())
        .findFirst()
        .orElse("혼돈 공명기");
  }

  /** 혼돈 공명기 이름(칸 수) — 시세 이름. */
  private String resonatorName(int sockets) {
    return craft.resonators().stream()
        .filter(r -> "chaotic".equals(r.kind()) && r.sockets() == sockets)
        .map(PoeCraftDataService.Resonator::name)
        .findFirst()
        .orElse("Chaotic Resonator");
  }

  private Map<String, Double> fossilCost(List<PoeCraftDataService.Fossil> combo) {
    Map<String, Double> per = new LinkedHashMap<>();
    combo.forEach(f -> per.put(f.name(), 1.0));
    per.put(resonatorName(combo.size()), 1.0);
    return per;
  }

  /** 수확 태그 재제작(C339) — 목표 태그와 겹치는 reroll_with_mod 마다, 레어에서 시도 한 번 = 생명력(종류) × 소모량. 가장 나은 둘. */
  List<MethodResult> harvests(Item item, List<Target> targets, int trials) {
    java.util.Set<String> tags = targetTags(targets);
    PoeCraftEngine engine = craft.engine();
    Item rare = rareStart(engine, item, new SplittableRandom(7));
    List<MethodResult> all = new ArrayList<>();
    for (PoeCraftDataService.Harvest h : craft.harvests()) {
      if (!"reroll_with_mod".equals(h.command())) {
        continue;
      }
      String tag = h.params() == null ? "" : h.params().split("\\s+")[0];
      if (!tags.contains(tag)) {
        continue;
      }
      int hits =
          simulate(
              r -> engine.apply(rare, Action.HARVEST, h.id(), r).item(),
              targets,
              trials,
              new SplittableRandom(h.id().hashCode()));
      Map<String, Double> per = new LinkedHashMap<>();
      if (h.lifeforce() != null) {
        per.put(h.lifeforce() + " Crystallised Lifeforce", (double) h.cost());
      }
      all.add(
          result(
              "HARVEST:" + h.id(),
              "수확: " + (h.textKo() != null ? h.textKo() : h.text()),
              "Harvest: " + h.text(),
              trials,
              hits,
              per,
              "레어에서 시작(연금 한 번 비용 밖)",
              "Starts from a rare (one Alchemy not counted)"));
    }
    all.sort(
        Comparator.comparing(
            (MethodResult r) -> r.expectedCost() == null ? Double.MAX_VALUE : r.expectedCost()));
    return all.subList(0, Math.min(2, all.size()));
  }

  /** 장막 해제 목표(C421) — 목표에 해제 옵션이 있으면 장막의 카오스 오브(장막 하나 보장) → 해제 셋에서 목표 먼저(없으면 무작위). */
  private List<MethodResult> veiledChaos(Item item, List<Target> targets, int trials) {
    java.util.Set<String> vids = craft.veiledIds(item);
    if (targets.stream().noneMatch(t -> t.anyOf().stream().anyMatch(vids::contains))) {
      return List.of();
    }
    PoeCraftEngine engine = craft.engine();
    Item base = normal(item);
    int hits =
        simulate(
            r -> {
              Item it =
                  engine.apply(rareStart(engine, base, r), Action.VEILED_CHAOS, null, r).item();
              String o = unveilChoice(it, targets, r);
              return o == null ? it : engine.apply(it, Action.UNVEIL, o, r).item();
            },
            targets,
            trials,
            new SplittableRandom(45));
    return List.of(
        result(
            "VEILED",
            "장막의 카오스 오브 → 해제(목표 먼저)",
            "Veiled Chaos Orb → unveil (targets first)",
            trials,
            hits,
            Map.of("Veiled Chaos Orb", 1.0),
            "해제(쥰 작업대)는 비용 없음 · 처음 레어는 비용 밖",
            "Unveiling (Jun's bench) is free · the starting rare is not counted"));
  }

  /**
   * 섬뜩한 카오스 연타(10-15 C343) — 목표가 모두 접두(또는 모두 접미)일 때만. 그쪽이 우세하도록 불씨 · 영액을 한 번(상급 + 하급, 비용 밖) 쓴 레어에서
   * 섬뜩한 카오스로 그쪽만 다시 굴린다. 반대쪽은 시도마다 연금으로 새로 뽑는 근사(실제로는 한 번 정해지면 그대로 — 반대쪽 옵션 수에 따라 확률이 달라진다).
   */
  private List<MethodResult> eldritchChaos(Item item, List<Target> targets, int trials) {
    if (!PoeCraftEngine.ELDRITCH_CLASSES.contains(item.itemClass())
        || (item.influence() != null && !item.influence().isEmpty())) {
      return List.of();
    }
    java.util.Set<String> sides = new java.util.HashSet<>();
    for (Target t : targets) {
      for (String id : t.anyOf()) {
        PoeCraftDataService.CraftMod m = craft.mod(id);
        if (m != null) {
          sides.add(m.g());
        }
      }
    }
    if (sides.size() != 1) {
      return List.of();
    }
    boolean pre = sides.contains("prefix");
    PoeCraftEngine engine = craft.engine();
    SplittableRandom setup = new SplittableRandom(43);
    Item base = normal(item);
    PoeCraftEngine.Result a = engine.apply(base, pre ? Action.EMBER : Action.ICHOR, "2", setup);
    PoeCraftEngine.Result b =
        a.error() == null
            ? engine.apply(a.item(), pre ? Action.ICHOR : Action.EMBER, "1", setup)
            : a;
    if (b.error() != null) {
      return List.of();
    }
    Item withImp = b.item();
    int hits =
        simulate(
            r -> engine.apply(rareStart(engine, withImp, r), Action.ELDRITCH_CHAOS, null, r).item(),
            targets,
            trials,
            new SplittableRandom(44));
    return List.of(
        result(
            "ELDRITCH",
            "섬뜩한 카오스 오브 연타(" + (pre ? "총주교 우세 · 접두" : "포식자 우세 · 접미") + ")",
            "Eldritch Chaos Orb spam ("
                + (pre ? "Exarch dominant · prefixes" : "Eater dominant · suffixes")
                + ")",
            trials,
            hits,
            Map.of("Eldritch Chaos Orb", 1.0),
            "불씨 · 영액 한 번(상급 + 하급)은 비용 밖 · 반대쪽은 시도마다 새로(근사)",
            "One Greater + Lesser Ember/Ichor not counted · the other side is rerolled each try"
                + " (approximation)"));
  }

  /** 목표를 채울 수 있는 에센스(C339) — 계열마다 쓸 수 있는 가장 높은 등급, 사용자가 고른 것 빼고 가장 나은 하나. */
  private List<MethodResult> autoEssences(
      Item item, List<Target> targets, String chosen, int trials) {
    Map<String, PoeCraftDataService.Essence> best = new LinkedHashMap<>();
    for (PoeCraftDataService.Essence e : craft.essences(item.itemClass())) {
      if (e.modId() == null
          || e.slug() == null
          || e.slug().equals(chosen)
          || (e.ilvlMax() > 0 && item.ilvl() > e.ilvlMax())) {
        continue;
      }
      if (targets.stream().noneMatch(t -> essenceCounts(e, t))) {
        continue;
      }
      PoeCraftDataService.Essence cur = best.get(e.family());
      if (cur == null || e.tier() > cur.tier()) {
        best.put(e.family(), e);
      }
    }
    List<MethodResult> all = new ArrayList<>();
    for (PoeCraftDataService.Essence e : best.values()) {
      MethodResult r =
          run(
              "ESSENCE",
              item,
              targets,
              e.slug(),
              trials,
              new SplittableRandom(e.slug().hashCode()));
      if (r != null) {
        all.add(
            new MethodResult(
                "ESSENCE:" + e.slug(),
                r.label(),
                r.trials(),
                r.hits(),
                r.chance(),
                r.expectedAttempts(),
                r.perAttempt(),
                r.costPerAttempt(),
                r.expectedCost(),
                r.note(),
                r.labelEn(),
                r.noteEn()));
      }
    }
    all.sort(
        Comparator.comparing(
            (MethodResult r) -> r.expectedCost() == null ? Double.MAX_VALUE : r.expectedCost()));
    return all.subList(0, Math.min(1, all.size()));
  }

  /** 서로 다른 옵션으로 채울 수 있는 목표 수의 최댓값(C456 — 조합의 출발 조건 · 남은 목표). 빈 자리 조건(C519)은 지금 남은 자리만큼. */
  static int matched(Item item, List<Target> targets) {
    List<String> ids = item.affixes().stream().map(PoeCraftEngine.Affix::id).toList();
    List<Target> opt = real(targets);
    int[] need = openNeed(targets);
    int[] free = openFree(item);
    return most(ids, opt, 0, new boolean[ids.size()])
        + Math.min(need[0], free[0])
        + Math.min(need[1], free[1]);
  }

  /** 빈 자리 조건(10-20 C519, craftofexile 계산기 'open affix') — 목표 하나 = 그 쪽 빈 자리 하나(작업대 옵션도 자리를 차지). */
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

  /** 지금 빈 자리 [접두, 접미] — 희귀도 한도(매직 1 · 레어 3 · 주얼 2) − 그 쪽 옵션 수. */
  static int[] openFree(Item item) {
    int cap = PoeCraftEngine.maxPerGen(item, item.rarity());
    int p = 0;
    int s = 0;
    for (PoeCraftEngine.Affix a : item.affixes()) {
      if ("prefix".equals(a.gen())) {
        p++;
      } else if ("suffix".equals(a.gen())) {
        s++;
      }
    }
    return new int[] {Math.max(0, cap - p), Math.max(0, cap - s)};
  }

  private static int most(List<String> ids, List<Target> targets, int t, boolean[] used) {
    if (t == targets.size()) {
      return 0;
    }
    int best = most(ids, targets, t + 1, used);
    List<String> any = targets.get(t).anyOf();
    for (int i = 0; i < ids.size() && best < targets.size() - t; i++) {
      if (!used[i] && any != null && any.contains(ids.get(i))) {
        used[i] = true;
        best = Math.max(best, 1 + most(ids, targets, t + 1, used));
        used[i] = false;
      }
    }
    return best;
  }

  /**
   * 작업대 옵션이 목표를 채우는가(C456, essenceCounts 와 같은 규칙) — 같은 옵션 그룹 · 같은 문구 모양이고 최소 굴림이 목표 최하 등급의 최소 굴림
   * 이상.
   */
  boolean benchCounts(PoeCraftDataService.BenchCraft b, Target t) {
    if (isOpen(t) || b.groups() == null || b.groups().isEmpty()) {
      return false;
    }
    boolean same = false;
    double floor = Double.MAX_VALUE;
    String bShape = shape(b.enMin());
    for (String id : t.anyOf()) {
      PoeCraftDataService.CraftMod m = craft.mod(id);
      if (m != null
          && m.grp().stream().anyMatch(b.groups()::contains)
          && bShape != null
          && bShape.equals(shape(craft.minText(id)))) {
        same = true;
      }
      Double v = firstNumber(craft.minText(id));
      if (v != null) {
        floor = Math.min(floor, v);
      }
    }
    Double bv = firstNumber(b.enMin());
    return same && bv != null && floor != Double.MAX_VALUE && bv >= floor;
  }

  private static List<String> concat(List<String> a, String b) {
    List<String> out = new ArrayList<>(a);
    out.add(b);
    return out;
  }

  /**
   * 계산기 목표 칸 덧붙임(10-21 C548 사용자 요청 "제작 계산기에 타락 · 정복자 · 제작 옵션이 안 보인다") — bench = 계열마다 그 목표를 채우는 작업대
   * 옵션 중 가장 높은 값어치(tier = 그 작업대 옵션이 채우는 가장 높은 등급 — 목표를 T1~T그 등급 이하로 고르면 작업대로도 됨, benchCounts 규칙),
   * corrupted = 바알 오브 타락 고정 속성(chance = 바알 한 번에 그 옵션이 붙을 확률 — 결과 넷 중 '고정 속성' × 가중 몫, 계산기 목표는 아님),
   * influenceIds = 고른 영향력 때문에 풀에 들어온 옵션 id(영향력 없으면 빈 목록).
   */
  public record CalcExtras(
      List<BenchEquiv> bench,
      List<CorruptedLine> corrupted,
      List<String> influenceIds,
      // 탐광(화석 전용) 옵션(C549) — 그 옵션을 붙일 수 있게 하는 화석 이름(영어 · 한국어)
      List<DelveLine> delve,
      // 섬뜩한 고정 속성(C549) — 쪽 · 계열마다 등급 1~6, 못 쓰면 eldritchBlocked 에 사유
      List<EldritchFamily> eldritch,
      String eldritchBlocked,
      // eldritchBlocked 의 영어 판(C557)
      String eldritchBlockedEn,
      // 에센스 전용 옵션(C551) — 자연 풀에 같은 옵션(같은 그룹 · 문구 모양)이 없는 에센스 보장 옵션, 에센스 가문마다 등급
      List<EssenceOnly> essenceOnly) {}

  /** 에센스 전용 옵션 — 가문(Fear · Insanity …)마다 이 아이템 종류에 붙는 보장 옵션, tiers = 에센스 등급 오름차순. */
  public record EssenceOnly(
      String family,
      String gen,
      String icon,
      List<String> tags,
      List<String> tagsKo,
      List<EssenceTier> tiers) {}

  public record EssenceTier(
      int tier, String modId, String name, String nameKo, List<String> en, List<String> ko) {}

  /**
   * 섬뜩한 고정 속성 계열 — side = exarch|eater, tiers = 등급 1~6(chance = 그 등급 화폐 한 개로 이 계열이 나올 확률, 5~6 은 0).
   */
  public record EldritchFamily(
      String side, String key, List<String> tags, List<String> tagsKo, List<EldritchTier> tiers) {}

  public record EldritchTier(
      int tier,
      String id,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      double chance) {}

  public record DelveLine(
      String id,
      String gen,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      List<String> tags,
      List<String> tagsKo,
      int level,
      List<String> fossils,
      List<String> fossilsKo) {}

  public record BenchEquiv(
      String gen,
      String family,
      String modId,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      int tier) {}

  public record CorruptedLine(
      String id,
      List<String> en,
      List<String> enMin,
      List<String> ko,
      List<String> koMin,
      List<String> tags,
      List<String> tagsKo,
      int level,
      double chance) {}

  /** 계열(등급 순)을 채우는 가장 높은 등급 상당의 작업대 옵션 — 가장 높은 등급부터 T1~Tr 목표를 채우는 가장 작은 r, 없으면 null. */
  private BenchEquiv benchFor(
      List<PoeCraftService.PoolLine> tiers, List<PoeCraftDataService.BenchCraft> benches) {
    BenchEquiv best = null;
    for (PoeCraftDataService.BenchCraft b : benches) {
      if (b.modId() == null || !b.gen().equals(tiers.get(0).gen())) {
        continue;
      }
      List<String> ids = new ArrayList<>();
      for (PoeCraftService.PoolLine t : tiers) {
        ids.add(t.id());
        if (benchCounts(b, new Target(List.copyOf(ids)))) {
          if (best == null || t.tierRank() < best.tier()) {
            best =
                new BenchEquiv(
                    t.gen(),
                    t.family(),
                    b.modId(),
                    b.en(),
                    b.enMin(),
                    b.ko(),
                    b.koMin(),
                    t.tierRank());
          }
          break;
        }
      }
    }
    return best;
  }

  /**
   * 같은 옵션을 얻는 다른 길(10-22 P4, poedb 속성 부여의 '같은 문구 다른 출처') — 계열 하나(접두/접미:계열)에 대해 보장 에센스(에센스 가문마다 가장
   * 높은 등급 상당 — 계산기와 같은 essenceCounts), 작업대(가장 높은 등급 상당), 화석 배수(그 계열 태그에 걸리는 배수 ≠ 1 — 늘림 · 막음), 수확
   * '그 태그 포함 재련'. 등급(rank) = 그 수단이 채우는 자연 등급(T1~Tr).
   */
  public record FamilyWays(
      List<EssenceWay> essences,
      BenchEquiv bench,
      List<FossilWay> fossils,
      List<HarvestWay> harvests) {}

  public record EssenceWay(
      String slug, String name, String nameKo, String icon, int level, int rank) {}

  public record FossilWay(String slug, String name, String nameKo, String icon, double mult) {}

  public record HarvestWay(String id, String text, String textKo) {}

  /**
   * 에센스가 확정으로 붙이는 옵션(10-22 Q7) — 그 옵션이 채우는 자연 계열과 등급 상당(essenceCounts, familyWays 와 같은 규칙). 자연 계열이
   * 없는 에센스 전용 옵션이면 family = null. 모르는 에센스면 빈 목록.
   */
  public List<PoeCraftService.Forced> essenceForced(Item item, String slug) {
    Item n = normal(craft.normalize(item));
    PoeCraftService.Options o =
        craft.options(n.itemClass(), n.variant() == null ? "" : n.variant());
    PoeCraftDataService.Essence e =
        (o.essences() == null ? List.<PoeCraftDataService.Essence>of() : o.essences())
            .stream().filter(x -> x.slug().equals(slug)).findFirst().orElse(null);
    if (e == null) {
      return List.of();
    }
    Map<String, List<PoeCraftService.PoolLine>> fams = new LinkedHashMap<>();
    for (PoeCraftService.PoolLine p : craft.pool(n, Rarity.RARE)) {
      if (p.tierRank() != null && p.family() != null && p.gen().equals(e.gen())) {
        fams.computeIfAbsent(p.gen() + ":" + p.family(), k -> new ArrayList<>()).add(p);
      }
    }
    for (Map.Entry<String, List<PoeCraftService.PoolLine>> f : fams.entrySet()) {
      List<PoeCraftService.PoolLine> tiers = new ArrayList<>(f.getValue());
      tiers.sort(Comparator.comparing(PoeCraftService.PoolLine::tierRank));
      List<String> ids = new ArrayList<>();
      for (PoeCraftService.PoolLine t : tiers) {
        ids.add(t.id());
        if (essenceCounts(e, new Target(List.copyOf(ids)))) {
          return List.of(
              new PoeCraftService.Forced(
                  f.getKey(), t.tierRank(), e.en(), e.enMin(), e.ko(), e.koMin()));
        }
      }
    }
    return List.of(new PoeCraftService.Forced(null, null, e.en(), e.enMin(), e.ko(), e.koMin()));
  }

  public FamilyWays familyWays(Item item, String key) {
    Item n = normal(craft.normalize(item));
    int c = key == null ? -1 : key.indexOf(':');
    if (c < 0) {
      return new FamilyWays(List.of(), null, List.of(), List.of());
    }
    String gen = key.substring(0, c);
    String fam = key.substring(c + 1);
    List<PoeCraftService.PoolLine> tiers =
        craft.pool(n, Rarity.RARE).stream()
            .filter(p -> gen.equals(p.gen()) && fam.equals(p.family()) && p.tierRank() != null)
            .sorted(Comparator.comparing(PoeCraftService.PoolLine::tierRank))
            .toList();
    if (tiers.isEmpty()) {
      return new FamilyWays(List.of(), null, List.of(), List.of());
    }
    PoeCraftService.Options o =
        craft.options(n.itemClass(), n.variant() == null ? "" : n.variant());
    Map<String, EssenceWay> best = new LinkedHashMap<>();
    for (PoeCraftDataService.Essence e :
        o.essences() == null ? List.<PoeCraftDataService.Essence>of() : o.essences()) {
      if (!gen.equals(e.gen())) {
        continue;
      }
      List<String> ids = new ArrayList<>();
      for (PoeCraftService.PoolLine t : tiers) {
        ids.add(t.id());
        if (essenceCounts(e, new Target(List.copyOf(ids)))) {
          EssenceWay cur = best.get(e.family());
          if (cur == null
              || t.tierRank() < cur.rank()
              || (t.tierRank() == cur.rank() && e.tier() < cur.level())) {
            best.put(
                e.family(),
                new EssenceWay(e.slug(), e.name(), e.nameKo(), e.icon(), e.tier(), t.tierRank()));
          }
          break;
        }
      }
    }
    List<EssenceWay> essences = new ArrayList<>(best.values());
    essences.sort(Comparator.comparingInt(EssenceWay::rank).thenComparingInt(x -> -x.level()));
    PoeCraftDataService.CraftMod top = craft.mod(tiers.get(0).id());
    List<String> tags = top == null || top.t() == null ? List.of() : top.t();
    List<FossilWay> fossils = new ArrayList<>();
    for (PoeCraftDataService.Fossil f :
        o.fossils() == null ? List.<PoeCraftDataService.Fossil>of() : o.fossils()) {
      double m = 1;
      boolean hit = false;
      for (String tg : tags) {
        Double x = f.weights() == null ? null : f.weights().get(tg);
        if (x != null) {
          m *= x;
          hit = true;
        }
      }
      if (hit && m != 1) {
        fossils.add(new FossilWay(f.slug(), f.name(), f.nameKo(), f.icon(), m));
      }
    }
    // 늘림(큰 배수 먼저) 다음 막음
    fossils.sort(Comparator.comparingDouble((FossilWay x) -> -x.mult()));
    List<HarvestWay> harvests = new ArrayList<>();
    for (PoeCraftDataService.Harvest h :
        o.harvest() == null ? List.<PoeCraftDataService.Harvest>of() : o.harvest()) {
      if (!"reroll_with_mod".equals(h.command()) || h.params() == null) {
        continue;
      }
      String tag = h.params().trim().split("\\s+")[0];
      if (tags.contains(tag)) {
        harvests.add(new HarvestWay(h.id(), h.text(), h.textKo()));
      }
    }
    BenchEquiv bench =
        benchFor(tiers, o.bench() == null ? List.<PoeCraftDataService.BenchCraft>of() : o.bench());
    return new FamilyWays(essences, bench, fossils, harvests);
  }

  public CalcExtras extras(Item item) {
    Item n = normal(craft.normalize(item));
    List<PoeCraftService.PoolLine> pool = craft.pool(n, Rarity.RARE);
    // 계열 → 등급 순 id
    Map<String, List<PoeCraftService.PoolLine>> fams = new LinkedHashMap<>();
    for (PoeCraftService.PoolLine p : pool) {
      if (p.tierRank() != null && p.family() != null) {
        fams.computeIfAbsent(p.gen() + ":" + p.family(), k -> new ArrayList<>()).add(p);
      }
    }
    PoeCraftService.Options o =
        craft.options(n.itemClass(), n.variant() == null ? "" : n.variant());
    List<PoeCraftDataService.BenchCraft> benches = o.bench() == null ? List.of() : o.bench();
    List<BenchEquiv> bench = new ArrayList<>();
    for (Map.Entry<String, List<PoeCraftService.PoolLine>> e : fams.entrySet()) {
      List<PoeCraftService.PoolLine> tiers = new ArrayList<>(e.getValue());
      tiers.sort(Comparator.comparing(PoeCraftService.PoolLine::tierRank));
      BenchEquiv best = benchFor(tiers, benches);
      if (best != null) {
        bench.add(best);
      }
    }
    // 바알 오브 타락 고정 속성 — 엔진 vaal() 과 같은 거르기(요구 레벨 ≤ 아이템 레벨 · 가중 > 0)
    List<CorruptedLine> corrupted = new ArrayList<>();
    Map<String, Integer> cp = craft.corruptedPool(n.poolKey());
    double total = 0;
    for (Map.Entry<String, Integer> c : cp.entrySet()) {
      PoeCraftDataService.VeiledMod m = craft.corruptedMod(c.getKey());
      if (m != null && m.l() <= n.ilvl() && c.getValue() > 0) {
        total += c.getValue();
      }
    }
    double implicitShare = 1.0 / PoeCraftEngine.VAAL_OUTCOMES.size();
    for (Map.Entry<String, Integer> c : cp.entrySet()) {
      PoeCraftDataService.VeiledMod m = craft.corruptedMod(c.getKey());
      if (m == null || m.l() > n.ilvl() || c.getValue() <= 0 || total <= 0) {
        continue;
      }
      corrupted.add(
          new CorruptedLine(
              c.getKey(),
              m.en(),
              m.enMin(),
              m.ko(),
              m.koMin(),
              craft.tagNames(m.t(), 0),
              craft.tagNames(m.t(), 1),
              m.l(),
              implicitShare * c.getValue() / total));
    }
    corrupted.sort(Comparator.comparingDouble((CorruptedLine c) -> -c.chance()));
    // 영향력 옵션 — 영향력 없는 같은 아이템의 풀과의 차
    List<String> inf = new ArrayList<>();
    if (n.influence() != null && !n.influence().isBlank()) {
      java.util.Set<String> plain = new java.util.HashSet<>();
      craft
          .pool(
              new Item(n.itemClass(), n.variant(), "", n.ilvl(), Rarity.NORMAL, List.of(), false),
              Rarity.RARE)
          .forEach(p -> plain.add(p.id()));
      pool.forEach(
          p -> {
            if (!plain.contains(p.id())) {
              inf.add(p.id());
            }
          });
    }
    // 탐광(화석 전용) 옵션 — 화석마다 이 아이템에 붙일 수 있게 되는 자연 풀 밖 옵션(fossilEligible)
    java.util.Set<String> naturalIds = new java.util.HashSet<>();
    pool.forEach(p -> naturalIds.add(p.id()));
    Map<String, List<PoeCraftDataService.Fossil>> byMod = new LinkedHashMap<>();
    for (PoeCraftDataService.Fossil f : craft.fossils()) {
      for (String id : craft.engine().fossilEligible(n, f.slug()).keySet()) {
        if (!naturalIds.contains(id) && craft.fossilMod(id) != null) {
          byMod.computeIfAbsent(id, k -> new ArrayList<>()).add(f);
        }
      }
    }
    List<DelveLine> delve = new ArrayList<>();
    for (Map.Entry<String, List<PoeCraftDataService.Fossil>> e : byMod.entrySet()) {
      PoeCraftDataService.VeiledMod m = craft.fossilMod(e.getKey());
      delve.add(
          new DelveLine(
              e.getKey(),
              m.g(),
              m.en(),
              m.enMin(),
              m.ko(),
              m.koMin(),
              craft.tagNames(m.t(), 0),
              craft.tagNames(m.t(), 1),
              m.l(),
              e.getValue().stream().map(PoeCraftDataService.Fossil::name).toList(),
              e.getValue().stream().map(f -> f.nameKo() == null ? f.name() : f.nameKo()).toList()));
    }
    // 섬뜩한 고정 속성 — 쪽 · 계열마다 등급(화폐 등급과 같은 1~6), 등급 안 확률(가중 몫)
    List<EldritchFamily> eldritch = new ArrayList<>();
    String eldBlocked = null;
    String eldBlockedEn = null;
    if (!PoeCraftEngine.ELDRITCH_CLASSES.contains(n.itemClass())) {
      eldBlocked = "섬뜩한 고정 속성은 갑옷 · 장화 · 장갑 · 투구에만";
      eldBlockedEn = "Eldritch implicits only on body armours, boots, gloves and helmets";
    } else {
      if (n.influence() != null && !n.influence().isBlank()) {
        eldBlocked = "영향력 아이템에는 섬뜩한 불씨 · 영액을 쓸 수 없음";
        eldBlockedEn = "Embers and Ichors cannot be used on influenced items";
      }
      Map<String, Integer> ep = craft.eldritchPool(n.poolKey());
      if (ep.isEmpty() && n.influence() != null && !n.influence().isBlank()) {
        ep =
            craft.eldritchPool(
                new Item(n.itemClass(), n.variant(), "", n.ilvl(), Rarity.NORMAL, List.of(), false)
                    .poolKey());
      }
      Map<String, Double> tierSum = new java.util.HashMap<>();
      for (Map.Entry<String, Integer> e : ep.entrySet()) {
        PoeCraftDataService.EldritchMod m = craft.eldritchMod(e.getKey());
        if (m != null && e.getValue() > 0) {
          tierSum.merge(m.g() + ":" + m.tier(), (double) e.getValue(), Double::sum);
        }
      }
      Map<String, List<EldritchTier>> byFam = new LinkedHashMap<>();
      Map<String, PoeCraftDataService.EldritchMod> famMod = new java.util.HashMap<>();
      for (Map.Entry<String, Integer> e : new java.util.TreeMap<>(ep).entrySet()) {
        PoeCraftDataService.EldritchMod m = craft.eldritchMod(e.getKey());
        if (m == null || e.getValue() <= 0) {
          continue;
        }
        String key = m.g() + ":" + eldritchFamily(e.getKey());
        double s = tierSum.getOrDefault(m.g() + ":" + m.tier(), 0d);
        byFam
            .computeIfAbsent(key, k -> new ArrayList<>())
            .add(
                new EldritchTier(
                    m.tier(),
                    e.getKey(),
                    m.en(),
                    m.enMin(),
                    m.ko(),
                    m.koMin(),
                    m.tier() <= 4 && s > 0 ? e.getValue() / s : 0));
        famMod.putIfAbsent(key, m);
      }
      for (Map.Entry<String, List<EldritchTier>> e : byFam.entrySet()) {
        List<EldritchTier> tl = new ArrayList<>(e.getValue());
        tl.sort(Comparator.comparingInt(EldritchTier::tier));
        PoeCraftDataService.EldritchMod m = famMod.get(e.getKey());
        eldritch.add(
            new EldritchFamily(
                m.g(),
                e.getKey().substring(e.getKey().indexOf(':') + 1),
                craft.tagNames(m.t(), 0),
                craft.tagNames(m.t(), 1),
                tl));
      }
    }
    return new CalcExtras(
        bench, corrupted, inf, delve, eldritch, eldBlocked, eldBlockedEn, essenceOnly(n, pool));
  }

  /**
   * 에센스 전용 옵션(10-21 C551 — 사용자 요청 "최종 아이템에 붙는 옵션을 기준으로 — 옵션 자체를 고를 수 없는 게 누락되면 안 된다"의 남은 출처) — 아이템
   * 레벨로 쓸 수 있는 에센스의 보장 옵션 중 자연 풀에 같은 그룹 · 같은 문구 모양의 옵션이 없는 것. 가문마다 등급 오름차순.
   */
  List<EssenceOnly> essenceOnly(Item n, List<PoeCraftService.PoolLine> pool) {
    Map<String, List<PoeCraftDataService.Essence>> byFam = new LinkedHashMap<>();
    for (PoeCraftDataService.Essence e : craft.essences(n.itemClass())) {
      if (e.modId() == null
          || e.slug() == null
          || (e.ilvlMax() > 0 && n.ilvl() > e.ilvlMax())
          || naturalEquivalent(e, pool)) {
        continue;
      }
      byFam.computeIfAbsent(e.family(), k -> new ArrayList<>()).add(e);
    }
    List<EssenceOnly> out = new ArrayList<>();
    for (Map.Entry<String, List<PoeCraftDataService.Essence>> f : byFam.entrySet()) {
      List<PoeCraftDataService.Essence> es = new ArrayList<>(f.getValue());
      es.sort(Comparator.comparingInt(PoeCraftDataService.Essence::tier));
      PoeCraftDataService.Essence top = es.get(es.size() - 1);
      List<String> tags = top.t() == null ? List.of() : top.t();
      out.add(
          new EssenceOnly(
              f.getKey(),
              top.gen(),
              top.icon(),
              craft.tagNames(tags, 0),
              craft.tagNames(tags, 1),
              es.stream()
                  .map(
                      e ->
                          new EssenceTier(
                              e.tier(), e.modId(), e.name(), e.nameKo(), e.en(), e.ko()))
                  .toList()));
    }
    return out;
  }

  /** 자연 풀에 그 에센스 옵션과 같은 옵션(같은 id, 또는 같은 그룹 · 숫자 뺀 문구 모양)이 있는가. */
  private boolean naturalEquivalent(
      PoeCraftDataService.Essence e, List<PoeCraftService.PoolLine> pool) {
    String eShape = shape(e.enMin());
    for (PoeCraftService.PoolLine p : pool) {
      if (p.id().equals(e.modId())) {
        return true;
      }
      PoeCraftDataService.CraftMod m = craft.mod(p.id());
      boolean grp =
          e.groups() == null
              || e.groups().isEmpty()
              || (m != null && m.grp() != null && m.grp().stream().anyMatch(e.groups()::contains));
      if (grp && eShape != null && eShape.equals(shape(p.enMin()))) {
        return true;
      }
    }
    return false;
  }

  /** 섬뜩한 고정 속성 목표인가(C549) — 옵션 id 가 모두 섬뜩한 고정 속성. */
  boolean eldritchTarget(Target t) {
    return t != null
        && t.anyOf() != null
        && !t.anyOf().isEmpty()
        && t.anyOf().stream().allMatch(id -> craft.eldritchMod(id) != null);
  }

  /**
   * 섬뜩한 고정 속성 계열 키 — id 끝 등급 숫자(와 밑줄)를 뗀 것(AttackSpeedEldritchImplicit3 →
   * AttackSpeedEldritchImplicit).
   */
  static String eldritchFamily(String id) {
    return id == null ? "" : id.replaceAll("_?\\d+_?$", "");
  }

  /** 섬뜩한 고정 속성 목표 비용(C549, EldritchResult 설명). */
  EldritchResult eldritchPlan(Item item, List<Target> eld) {
    if (!PoeCraftEngine.ELDRITCH_CLASSES.contains(item.itemClass())) {
      return new EldritchResult(
          null,
          List.of(),
          "섬뜩한 고정 속성은 갑옷 · 장화 · 장갑 · 투구에만 붙습니다",
          "Eldritch implicits only apply to body armours, boots, gloves and helmets");
    }
    if (item.influence() != null && !item.influence().isBlank()) {
      return new EldritchResult(
          null,
          List.of(),
          "쉐이퍼 · 엘더 · 정복자 영향 아이템에는 섬뜩한 불씨 · 영액을 쓸 수 없습니다",
          "Eldritch Embers and Ichors cannot be used on Shaper, Elder or Conqueror influenced items");
    }
    Map<String, Integer> pool = craft.eldritchPool(item.poolKey());
    Map<String, String> ko = spentKo(item.itemClass());
    List<EldritchStep> steps = new ArrayList<>();
    java.util.Set<String> sides = new java.util.HashSet<>();
    double total = 0;
    boolean priced = true;
    for (Target t : eld) {
      PoeCraftDataService.EldritchMod m0 = craft.eldritchMod(t.anyOf().get(0));
      String side = m0.g();
      if (!sides.add(side)) {
        return new EldritchResult(
            null,
            List.of(),
            "고정 속성은 한쪽(작열의 총주교 · 세계 포식자)에 하나씩만 붙습니다",
            "Only one implicit per side (Searing Exarch · Eater of Worlds)");
      }
      java.util.Set<String> ids = new java.util.HashSet<>(t.anyOf());
      int minTier = ids.stream().mapToInt(id -> craft.eldritchMod(id).tier()).min().orElse(1);
      String fam = eldritchFamily(t.anyOf().get(0));
      EldritchStep best = null;
      for (int ct = Math.max(1, minTier); ct <= 4; ct++) {
        double sum = 0;
        double hit = 0;
        String hitId = null;
        for (Map.Entry<String, Integer> e : pool.entrySet()) {
          PoeCraftDataService.EldritchMod m = craft.eldritchMod(e.getKey());
          if (m == null || !side.equals(m.g()) || m.tier() != ct || e.getValue() <= 0) {
            continue;
          }
          sum += e.getValue();
          if (ids.contains(e.getKey())) {
            hit += e.getValue();
            hitId = e.getKey();
          }
        }
        if (sum <= 0 || hit <= 0) {
          continue;
        }
        double p = hit / sum;
        Map<String, Double> one = new LinkedHashMap<>();
        spend(
            one,
            PoeCraftEngine.EXARCH.equals(side) ? Action.EMBER : Action.ICHOR,
            String.valueOf(ct),
            item.itemClass());
        Double price = cost(one);
        String cur = one.keySet().stream().findFirst().orElse("");
        PoeCraftDataService.EldritchMod hm = craft.eldritchMod(hitId);
        EldritchStep s =
            new EldritchStep(
                side,
                fam,
                hm.en(),
                hm.ko(),
                minTier,
                ct,
                cur,
                ko.getOrDefault(cur, cur),
                p,
                1 / p,
                price == null ? null : price / p);
        if (best == null || (s.cost() != null && (best.cost() == null || s.cost() < best.cost()))) {
          best = s;
        }
      }
      if (best == null) {
        return new EldritchResult(
            null,
            List.of(),
            "그 고정 속성 등급은 화폐(하급 ~ 특출난)로 굴릴 수 없습니다 — 등급 5~6 은 갈등의 오브 · 지배의 오브",
            "That implicit tier cannot be rolled with Lesser ~ Exceptional currency — tiers 5~6 come"
                + " from the Orb of Conflict / Orb of Dominance");
      }
      steps.add(best);
      if (best.cost() == null) {
        priced = false;
      } else {
        total += best.cost();
      }
    }
    return new EldritchResult(priced ? total : null, steps, null);
  }

  /** 에센스 보장 옵션이 그 목표를 채우는가 — 같은 옵션 그룹 · 최소 굴림 첫 수치 ≥ 목표 옵션들의 최소 굴림 첫 수치 중 가장 작은 것. */
  boolean essenceCounts(PoeCraftDataService.Essence e, Target t) {
    if (isOpen(t)) {
      return false;
    }
    // 목표가 그 에센스 옵션 id 자체(에센스 전용 목표 C551 — 자연 풀에 같은 옵션이 없어 계산기가 에센스 옵션 id 로 고른다)
    if (e.modId() != null && t.anyOf().contains(e.modId())) {
      return true;
    }
    if (e.groups() == null || e.groups().isEmpty()) {
      return false;
    }
    boolean same = false;
    double floor = Double.MAX_VALUE;
    String eShape = shape(e.enMin());
    for (String id : t.anyOf()) {
      PoeCraftDataService.CraftMod m = craft.mod(id);
      // 같은 그룹이어도 다른 옵션일 수 있다(방어도 · 회피 · 에너지 보호막 고정치가 그룹 2604 하나 — 10-15 실측: 회피 에센스가 방어도 목표를 채운 것으로
      // 나왔다)
      // → 숫자를 뺀 문구 모양까지 같아야
      if (m != null
          && m.grp().stream().anyMatch(e.groups()::contains)
          && eShape != null
          && eShape.equals(shape(craft.minText(id)))) {
        same = true;
      }
      Double v = firstNumber(craft.minText(id));
      if (v != null) {
        floor = Math.min(floor, v);
      }
    }
    Double ev = firstNumber(e.enMin());
    return same && ev != null && floor != Double.MAX_VALUE && ev >= floor;
  }

  private static final java.util.regex.Pattern NUMBER =
      java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");

  /** 문구 모양 — 숫자를 # 로(같은 옵션인지 견줄 때). 없으면 null. */
  static String shape(List<String> lines) {
    return lines == null || lines.isEmpty()
        ? null
        : String.join(" / ", lines).replaceAll("[0-9]+([.][0-9]+)?", "#");
  }

  /** 문구 첫 줄의 첫 수치. 없으면 null. */
  static Double firstNumber(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return null;
    }
    java.util.regex.Matcher m = NUMBER.matcher(lines.get(0));
    return m.find() ? Double.valueOf(m.group(1)) : null;
  }

  /** 시도당 카오스 — 단가를 모르는 화폐가 하나라도 있으면 null. */
  Double cost(Map<String, Double> per) {
    double sum = 0;
    for (Map.Entry<String, Double> e : per.entrySet()) {
      if (e.getValue() <= 0) {
        continue;
      }
      Double p = prices.chaos(e.getKey());
      if (p == null) {
        return null;
      }
      sum += p * e.getValue();
    }
    return sum;
  }

  /** 매직으로 가능한 목표인가 — 목표 둘 이하(접두 하나 · 접미 하나로 채울 수 있는지는 시뮬레이션이 판정). */
  private static boolean magicFeasible(List<Target> targets) {
    return real(targets).size() <= 2;
  }

  /** 결과 분포 요청(10-21 C533, craftofexile 대량 모의) — 지금 아이템에 화폐를 samples 번(기본 5000, 100~20000) 따로 적용. */
  public record DistRequest(Item item, String action, String arg, Integer samples) {}

  /** 계열 하나 — share = 결과에 그 계열이 있는 비율, top = 결과 중 그 계열이 그 풀 1등급인 비율. */
  public record DistLine(String family, String gen, double share, double top) {}

  /** affixes = 옵션 개수 → 비율, error = 처음부터 못 쓰면 엔진 이유. */
  public record DistResult(
      int samples, List<DistLine> lines, Map<Integer, Double> affixes, String error) {}

  /** 결과 분포(C533) — 화폐 한 번의 결과에 어떤 계열이 몇 % 로 붙나(계열마다 한 번씩 셈) · 옵션 개수 분포. */
  public DistResult distribution(DistRequest req) {
    Item start = craft.normalize(req.item());
    PoeCraftEngine.Action act;
    try {
      act = PoeCraftEngine.Action.valueOf(req.action());
    } catch (RuntimeException e) {
      return new DistResult(0, List.of(), Map.of(), "모르는 화폐");
    }
    int n = Math.max(100, Math.min(20_000, req.samples() == null ? 5_000 : req.samples()));
    RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    Map<String, int[]> fam = new java.util.HashMap<>();
    Map<Integer, Integer> count = new java.util.TreeMap<>();
    int done = 0;
    for (int i = 0; i < n; i++) {
      PoeCraftEngine.Result r = craft.engine().apply(start, act, req.arg(), rng);
      if (r.error() != null) {
        if (done == 0) {
          return new DistResult(0, List.of(), Map.of(), r.error());
        }
        break;
      }
      Item it = r.item();
      done++;
      java.util.Set<String> seen = new java.util.HashSet<>();
      for (PoeCraftEngine.Affix a : it.affixes()) {
        PoeCraftDataService.CraftMod m = craft.mod(a.id());
        String key = a.gen() + "|" + (m == null || m.f() == null ? a.id() : m.f());
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
            .sorted(Comparator.comparingDouble(DistLine::share).reversed())
            .toList();
    Map<Integer, Double> affixes = new java.util.TreeMap<>();
    count.forEach((k, v) -> affixes.put(k, (double) v / total));
    return new DistResult(total, lines, affixes, null);
  }

  /** 목표까지 연타 요청(C506) — max = 최대 횟수(기본 1000, 5000 까지). */
  public record SpamRequest(
      Item item,
      String action,
      String arg,
      List<Target> targets,
      Integer max,
      // 올플레임 제작(C524) — 한 번에 실체 없는 복제본 copies 개 중 목표를 채운 것을(없으면 첫 것) 고른다, 없으면 1
      Integer copies) {

    public SpamRequest(Item item, String action, String arg, List<Target> targets, Integer max) {
      this(item, action, arg, targets, max, null);
    }
  }

  /** 연타 결과 — tries = 쓴 횟수, met = 목표를 채웠나, error = 처음부터 못 쓰면 엔진 이유. */
  public record SpamResult(
      PoeCraftService.View view, int tries, boolean met, List<String> added, String error) {}

  /** 목표까지 연타(10-20 C506, craftofexile 시뮬레이터의 '될 때까지') — 같은 제작 엔진으로 목표를 모두 채우거나 최대 횟수까지. */
  public SpamResult spam(SpamRequest req) {
    Item start = craft.normalize(req.item());
    List<Target> targets =
        req.targets() == null
            ? List.of()
            : equivalentGoal(start, req.targets(), req.action(), req.arg());
    int max = Math.max(1, Math.min(5000, req.max() == null ? 1000 : req.max()));
    PoeCraftEngine.Action act;
    try {
      act = PoeCraftEngine.Action.valueOf(req.action());
    } catch (RuntimeException e) {
      return new SpamResult(craft.view(start), 0, false, List.of(), "모르는 화폐");
    }
    RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    Item cur = start;
    int tries = 0;
    int copies = Math.max(1, Math.min(4, req.copies() == null ? 1 : req.copies()));
    boolean met = !targets.isEmpty() && meets(cur, targets);
    while (!met && tries < max) {
      // 올플레임(C524) — 같은 원본에 copies 번 적용해 목표를 채운 복제본을(없으면 첫 복제본) 가진다, 화폐는 한 번
      Item next = null;
      String err = null;
      for (int c = 0; c < copies; c++) {
        PoeCraftEngine.Result r = craft.engine().apply(cur, act, req.arg(), rng);
        if (r.error() != null) {
          err = r.error();
          break;
        }
        if (next == null) {
          next = r.item();
        }
        if (!targets.isEmpty() && meets(r.item(), targets)) {
          next = r.item();
          break;
        }
      }
      if (next == null) {
        if (tries == 0) {
          return new SpamResult(craft.view(start), 0, false, List.of(), err);
        }
        break;
      }
      cur = next;
      tries++;
      met = !targets.isEmpty() && meets(cur, targets);
    }
    java.util.Set<String> before = new java.util.HashSet<>();
    start.affixes().forEach(a -> before.add(a.id()));
    List<String> added =
        cur.affixes().stream()
            .map(PoeCraftEngine.Affix::id)
            .filter(id -> !before.contains(id))
            .toList();
    return new SpamResult(craft.view(cur), tries, met, added, null);
  }

  /** 목표 적중 확률 요청(C494) — 지금 아이템에 화폐마다 한 번 써서 목표를 모두 채울 확률. samples = 화폐마다 표본(기본 1000). */
  public record HitRequest(
      Item item, List<Target> targets, List<HitAction> actions, Integer samples) {}

  public record HitAction(String action, String arg) {}

  /** chance = 표본 중 목표를 모두 채운 비율, error = 이 아이템에 못 쓰는 이유(엔진 그대로). */
  public record Hit(String action, String arg, Double chance, String error) {}

  /**
   * 화폐 한 번의 목표 적중 확률(10-20 C485 뒤 C494, craftofexile 에뮬레이터의 '목표 확률') — 같은 제작 엔진으로 화폐마다 표본을 떠 목표를 모두
   * 채운 비율. 화폐가 많으면 표본을 줄여 전체를 2만 번 안으로.
   */
  public List<Hit> hitChance(HitRequest req) {
    Item start = craft.normalize(req.item());
    List<Target> targets = req.targets() == null ? List.of() : req.targets();
    List<HitAction> actions =
        req.actions() == null
            ? List.of()
            // 무인증 API 입력 한도(C540) — 화폐 수가 끝없으면 화폐마다 표본 하한 100 이라 엔진 적용이 한없이 는다
            : req.actions().subList(0, Math.min(200, req.actions().size()));
    int n = Math.max(100, Math.min(4000, req.samples() == null ? 1000 : req.samples()));
    if (!actions.isEmpty()) {
      n = Math.max(100, Math.min(n, 20_000 / actions.size()));
    }
    RandomGenerator rng =
        new java.util.SplittableRandom(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    List<Hit> out = new ArrayList<>();
    for (HitAction ha : actions) {
      PoeCraftEngine.Action act;
      try {
        act = PoeCraftEngine.Action.valueOf(ha.action());
      } catch (RuntimeException e) {
        out.add(new Hit(ha.action(), ha.arg(), null, "모르는 화폐"));
        continue;
      }
      int hits = 0;
      String error = null;
      List<Target> goal =
          targets.isEmpty() ? targets : equivalentGoal(start, targets, ha.action(), ha.arg());
      for (int i = 0; i < n; i++) {
        PoeCraftEngine.Result r = craft.engine().apply(start, act, ha.arg(), rng);
        if (r.error() != null) {
          error = r.error();
          break;
        }
        if (!goal.isEmpty() && meets(r.item(), goal)) {
          hits++;
        }
      }
      out.add(new Hit(ha.action(), ha.arg(), error == null ? (double) hits / n : null, error));
    }
    return out;
  }

  /** 서로 다른 옵션으로 목표를 모두 채우는가(목표 수가 적어 백트래킹으로 충분). */
  static boolean meets(Item item, List<Target> targets) {
    int[] need = openNeed(targets);
    if (need[0] + need[1] > 0) {
      int[] free = openFree(item);
      if (free[0] < need[0] || free[1] < need[1]) {
        return false;
      }
    }
    List<String> ids = item.affixes().stream().map(PoeCraftEngine.Affix::id).toList();
    return assign(ids, real(targets), 0, new boolean[ids.size()]);
  }

  private static boolean assign(List<String> ids, List<Target> targets, int t, boolean[] used) {
    if (t == targets.size()) {
      return true;
    }
    List<String> any = targets.get(t).anyOf();
    for (int i = 0; i < ids.size(); i++) {
      if (!used[i] && any != null && any.contains(ids.get(i))) {
        used[i] = true;
        if (assign(ids, targets, t + 1, used)) {
          return true;
        }
        used[i] = false;
      }
    }
    return false;
  }
}
