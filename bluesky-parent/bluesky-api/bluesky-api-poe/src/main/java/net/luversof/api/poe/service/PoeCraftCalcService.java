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
      Item item, List<Target> targets, List<String> methods, String essence, Integer trials) {}

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
      String note) {}

  public record Response(
      List<MethodResult> results, String league, String pricesAt, String error) {}

  /** 에뮬레이터에서 쓴 단계 하나(C365) — action = 엔진 화폐 이름 또는 FORESEE(히네코라의 머리카락), arg = 화폐 인자. */
  public record SpentStep(String action, String arg) {}

  public record SpentRequest(String itemClass, List<SpentStep> steps) {}

  /** 화폐 한 줄 — 개수 · 단가(카오스, 시세 없으면 null) · 합계(개수 × 단가). */
  public record SpentLine(
      String name, String nameKo, double count, Double unitChaos, Double subtotalChaos) {}

  /** 쓴 화폐 한국어 이름 — 데이터(화폐 설명 · 에센스 · 화석 · 공명기 · 제작대 비용)에 없는 것(BaseItemTypes 한국어, C380). */
  /** 짐승 제작 인자 → 쓴 화폐 표 이름(C387, BestiaryRecipes 설명 — 한국어는 SPENT_KO). */
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

  static final Map<String, String> SPENT_KO =
      Map.ofEntries(
          Map.entry("Bestiary: Add a Suffix, Remove a Random Prefix", "짐승 제작: 접미어 추가, 무작위 접두어 제거"),
          Map.entry("Bestiary: Add a Prefix, Remove a Random Suffix", "짐승 제작: 접두어 추가, 무작위 접미어 제거"),
          Map.entry("Bestiary: Add a Mod to a Shaper Item", "짐승 제작: 쉐이퍼 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to an Elder Item", "짐승 제작: 엘더 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Redeemer Item", "짐승 제작: 대속자 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Hunter Item", "짐승 제작: 사냥꾼 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Crusader Item", "짐승 제작: 십자군 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Add a Mod to a Warlord Item", "짐승 제작: 전쟁군주 아이템에 속성 부여 추가"),
          Map.entry("Bestiary: Level 30 Aspect of the Cat skill", "짐승 제작: 30레벨 고양이의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Avian skill", "짐승 제작: 30레벨 새의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Spider skill", "짐승 제작: 30레벨 거미의 위상 스킬"),
          Map.entry("Bestiary: Level 30 Aspect of the Crab skill", "짐승 제작: 30레벨 게의 위상 스킬"),
          Map.entry("Bestiary: Imprint", "짐승 제작: 각인"),
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
    if (req.targets() == null || req.targets().isEmpty()) {
      return new Response(List.of(), null, null, "목표 옵션을 하나 이상 고르세요");
    }
    for (int i = 0; i < req.targets().size(); i++) {
      Target t = req.targets().get(i);
      if (t == null || t.anyOf() == null || t.anyOf().isEmpty()) {
        // 빈 목표면 모든 방법이 0% 로 나와 "불가능한 조합"처럼 보였다(10-15 실측 — 계열 키를 잘못 준 요청)
        return new Response(List.of(), null, null, "목표 " + (i + 1) + "에 옵션이 없습니다");
      }
    }
    Item item = craft.normalize(req.item());
    if (item.corrupted()) {
      // 타락한 아이템(C382) — 화폐가 모두 막혀, 이미 목표를 채운 아이템이면 '성공 100%'처럼 나왔다
      return new Response(
          List.of(), null, null, "타락한 아이템은 화폐로 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)");
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
    switch (method) {
      case "CHAOS" -> {
        // 레어 출발점 — 일반이면 연금 한 번(비용 밖), 매직이면 제왕은 옵션이 남아 결과가 달라 여기선 정제 → 연금으로 본다
        Item start = item;
        if (item.rarity() != Rarity.RARE) {
          start = normal(item);
          if (start.rarity() == Rarity.NORMAL) {
            start = engine.apply(start, Action.ALCHEMY, null, rng).item();
            note = "일반 → 연금 한 번 뒤 카오스";
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
        if (targets.size() != 3) {
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
                List.of(targets.get(0), targets.get(1)),
                List.of(targets.get(0), targets.get(2)),
                List.of(targets.get(1), targets.get(2)));
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
                method, label(method, e), 0, 0, 0, null, Map.of(), null, null, r.error());
          }
          if (meets(r.item(), goal)) {
            hits++;
          }
        }
        per.put(e.name(), 1.0);
        note = "보장 옵션: " + String.join(", ", e.ko() != null && !e.ko().isEmpty() ? e.ko() : e.en());
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
        hits == 0
            ? (note == null ? "" : note + " · ")
                + "시도 "
                + trials
                + "번에 한 번도 안 나옴(확률 < 1/"
                + trials
                + ")"
            : note);
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
        b.note());
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
      Item item, List<Target> targets, List<Step> steps, Integer trials) {}

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
      String error) {}

  static final int STRATEGY_DEFAULT_RUNS = 20_000;
  static final int STRATEGY_MAX_STEP = 500;

  /** 한 회차 진행 상태 — 아이템 · 중단 여부 · 화폐 합계(회차마다 합산). */
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
    for (int i = 0; i < runs && ops < STRATEGY_BUDGET; i++) {
      done++;
      Run run = new Run();
      run.item = start;
      run.samplers = samplers;
      run.samplerOps = samplerOps;
      for (int s = 0; s < nSteps && !run.aborted; s++) {
        reached[s]++;
        uses[s] += step(run, req.steps().get(s), req.targets(), currency, rng);
      }
      if (!run.aborted && meets(run.item, req.targets())) {
        successes++;
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
            : cut ? "계산량 한도로 " + runs + "회까지만 모의했습니다(오차가 큽니다)" : null);
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
      // 기억 없는 반복: 한 화폐, 또는 [제작대 …, 다시 굴리기] 묶음(제작대는 정해진 결과 — 잠금 메타 + 카오스 등)
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
   * 표본으로 처리할 수 있는 묶음 — [제작대 …] + 기억 없는 다시 굴리기 + [그 뒤 아무 화폐(묶음 제외)]. 다시 굴리기가 남는 옵션만 남기므로 묶음 한 번의 결과
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
   * 기억 없는 반복 단계 — 표본으로 성공까지의 사용 수를 기하분포로 뽑는다(상한을 넘으면 그 회차 중단). 앞의 제작대가 실패하면(자리 · 그룹) -1 — 그땐 한 번씩
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
        // 제작대 비용은 묶음마다 한 번 — 표본 회수만큼 곱해 평균에 맞춘다
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

  /** 화폐 한 번 사용 → 시세 이름별 개수(제작대 · 화석 · 수확 · 정복자 포함). */
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
      // 축복의 오브(C375) — 게이트가 임플리싯 수치만 다시 굴린다
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

  /** 쓴 화폐 한국어 이름표(C380) — 고정 이름표 + 이 종류의 화폐 설명 · 에센스 · 화석 · 공명기 · 제작대 비용. */
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
      // 짐승 제작(C387) — 화폐가 아니라 포획한 짐승(레시피 표에 재료가 없어 시세 없음, 횟수만)
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
   * 출발점(10-15 C349) — 분열 옵션 · 임플리싯은 남기고 나머지는 비운다(지금 아이템에서 시작 — 분열 베이스 등). 분열 옵션이 있으면 정제해도 일반이 되지
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
      String method, String label, int trials, int hits, Map<String, Double> per, String note) {
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
        hits == 0
            ? (note == null ? "" : note + " · ")
                + "시도 "
                + trials
                + "번에 한 번도 안 나옴(확률 < 1/"
                + trials
                + ")"
            : note);
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
    if (cands.isEmpty()) {
      return List.of();
    }
    Item start = normal(item);
    PoeCraftEngine engine = craft.engine();
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
    List<PoeCraftDataService.Fossil> top =
        cands.stream()
            .sorted(Comparator.comparing((PoeCraftDataService.Fossil f) -> -score.get(f.slug())))
            .limit(5)
            .toList();
    // 조합 — 1개짜리도 후보(공명기 1칸)
    List<List<PoeCraftDataService.Fossil>> combos = new ArrayList<>();
    int n = top.size();
    for (int mask = 1; mask < (1 << n); mask++) {
      if (Integer.bitCount(mask) > 4) {
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
      out.add(
          result(
              "FOSSIL:" + slugs,
              // 공명기 이름은 한국어(C423 UX QA — 화면 결과에 '(Primitive Chaotic Resonator)' 가 섞였다), 비용 키는 영어
              // 그대로
              names + " (" + resonatorNameKo(s.combo().size()) + ")",
              trials,
              h,
              fossilCost(s.combo()),
              "화석 " + s.combo().size() + "개 · 태그 배수 곱"));
    }
    return out;
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
              trials,
              hits,
              per,
              "레어에서 시작(연금 한 번 비용 밖)"));
    }
    all.sort(
        Comparator.comparing(
            (MethodResult r) -> r.expectedCost() == null ? Double.MAX_VALUE : r.expectedCost()));
    return all.subList(0, Math.min(2, all.size()));
  }

  /**
   * 섬뜩한 카오스 연타(10-15 C343) — 목표가 모두 접두(또는 모두 접미)일 때만. 그쪽이 우세하도록 불씨 · 영액을 한 번(상급 + 하급, 비용 밖) 쓴 레어에서
   * 섬뜩한 카오스로 그쪽만 다시 굴린다. 반대쪽은 시도마다 연금으로 새로 뽑는 근사(실제로는 한 번 정해지면 그대로 — 반대쪽 옵션 수에 따라 확률이 달라진다).
   */
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
            trials,
            hits,
            Map.of("Veiled Chaos Orb", 1.0),
            "해제(쥰 작업대)는 비용 없음 · 처음 레어는 비용 밖"));
  }

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
            trials,
            hits,
            Map.of("Eldritch Chaos Orb", 1.0),
            "불씨 · 영액 한 번(상급 + 하급)은 비용 밖 · 반대쪽은 시도마다 새로(근사)"));
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
                r.note()));
      }
    }
    all.sort(
        Comparator.comparing(
            (MethodResult r) -> r.expectedCost() == null ? Double.MAX_VALUE : r.expectedCost()));
    return all.subList(0, Math.min(1, all.size()));
  }

  /** 서로 다른 옵션으로 채울 수 있는 목표 수의 최댓값(C456 — 조합의 출발 조건 · 남은 목표). */
  static int matched(Item item, List<Target> targets) {
    List<String> ids = item.affixes().stream().map(PoeCraftEngine.Affix::id).toList();
    return most(ids, targets, 0, new boolean[ids.size()]);
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
   * 제작대 옵션이 목표를 채우는가(C456, essenceCounts 와 같은 규칙) — 같은 옵션 그룹 · 같은 문구 모양이고 최소 굴림이 목표 최하 등급의 최소 굴림
   * 이상.
   */
  boolean benchCounts(PoeCraftDataService.BenchCraft b, Target t) {
    if (b.groups() == null || b.groups().isEmpty()) {
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

  /** 에센스 보장 옵션이 그 목표를 채우는가 — 같은 옵션 그룹 · 최소 굴림 첫 수치 ≥ 목표 옵션들의 최소 굴림 첫 수치 중 가장 작은 것. */
  boolean essenceCounts(PoeCraftDataService.Essence e, Target t) {
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
    return targets.size() <= 2;
  }

  /** 서로 다른 옵션으로 목표를 모두 채우는가(목표 수가 적어 백트래킹으로 충분). */
  static boolean meets(Item item, List<Target> targets) {
    List<String> ids = item.affixes().stream().map(PoeCraftEngine.Affix::id).toList();
    return assign(ids, targets, 0, new boolean[ids.size()]);
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
