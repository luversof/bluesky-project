package net.luversof.web.gate.poe;

/**
 * 인게임 툴팁 맨 아래 사용 설명(10-09 C212) — 게임 ClientStrings ItemDescription* 문구의 메시지 키.
 *
 * <p>플라스크 "마시려면 우클릭하십시오. …"(ItemDescriptionFlask) · 팅크처(ItemDescriptionTincture) · 주얼(PassiveJewel)
 * · 심연 주얼(AbyssJewel, 베이스 "… Eye Jewel") · 스킬 군 주얼(일반 주얼 문구, C216). 장비는 설명이 없어 null.
 */
public final class PoeItemDescription {

  private PoeItemDescription() {}

  /**
   * PoE2 — 아이템 분류(ItemClasses.Id)로 고른다(10-09 C213). 생명력 · 마나 플라스크 ItemDescriptionFlask("…[우물]에서
   * 충전…"), 부적 (UtilityFlask) ItemDescriptionFlaskUtility1("조건이 충족되면 자동으로 사용됩니다. …"), 주얼
   * PassiveJewel. 그 밖은 null.
   */
  public static String poe2Key(String itemClass) {
    if (itemClass == null) {
      return null;
    }
    return switch (itemClass) {
      case "LifeFlask", "ManaFlask" -> "poe2.items.description.flask";
      case "UtilityFlask" -> "poe2.items.description.charm";
      case "Jewel" -> "poe2.items.description.jewel";
      default -> null;
    };
  }

  public static String key(String category, String baseType) {
    if (category == null) {
      return null;
    }
    String base = baseType == null ? "" : baseType;
    return switch (category) {
      case "flask" -> "poe.items.description.flask";
      case "tincture" -> "poe.items.description.tincture";
      case "jewel" -> {
        if (base.endsWith("Eye Jewel")) {
          yield "poe.items.description.abyss.jewel";
        }
        // 스킬 군 주얼도 일반 주얼 문구(10-09 C216 — poedb 목소리 · Large Cluster Jewel 툴팁이 PassiveJewel 문구.
        // ClientStrings 의
        //   AfflictionJewel* 는 지금 게임이 안 쓴다 — 같은 poedb 가 심연 주얼엔 전용 문구를 보여 줘 분류별로 가려 쓰는 출처임을 확인)
        yield "poe.items.description.jewel";
      }
      default -> null;
    };
  }
}
