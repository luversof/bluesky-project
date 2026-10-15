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

  static final int DEFAULT_TRIALS = 100_000;
  static final int MAX_TRIALS = 1_000_000;

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
    int trials =
        Math.max(1_000, Math.min(MAX_TRIALS, req.trials() == null ? DEFAULT_TRIALS : req.trials()));
    List<String> methods =
        req.methods() == null || req.methods().isEmpty()
            ? List.of("CHAOS", "ALT", "ESSENCE")
            : req.methods();
    List<MethodResult> out = new ArrayList<>();
    for (String m : methods) {
      MethodResult r =
          run(m, item, req.targets(), req.essence(), trials, new SplittableRandom(m.hashCode()));
      if (r != null) {
        out.add(r);
      }
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
          start =
              new Item(
                  item.itemClass(),
                  item.variant(),
                  item.influence(),
                  item.ilvl(),
                  Rarity.NORMAL,
                  List.of(),
                  false);
          start = engine.apply(start, Action.ALCHEMY, null, rng).item();
          note = "일반 → 연금 한 번 뒤 카오스";
        }
        for (int i = 0; i < trials; i++) {
          if (meets(engine.apply(start, Action.CHAOS, null, rng).item(), targets)) {
            hits++;
          }
        }
        per.put("Chaos Orb", 1.0);
      }
      case "ALT" -> {
        if (!magicFeasible(targets)) {
          return null;
        }
        Item start =
            new Item(
                item.itemClass(),
                item.variant(),
                item.influence(),
                item.ilvl(),
                Rarity.MAGIC,
                List.of(),
                false);
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
        Item start =
            new Item(
                item.itemClass(),
                item.variant(),
                item.influence(),
                item.ilvl(),
                Rarity.NORMAL,
                List.of(),
                false);
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

  private static String label(String method, PoeCraftDataService.Essence e) {
    return switch (method) {
      case "CHAOS" -> "카오스 오브 연타";
      case "ALT" -> "변경의 오브 연타(+확장)";
      case "ESSENCE" -> e == null ? "에센스 연타" : e.nameKo() + " 연타";
      default -> method;
    };
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
    for (String id : t.anyOf()) {
      PoeCraftDataService.CraftMod m = craft.mod(id);
      if (m != null && m.grp().stream().anyMatch(e.groups()::contains)) {
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

  /** 문구 첫 줄의 첫 수치. 없으면 null. */
  static Double firstNumber(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return null;
    }
    java.util.regex.Matcher m = NUMBER.matcher(lines.get(0));
    return m.find() ? Double.valueOf(m.group(1)) : null;
  }

  /** 시도당 카오스 — 단가를 모르는 화폐가 하나라도 있으면 null. */
  private Double cost(Map<String, Double> per) {
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
