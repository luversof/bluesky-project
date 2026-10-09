package net.luversof.api.poe.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 빌드 화면 아이템 → 거래소 검색 쿼리(q JSON) — PoE1 · PoE2 빌드 화면 공용(10-08). 시뮬레이터 결과는 이제 빌드 화면에서 열고, 거래소 링크도 빌드
 * 화면이 준다.
 *
 * <ul>
 *   <li>고유: 이름(한국 서버라 한국어 이름) + 즉시 구입 + 가격순.
 *   <li>레어 · 마법: 베이스(한국어) + 익스플리싯 옵션 줄의 스탯 필터. 생명력 · 저항 · 능력치는 합산(pseudo) — 시장 매물은 순수 + 하이브리드 옵션으로
 *       나뉘어 있어 단일 옵션 검색은 매물이 거의 없다(최적화기 거래소 링크와 같은 규칙). 최소값 = 이 아이템 수치의 {@link #MIN_RATIO}(비슷한 수준
 *       이상 — 똑같은 값만 찾으면 매물이 없다). 수치가 둘인 줄(피해 X~Y 추가)과 감소 줄은 옵션 존재만, 제작(crafted) 줄은 뺀다(거래소 제작 옵션 칸은
 *       따로라 익스플리싯 필터로는 안 잡힌다). 옵션이 4개 이상이면 그중 n−1 개 이상(count 그룹).
 * </ul>
 */
public final class PoeTradeQueries {

  /** 레어 옵션 최소값 = 이 아이템 수치 × 0.85(내림). 조정은 여기 한 곳. */
  public static final double MIN_RATIO = 0.85;

  private static final Pattern NUMBER = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

  private PoeTradeQueries() {}

  /** 고유 — 이름 검색. 이름이 없으면 null. */
  public static String unique(String nameKo, String name) {
    String n = nameKo != null && !nameKo.isBlank() ? nameKo : name;
    if (n == null || n.isBlank()) {
      return null;
    }
    return "{\"query\":{\"status\":{\"option\":\"securable\"},\"name\":\""
        + escape(n)
        + "\",\"stats\":[{\"type\":\"and\",\"filters\":[]}]},\"sort\":{\"price\":\"asc\"}}";
  }

  /**
   * 레어 · 마법 — 베이스 + 익스플리싯 옵션 필터. kinds 는 옵션 줄과 같은 길이의 줄 종류("crafted" 등, 없으면 null). 걸 필터도 베이스도 없으면
   * null.
   */
  public static String rare(
      String baseTypeKo,
      List<String> explicitKo,
      List<String> kinds,
      PoeTradeStatDataService stats) {
    return rare(baseTypeKo, explicitKo, kinds, stats, MIN_RATIO);
  }

  /**
   * ratio = 최소값 비율 — 빌드 아이템은 {@link #MIN_RATIO}(이 수치 근처), 업그레이드 가이드 레어 목표는 1.0(옵션 줄이 이미 그 티어의 최저 롤
   * — 사용자가 확정한 "같은 티어면 시장에서 살 수 있는 하한" 규칙, 10-08 C171).
   */
  public static String rare(
      String baseTypeKo,
      List<String> explicitKo,
      List<String> kinds,
      PoeTradeStatDataService stats,
      double ratio) {
    Map<String, Double> sums = new LinkedHashMap<>(); // pseudo id → 합산 값
    Map<String, Double> mins = new LinkedHashMap<>(); // explicit id → 값(null = 존재만)
    // 한국어가 영문 두 줄을 한 줄로 합치면 줄 종류와 길이가 어긋난다 — 그땐 제작 줄 판정을 하지 않는다(엉뚱한 줄을 뺄 수 있어서)
    if (kinds != null && explicitKo != null && kinds.size() != explicitKo.size()) {
      kinds = null;
    }
    for (int i = 0; explicitKo != null && i < explicitKo.size(); i++) {
      String line = explicitKo.get(i);
      if (line == null || line.isBlank()) {
        continue;
      }
      if (kinds != null && i < kinds.size() && "crafted".equals(kinds.get(i))) {
        continue;
      }
      Double value = singleNumber(line);
      if (line.contains("감소")) {
        value = null; // 거래소 사전은 증가 쪽 문구 — 감소는 같은 id 의 음수라 최소값을 걸면 반대가 된다
      }
      String pseudo = stats.pseudoIdFor(line);
      if (pseudo != null && value != null) {
        sums.merge(pseudo, value, Double::sum);
        continue;
      }
      String id = stats.statIdFor(line);
      if (id != null && !mins.containsKey(id)) {
        mins.put(id, value);
      }
    }
    List<String> filters = new ArrayList<>();
    sums.forEach((id, v) -> filters.add(filter(id, v, ratio)));
    mins.forEach((id, v) -> filters.add(filter(id, v, ratio)));
    boolean hasType = baseTypeKo != null && !baseTypeKo.isBlank();
    if (filters.isEmpty() && !hasType) {
      return null;
    }
    // 옵션이 4개 이상이면 "하나는 빠져도"(count 그룹, 최소 n−1) — 전부 요구하면 최상급 레어는 매물이 0 이었다(10-08 실측: PoE2 활 6/6=0 ·
    //   5/6=67 · 4/6=114). 3개 이하면 전부(and).
    String group =
        filters.size() >= 4
            ? "{\"type\":\"count\",\"value\":{\"min\":" + (filters.size() - 1) + "},\"filters\":["
            : "{\"type\":\"and\",\"filters\":[";
    return "{\"query\":{\"status\":{\"option\":\"securable\"},"
        + (hasType ? "\"type\":\"" + escape(baseTypeKo) + "\"," : "")
        + "\"stats\":["
        + group
        + String.join(",", filters)
        + "]}]},\"sort\":{\"price\":\"asc\"}}";
  }

  private static String filter(String id, Double value, double ratio) {
    if (value == null) {
      return "{\"id\":\"" + id + "\"}";
    }
    // 10 미만(0.8% 흡수 · 4.95% 치명타 같은 작은 수치)은 소수 첫째 자리까지 — 정수로 내리면 0 이 된다
    double min = value < 10 ? Math.floor(value * ratio * 10) / 10 : Math.floor(value * ratio);
    return "{\"id\":\""
        + id
        + "\",\"value\":{\"min\":"
        + (min == Math.rint(min) ? String.valueOf((long) min) : String.valueOf(min))
        + "}}";
  }

  /** 장비 분류(PoeBaseItem.itemClass) → 거래소 분류 id(type_filters.category). 없으면 null. */
  private static final Map<String, String> CATEGORY_BY_CLASS =
      Map.ofEntries(
          Map.entry("Boots", "armour.boots"),
          Map.entry("Gloves", "armour.gloves"),
          Map.entry("Helmet", "armour.helmet"),
          Map.entry("Body Armour", "armour.chest"),
          Map.entry("Shield", "armour.shield"),
          Map.entry("Quiver", "armour.quiver"),
          Map.entry("Ring", "accessory.ring"),
          Map.entry("Amulet", "accessory.amulet"),
          Map.entry("Belt", "accessory.belt"),
          Map.entry("Bow", "weapon.bow"),
          Map.entry("Claw", "weapon.claw"),
          Map.entry("Dagger", "weapon.basedagger"),
          Map.entry("Rune Dagger", "weapon.runedagger"),
          Map.entry("One Hand Axe", "weapon.oneaxe"),
          Map.entry("One Hand Sword", "weapon.basesword"),
          Map.entry("Thrusting One Hand Sword", "weapon.rapier"),
          Map.entry("One Hand Mace", "weapon.basemace"),
          Map.entry("Sceptre", "weapon.sceptre"),
          Map.entry("Staff", "weapon.basestaff"),
          Map.entry("Warstaff", "weapon.warstaff"),
          Map.entry("Two Hand Axe", "weapon.twoaxe"),
          Map.entry("Two Hand Mace", "weapon.twomace"),
          Map.entry("Two Hand Sword", "weapon.twosword"),
          Map.entry("Wand", "weapon.wand"));

  /**
   * 업그레이드 가이드 레어 목표(10-08 C171) — "무엇을 살지" 찾는 출발점이라 베이스를 정확히 맞추지 않고 분류(장화 · 반지 …)로, 옵션은 그 티어 최저 롤
   * 이상(비율 1.0), 옵션이 5개 이상이면 2개는 빠져도(n−2). 실측: 장화 목표 옵션 6개 — 베이스 + n−1 = 0건 · 베이스 + n−2 = 3건 · 분류 +
   * n−2 = 376건. 분류를 모르면 베이스로.
   */
  public static String rareTarget(
      String itemClass, String baseTypeKo, List<String> explicitKo, PoeTradeStatDataService stats) {
    return rareTarget(CATEGORY_BY_CLASS, itemClass, baseTypeKo, explicitKo, stats, 1.0);
  }

  /** PoE2 장비 분류 → PoE2 거래소 분류 id(PoE1 과 일부 다르다: 한손검 · 한손 철퇴 · 지팡이 · 단검, PoE2 전용 석궁 · 창 · 철퇴 …). */
  private static final Map<String, String> CATEGORY_BY_CLASS_POE2 =
      Map.ofEntries(
          Map.entry("Boots", "armour.boots"),
          Map.entry("Gloves", "armour.gloves"),
          Map.entry("Helmet", "armour.helmet"),
          Map.entry("Body Armour", "armour.chest"),
          Map.entry("Shield", "armour.shield"),
          Map.entry("Buckler", "armour.buckler"),
          Map.entry("Focus", "armour.focus"),
          Map.entry("Quiver", "armour.quiver"),
          Map.entry("Ring", "accessory.ring"),
          Map.entry("Amulet", "accessory.amulet"),
          Map.entry("Belt", "accessory.belt"),
          Map.entry("Bow", "weapon.bow"),
          Map.entry("Crossbow", "weapon.crossbow"),
          Map.entry("Claw", "weapon.claw"),
          Map.entry("Dagger", "weapon.dagger"),
          Map.entry("Spear", "weapon.spear"),
          Map.entry("Flail", "weapon.flail"),
          Map.entry("One Hand Axe", "weapon.oneaxe"),
          Map.entry("One Hand Sword", "weapon.onesword"),
          Map.entry("One Hand Mace", "weapon.onemace"),
          Map.entry("Sceptre", "weapon.sceptre"),
          Map.entry("Staff", "weapon.staff"),
          Map.entry("Warstaff", "weapon.warstaff"),
          Map.entry("Talisman", "weapon.talisman"),
          Map.entry("Two Hand Axe", "weapon.twoaxe"),
          Map.entry("Two Hand Mace", "weapon.twomace"),
          Map.entry("Two Hand Sword", "weapon.twosword"),
          Map.entry("Wand", "weapon.wand"));

  /** PoE2 가이드 레어 목표(10-08 C172) — 옵션 줄이 최고 롤이라 비율은 빌드 아이템과 같은 {@link #MIN_RATIO}. */
  public static String rareTarget2(
      String itemClass, String baseTypeKo, List<String> explicitKo, PoeTradeStatDataService stats) {
    return rareTarget(CATEGORY_BY_CLASS_POE2, itemClass, baseTypeKo, explicitKo, stats, MIN_RATIO);
  }

  private static String rareTarget(
      Map<String, String> categories,
      String itemClass,
      String baseTypeKo,
      List<String> explicitKo,
      PoeTradeStatDataService stats,
      double ratio) {
    String base = rare(baseTypeKo, explicitKo, null, stats, ratio);
    String category = itemClass == null ? null : categories.get(itemClass);
    if (base == null || category == null || baseTypeKo == null || baseTypeKo.isBlank()) {
      return base;
    }
    String typePart = "\"type\":\"" + escape(baseTypeKo) + "\",";
    String q =
        base.replace(
            typePart,
            "\"filters\":{\"type_filters\":{\"filters\":{\"category\":{\"option\":\""
                + category
                + "\"}}}},");
    // count 그룹 최소를 n−2 로(옵션 5개 이상일 때) — rare() 는 4개 이상이면 n−1
    Matcher m = Pattern.compile("\"type\":\"count\",\"value\":\\{\"min\":([0-9]+)\\}").matcher(q);
    if (m.find()) {
      int n = Integer.parseInt(m.group(1)) + 1;
      if (n >= 5) {
        q = q.substring(0, m.start(1)) + (n - 2) + q.substring(m.end(1));
      }
    }
    return q;
  }

  /** 줄의 수치가 하나면 그 값, 둘 이상이거나 없으면 null. */
  static Double singleNumber(String line) {
    Matcher m = NUMBER.matcher(line);
    if (!m.find()) {
      return null;
    }
    double first = Double.parseDouble(m.group());
    return m.find() ? null : first;
  }

  private static String escape(String s) {
    return s.replace("\\", "").replace("\"", "");
  }
}
