package net.luversof.web.gate.poe2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PoE2 제작 엔진(API) 문구의 영어(10-18 C447 UX QA — 영어 화면에 엔진의 한국어 오류 · 꺼진 이유가 섞여 나왔다). 인게임 문구에서 온 것은 게임 영어
 * 원문(ClientStrings), 나머지는 같은 뜻의 영어. 모르는 문구는 그대로 둔다.
 *
 * @author bluesky
 */
public final class Poe2CraftText {

  private static final Map<String, String> EN = new LinkedHashMap<>();

  static {
    // 인게임(ClientStrings 한국어 = 영어 원문)
    EN.put("다른 접두어 속성 부여를 추가할 수 없습니다.", "This item may not have another prefix mod.");
    EN.put("다른 접미어 속성 부여를 추가할 수 없습니다.", "This item may not have another suffix mod.");
    EN.put("속성 한도 도달", "Modifier limit reached");
    EN.put("아이템 레벨이 너무 낮습니다", "Item level is too low");
    EN.put("아이템의 퀄리티가 이미 최대입니다.", "Item already at max quality.");
    EN.put("아이템이 축성됨", "Item is Sanctified");
    EN.put("유효하지 않은 징조 조합입니다.", "This combination of Omens is not valid");
    EN.put("이 훼손 아이템은 징조와 호환되지 않습니다", "Omen is incompatible with this Desecrating item");
    EN.put("이미 훼손된 속성이 있습니다.", "This item already has a Desecrated Modifier.");
    EN.put("재조합에 선택할 수 없는 속성", "Modifier cannot be selected for Recombination");
    EN.put("접두어 속성 한도 도달", "Prefix modifier limit reached");
    EN.put("접미어 속성 한도 도달", "Suffix modifier limit reached");
    EN.put("훼손된 속성은 1개만 선택할 수 있습니다.", "Can only select one Desecrated modifier");
    EN.put("아이템을 재조합기에서 사용할 수 없습니다", "Item cannot be used in the Recombinator");
    EN.put("두 아이템에서 각각 속성을 하나 이상 골라야 합니다", "Must select at least one modifier from each item");
    EN.put("드러낼 훼손된 속성이 없습니다", "This item does not have a Desecrated Modifier.");
    // 엔진 자체 문구
    EN.put("같은 그룹 옵션이 이미 있습니다", "The item already has a modifier of the same group");
    EN.put("같은 종류의 아이템만 재조합할 수 있습니다", "Only items of the same class can be recombined");
    EN.put("그 옵션이 없습니다", "The item does not have that modifier");
    EN.put(
        "그 쪽 옵션이 모두 분열이라 자리를 낼 수 없습니다",
        "All modifiers on that side are fractured — no slot can be freed");
    EN.put("넣을 옵션을 고르세요", "Choose a modifier to add");
    EN.put("드러낼 수 있는 옵션이 없습니다", "No modifier can be revealed");
    EN.put("레어 아이템에만 쓸 수 있습니다", "Rare items only");
    EN.put(
        "레어 아이템에만 쓸 수 있습니다(하나 지우고 보장 옵션 추가)",
        "Rare items only (removes one, adds the guaranteed modifier)");
    EN.put("매직 · 레어 아이템에만 쓸 수 있습니다", "Magic or rare items only");
    EN.put("매직 아이템에만 쓸 수 있습니다", "Magic items only");
    EN.put(
        "매직 아이템에만 쓸 수 있습니다(레어로 바뀌며 보장 옵션 추가)",
        "Magic items only (becomes rare with the guaranteed modifier)");
    EN.put("반지나 목걸이에만 쓸 수 있습니다", "Rings or amulets only");
    EN.put(
        "보장 옵션 자리를 낼 수 없습니다(분열 · 징조 쪽)",
        "No slot can be freed for the guaranteed modifier (fractured / omen side)");
    EN.put(
        "분열할 수 있는 옵션이 없습니다(훼손 옵션 · 징표는 분열되지 않음)",
        "No modifier can be fractured (Desecrated modifiers and the Mark are not fractured)");
    EN.put("셋은 한 번만 다시 볼 수 있습니다", "The three options can be rerolled only once");
    EN.put(
        "심연의 메아리의 징조가 있어야 셋을 다시 볼 수 있습니다",
        "Rerolling the three options needs Omen of Abyssal Echoes");
    EN.put("알 수 없는 기폭제입니다", "Unknown catalyst");
    EN.put("옵션이 4개 이상인 레어에만 쓸 수 있습니다", "Rare items with at least 4 modifiers only");
    EN.put("옵션이 없습니다", "No modifiers");
    EN.put("이 속성은 다른 고른 속성과 호환되지 않습니다", "This modifier conflicts with another selected modifier");
    EN.put("이 아이템 종류에 쓸 수 없는 뼈입니다", "This bone cannot be used on this item class");
    EN.put("이 아이템 종류에 쓸 수 없는 에센스입니다", "This essence cannot be used on this item class");
    EN.put("이 아이템 풀의 옵션이 아닙니다", "Not a modifier of this item's pool");
    EN.put(
        "이 아이템에 붙을 훼손된 속성이 없습니다(종류 · 징조 쪽)",
        "No Desecrated modifier can roll on this item (class / omen side)");
    EN.put("이미 분열된 아이템입니다", "Item has Fractured Mods");
    EN.put("일반 · 매직 아이템에만 쓸 수 있습니다", "Normal or magic items only");
    EN.put("일반 아이템에만 쓸 수 있습니다", "Normal items only");
    EN.put("재료로 쓸 아이템이 둘 있어야 합니다", "Two items are needed");
    EN.put("제시된 셋 중 하나를 고르세요", "Choose one of the three offered");
    EN.put("주얼에만 쓸 수 있습니다", "Jewels only");
    EN.put(
        "지운 자리에 붙을 옵션이 없습니다(아이템 레벨 · 등급 최소 레벨)",
        "No modifier can fill the removed slot (item level / tier minimum level)");
    EN.put("지울 수 있는 옵션이 없습니다(분열 · 징조 쪽)", "No modifier can be removed (fractured / omen side)");
    EN.put("지울 훼손된 속성이 없습니다(빛의 징조)", "No Desecrated modifier to remove (Omen of Light)");
    EN.put("징표 쪽에 붙을 훼손된 속성이 없습니다", "No Desecrated modifier can roll on the Mark's side");
    EN.put("타락한 아이템은 바꿀 수 없습니다", "Corrupted items cannot be modified");
    EN.put(
        "붙을 수 있는 옵션이 없습니다(자리 · 레벨 · 징조 조건)",
        "No modifier can be added (slots / level / omen conditions)");
    EN.put("접두 자리가 없습니다", "No prefix slot");
    EN.put("접미 자리가 없습니다", "No suffix slot");
    // 계산기 · 전략(C448)
    EN.put("목표 옵션을 하나 이상 고르세요", "Pick at least one target modifier");
    EN.put("빈 목표가 있습니다", "A target is empty");
    EN.put("단계를 하나 이상 넣으세요", "Add at least one step");
    EN.put(
        "지금 아이템에서 시작은 레어 아이템일 때만(카오스 반복) 계산합니다",
        "'Start from the current item' is calculated only for rare items (repeated Chaos)");
    EN.put(
        "축성된 아이템은 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)",
        "Sanctified items cannot be modified — not calculated (turn off 'Start from the current item' to use a new base)");
    EN.put(
        "타락한 아이템은 화폐로 바꿀 수 없어 계산하지 않습니다('지금 아이템에서 시작'을 끄면 새 베이스로 계산)",
        "Corrupted items cannot be modified with currency — not calculated (turn off 'Start from the current item' to use a new base)");
  }

  private record Rule(Pattern ko, String en) {}

  // 값이 끼는 문구
  private static final List<Rule> RULES =
      List.of(
          new Rule(Pattern.compile("^모르는 화폐: (.*)$"), "Unknown currency: $1"),
          new Rule(
              Pattern.compile("^아이템 레벨 (\\d+) 이하에만 쓸 수 있는 뼈입니다$"),
              "Item level $1 or lower only for this bone"),
          new Rule(
              Pattern.compile("^아이템 레벨 (\\d+) 이상이어야 붙는 옵션입니다$"),
              "This modifier needs item level $1 or higher"),
          new Rule(
              Pattern.compile(
                  "^다른 접두어 속성 부여를 추가할 수 없습니다\\. 다른 접미어 속성 부여를 추가할 수 없습니다\\. — 접두 · 접미가 각 (\\d+)개로 가득 찼습니다$"),
              "This item may not have another prefix mod. This item may not have another suffix mod. — prefix and suffix slots are full ($1 each)"));

  private Poe2CraftText() {}

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
}
