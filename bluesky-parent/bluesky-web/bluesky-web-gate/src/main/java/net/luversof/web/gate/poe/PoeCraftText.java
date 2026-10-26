package net.luversof.web.gate.poe;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PoE1 제작 엔진(API) 문구의 영어(10-22 C558 — 영어 화면 에뮬레이터에 화폐를 못 쓰는 이유 · 오류가 한국어로 나왔다. PoE2 C447
 * Poe2CraftText 의 짝). 인게임 문구가 있는 것은 게임 영어 원문, 나머지는 같은 뜻의 영어. 모르는 문구는 그대로 둔다.
 *
 * @author bluesky
 */
public final class PoeCraftText {

  private static final Map<String, String> EN = new HashMap<>();

  static {
    // 인게임 문구(ClientStrings 한국어 = 영어 원문)
    EN.put("아이템 하나 혹은 두 개의 희귀도가 일반입니다.", "One or both items are Normal rarity.");
    EN.put("아이템의 퀄리티가 이미 최대입니다.", "Item already at max quality.");
    EN.put("이미 장막 속성 부여가 있습니다.", "Item already has a Veiled modifier.");
    EN.put("타락한 아이템은 다시 속성 부여 개조할 수 없습니다", "Corrupted items cannot be modified");
    // 엔진 상수(C560 검토 — fail(item, 상수) 라 첫 번역표에서 빠졌다), ClientStrings 영어 원문
    EN.put(
        "속성의 결과에 영향을 미치는 속성 부여가 포함된 아이템에는 해당 화폐를 사용할 수 없습니다.",
        "You can't use this currency on items with a modifier that affects mod outcomes.");
    EN.put("아이템에 분열된 속성 부여가 적용되어 있습니다.", "Item has Fractured Mods");
    EN.put(
        "아이템 레벨이 68 미만이므로 영향력 속성 부여를 추가할 수 없습니다.",
        "Item cannot gain any Influence Modifiers because its Item Level is below 68.");
    // 조건으로 고르는 문구(C560)
    EN.put(
        "작업대 옵션은 하나만(제작 옵션 3개 메타 제작이 있으면 3개)",
        "Only one bench craft (three with the 'Can have up to 3 Crafted Modifiers' meta craft)");
    EN.put("작업대 옵션은 3개까지", "Up to three bench crafts");
    EN.put(
        "제작 옵션은 하나만(제작 옵션 3개 메타 제작이 있으면 3개)",
        "Only one crafted modifier (three with the 'Can have up to 3 Crafted Modifiers' meta craft)");
    EN.put("제작 옵션은 3개까지", "Up to three crafted modifiers");
    EN.put("빈 접두어 자리가 없습니다", "No open prefix");
    EN.put("빈 접미어 자리가 없습니다", "No open suffix");
    EN.put("지울 접두어가 없습니다(분열 · 잠긴 쪽 제외)", "No prefix to remove (fractured · locked side excluded)");
    EN.put("지울 접미어가 없습니다(분열 · 잠긴 쪽 제외)", "No suffix to remove (fractured · locked side excluded)");
    EN.put("접두에 지울 수 있는 옵션이 없습니다", "No prefix can be removed");
    EN.put("접미에 지울 수 있는 옵션이 없습니다", "No suffix can be removed");
    EN.put("해제할 수 있는 옵션이 없습니다", "No modifier can be unveiled");
    EN.put("제시된 셋 중 하나를 고르세요", "Choose one of the three offered modifiers");
    // 엔진 자체 문구
    EN.put("갑옷 · 투구 · 장갑 · 장화에만 쓸 수 있습니다", "Body armours, helmets, gloves and boots only");
    EN.put("강제 옵션 쪽이 잠겨 있습니다(메타 제작)", "The forced modifier's side is locked (meta craft)");
    EN.put(
        "강제 옵션이 들어갈 자리가 없습니다(같은 그룹 · 자리)", "No room for the forced modifier (same group · slots)");
    EN.put("같은 그룹 옵션이 이미 있습니다", "The item already has a modifier of the same group");
    EN.put("같은 화석은 한 공명기에 하나만 넣을 수 있습니다", "Only one of each fossil per resonator");
    EN.put("고정할 옵션이 없습니다", "No modifier to fracture");
    EN.put("그 영향력이 있는 아이템에만 쓸 수 있습니다", "Only items with that influence");
    EN.put("그 옵션이 없습니다", "The item does not have that modifier");
    EN.put("그 태그 옵션이 붙을 수 없습니다", "No modifier with that tag can roll");
    EN.put("남는 옵션과 같은 그룹이라 쓸 수 없습니다", "Same group as a remaining modifier");
    EN.put("넣을 옵션을 고르세요", "Choose a modifier to add");
    EN.put("능력치 하나를 부여하는 속성이 없습니다", "No modifier that grants a single attribute");
    EN.put("다른 영향력의 아이템을 고르세요", "Choose an item with a different influence");
    EN.put("더 올릴 수 있는 고정 속성이 없습니다", "No implicit can be upgraded further");
    EN.put("레어 갑옷 · 장화 · 장갑 · 투구에만 쓸 수 있습니다", "Rare body armours, boots, gloves and helmets only");
    EN.put("레어 아이템에만 쓸 수 있습니다", "Rare items only");
    EN.put("매직 · 레어 아이템에만 쓸 수 있습니다", "Magic or rare items only");
    EN.put("매직 · 희귀 아이템에만 쓸 수 있습니다", "Magic or rare items only");
    EN.put("매직 아이템에만 쓸 수 있습니다", "Magic items only");
    EN.put("모르는 수확 제작", "Unknown harvest craft");
    EN.put("바꿀 등급을 찾지 못했습니다", "No tier to change to was found");
    EN.put("바꿀 수 있는 다른 영향력이 없습니다", "No other influence to change to");
    EN.put("바꿀 수 있는 옵션이 없습니다", "No modifier can be changed");
    EN.put("바꿀 옵션과 같은 그룹 옵션이 이미 있습니다", "A modifier of the target group already exists");
    EN.put("반지 · 목걸이 · 허리띠에만 쓸 수 있습니다", "Rings, amulets and belts only");
    EN.put("변환 정보를 읽지 못했습니다", "Could not read the conversion");
    EN.put("보장 옵션 쪽이 잠겨 있습니다(메타 제작)", "The guaranteed modifier's side is locked (meta craft)");
    EN.put("보장 옵션이 들어갈 자리가 없습니다", "No room for the guaranteed modifier");
    EN.put("보장할 옵션이 붙을 자리가 없습니다", "No room for the guaranteed modifier");
    EN.put(
        "분열된 화석은 영향력 · 분열 아이템에 쓸 수 없습니다",
        "Fractured Fossils cannot be used on influenced or fractured items");
    EN.put(
        "붙을 수 있는 덫 · 지뢰 속성이 없습니다(자리 · 이미 있음)",
        "No trap or mine modifier can be added (slots · already present)");
    EN.put("붙을 수 있는 영향력 옵션이 없습니다", "No influenced modifier can be added");
    EN.put("붙을 수 있는 옵션이 없습니다", "No modifier can be added");
    EN.put("붙이거나 지울 수 있는 옵션이 없습니다", "No modifier can be added or removed");
    EN.put("비-고유 목걸이에만 쓸 수 있습니다", "Non-unique amulets only");
    EN.put("빈 자리가 없습니다", "No open slot");
    EN.put(
        "쉐이퍼 · 엘더 · 엘더슬레이어 영향력 아이템에는 쓸 수 없습니다",
        "Cannot be used on Shaper, Elder or Elderslayer influenced items");
    EN.put("아직 계산하지 않는 수확 제작입니다", "This harvest craft is not modelled yet");
    EN.put("알 수 없는 기폭제입니다", "Unknown catalyst");
    EN.put("알 수 없는 두카트입니다", "Unknown ducat");
    EN.put("알 수 없는 영향력입니다", "Unknown influence");
    EN.put("알 수 없는 위상입니다", "Unknown aspect");
    EN.put("알 수 없는 야수 제작입니다", "Unknown beastcraft");
    EN.put("없앨 아이템의 영향력 옵션이 아닙니다", "Not an influenced modifier of the item to be destroyed");
    EN.put("영향력 엑잘티드 오브의 영향력이 아닙니다", "Not the influence of that Influenced Exalted Orb");
    EN.put("영향력 옵션이 둘 이상인 아이템에만 쓸 수 있습니다", "Only items with two or more influenced modifiers");
    EN.put("영향력이 없는 아이템에만 쓸 수 있습니다", "Non-influenced items only");
    EN.put("영향력이 있는 레어에만 쓸 수 있습니다", "Influenced rare items only");
    EN.put(
        "영향력이 있는 아이템에는 섬뜩한 고정 속성을 추가할 수 없습니다", "Cannot add Eldritch implicits to influenced items");
    EN.put("영향력이 있는 아이템에는 쓸 수 없습니다", "Cannot be used on influenced items");
    EN.put("영향력이 있는 아이템에만 쓸 수 있습니다", "Influenced items only");
    EN.put("영향력이 하나인 아이템에만 쓸 수 있습니다", "Items with a single influence only");
    EN.put("올릴 수 있는 영향력 옵션이 없습니다(잠긴 쪽)", "No influenced modifier can be upgraded (locked side)");
    EN.put("옵션이 4개 이상인 레어에만 쓸 수 있습니다", "Rare items with four or more modifiers only");
    EN.put("이 목걸이에 붙을 섬뜩한 고정 속성이 없습니다", "No Eldritch implicit can roll on this amulet");
    EN.put("이 아이템 종류에 쓸 수 없는 에센스입니다", "This essence cannot be used on this item class");
    EN.put("이 아이템 종류엔 그 영향력이 없습니다", "This item class has no such influence");
    EN.put("이 아이템 종류엔 부패 에센스 옵션이 없습니다", "This item class has no corrupted essence modifier");
    EN.put("이 아이템 종류엔 쓸 수 없는 수확 제작입니다", "This harvest craft cannot be used on this item class");
    EN.put("이 아이템 종류의 작업대 옵션이 아닙니다", "Not a bench craft for this item class");
    EN.put("이 아이템 풀의 옵션이 아닙니다", "Not a modifier of this item's pool");
    EN.put(
        "이 아이템에 바꿀 수 있는 다른 능력치 옵션이 없습니다",
        "No other attribute modifier on this item can be changed");
    EN.put("이 아이템에 붙을 수 있는 고정 속성이 없습니다(종류)", "No implicit can roll on this item (class)");
    EN.put(
        "이 아이템에 붙을 악명 높은 속성이 없습니다(종류 · 아이템 레벨 68)",
        "No Notorious modifier can roll on this item (class · item level 68)");
    EN.put("이미 분열된 아이템입니다", "The item is already fractured");
    EN.put("이미 섬뜩한 고정 속성이 있습니다", "The item already has Eldritch implicits");
    EN.put("이미 영향력이 있는 아이템입니다", "The item already has an influence");
    EN.put("이미 일반 아이템입니다", "The item is already normal");
    EN.put("일반 · 레어 아이템에만 쓸 수 있습니다", "Normal or rare items only");
    EN.put(
        "일반 아이템에만 쓸 수 있는 에센스입니다(레어 재련은 비명 등급부터)",
        "Normal items only for this essence (reforging a rare needs Screaming or higher)");
    EN.put("레어 재련은 비명 등급부터", "Reforging a rare needs Screaming or higher");
    EN.put("일반 아이템에만 쓸 수 있습니다", "Normal items only");
    EN.put(
        "일반(연금술 공명기) · 레어(혼돈 공명기)에만 쓸 수 있습니다",
        "Normal (Alchemical Resonator) or rare (Chaotic Resonator) items only");
    EN.put(
        "작열의 총주교 · 세계 포식자 우세가 없습니다(고정 속성 등급이 높은 쪽이 우세)",
        "Neither the Searing Exarch nor the Eater of Worlds is dominant (the side with the higher implicit tier"
            + " dominates)");
    EN.put(
        "작열의 총주교 고정 속성과 세계 포식자 고정 속성이 모두 있는 아이템에만 사용할 수 있습니다",
        "Only items with both a Searing Exarch and an Eater of Worlds implicit");
    EN.put(
        "작열의 총주교 속성 또는 세계 포식자 속성이 부여된 아이템에는 영향력을 추가할 수 없습니다",
        "Cannot add an influence to items with Searing Exarch or Eater of Worlds modifiers");
    EN.put("장막 옵션이 들어갈 자리가 없습니다", "No room for a veiled modifier");
    EN.put("장막 옵션이 없습니다", "The item has no veiled modifier");
    EN.put("장비에만 쓸 수 있습니다", "Equipment only");
    EN.put("접두 자리가 없습니다", "No open prefix");
    EN.put("접미 자리가 없습니다", "No open suffix");
    EN.put("작업대 옵션이 없습니다", "The item has no crafted modifier");
    EN.put(
        "지울 수 있는 다른 영향력 옵션이 없습니다(분열 · 잠긴 쪽)",
        "No other influenced modifier can be removed (fractured · locked side)");
    EN.put("지울 수 있는 옵션이 없습니다", "No modifier can be removed");
    EN.put(
        "지울 수 있는 옵션이 없습니다(분열 · 잠긴 쪽 · 금지 메타)",
        "No modifier can be removed (fractured · locked side · meta craft)");
    EN.put("타락한 반지 · 목걸이 · 허리띠에만 쓸 수 있습니다", "Corrupted rings, amulets and belts only");
    EN.put("타락한 희귀 아이템에만 쓸 수 있습니다", "Corrupted rare items only");
    EN.put("판테온 위상 데이터가 없습니다", "No pantheon aspect data");
    EN.put("플라스크에는 쓸 수 없습니다", "Cannot be used on flasks");
    EN.put("화석은 1~4개(공명기 칸 수)", "1 to 4 fossils (resonator sockets)");
    EN.put("화폐 등급(1 하급 ~ 4 특출난)을 고르세요", "Choose a currency tier (1 Lesser ~ 4 Exceptional)");
    EN.put("희귀 심연 주얼에만 쓸 수 있습니다", "Rare Abyss Jewels only");
    EN.put("희귀 아이템에만 쓸 수 있습니다", "Rare items only");
    EN.put("희귀 아이템에만 적용", "Rare items only");
    EN.put("희귀 허리띠에만 쓸 수 있습니다", "Rare belts only");
  }

  private record Rule(Pattern ko, String en, boolean element) {
    Rule(Pattern ko, String en) {
      this(ko, en, false);
    }
  }

  // 원소 이름 한국어 → 영어(10-24 Q129 — 엔진 사유가 '냉기 저항 옵션이 없습니다' 처럼 한국어 원소를 싣는다, 영문이 오면 그대로)
  private static final Map<String, String> ELEMENT_EN =
      Map.of("화염", "Fire", "냉기", "Cold", "번개", "Lightning", "카오스", "Chaos", "물리", "Physical");

  // 값이 끼는 문구
  private static final List<Rule> RULES =
      List.of(
          new Rule(
              Pattern.compile("^아이템 레벨 (\\d+) 이하에만 쓸 수 있는 에센스입니다$"),
              "Item level $1 or lower only for this essence"),
          new Rule(
              Pattern.compile("^아이템 레벨 (\\d+) 이상이어야 붙는 옵션입니다$"),
              "This modifier needs item level $1 or higher"),
          new Rule(Pattern.compile("^모르는 화석: (.*)$"), "Unknown fossil: $1"),
          // 화석 이름은 한국어라 빼고(C560)
          new Rule(
              Pattern.compile("^.+ 은\\(는\\) 이 아이템 종류에 쓸 수 없습니다$"),
              "This fossil cannot be used on this item class"),
          new Rule(Pattern.compile("^(\\S+) 피해 옵션이 없습니다$"), "No $1 damage modifier", true),
          new Rule(Pattern.compile("^(\\S+) 저항 옵션이 없습니다$"), "No $1 resistance modifier", true),
          new Rule(
              Pattern.compile("^이 아이템에 바꿀 (\\S+) 옵션이 없습니다$"),
              "This item has no $1 modifier to change",
              true));

  private PoeCraftText() {}

  /** 영어 — 모르면 그대로. */
  public static String en(String ko) {
    if (ko == null) {
      return null;
    }
    String hit = EN.get(ko);
    if (hit != null) {
      return hit;
    }
    for (Rule r : RULES) {
      Matcher m = r.ko().matcher(ko);
      if (m.matches()) {
        if (r.element()) {
          String el = m.group(1);
          return r.en().replace("$1", ELEMENT_EN.getOrDefault(el, el));
        }
        return m.replaceAll(r.en());
      }
    }
    return ko;
  }

  /** 꺼진 이유 맵의 값을 영어로. */
  public static Map<String, String> en(Map<String, String> blocked) {
    if (blocked == null) {
      return Map.of();
    }
    Map<String, String> out = new LinkedHashMap<>();
    blocked.forEach((k, v) -> out.put(k, en(v)));
    return out;
  }

  /** 아는 문구 수(시험용). */
  static int size() {
    return EN.size();
  }
}
