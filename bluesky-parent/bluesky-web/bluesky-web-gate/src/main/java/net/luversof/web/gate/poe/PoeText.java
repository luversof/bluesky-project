package net.luversof.web.gate.poe;

import java.util.List;

import org.springframework.context.i18n.LocaleContextHolder;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * PoE 화면의 이름/설명을 현재 요청 로케일(한국어/영어)에 맞춰 고르는 헬퍼. 게임 데이터는 한/영을 모두 담고 있어, 한국어 로케일이면 한국어를(없으면 영어로 폴백)
 * 보여준다. JTE 에서 {@code PoeText.name(x.nameKo(), x.name())} 형태로 쓴다.
 */
public final class PoeText {

  private PoeText() {}

  /** 현재 로케일이 한국어인가 */
  public static boolean isKorean() {
    return "ko".equals(LocaleContextHolder.getLocale().getLanguage());
  }

  /** 로케일에 맞는 이름 (한국어면 ko, 비었으면 en 폴백) */
  public static String name(String ko, String en) {
    return isKorean() && ko != null && !ko.isBlank() ? ko : en;
  }

  /**
   * 젬 소모 자원 표기 — 데이터의 costType 은 영문 코드(Mana/Life/ES/ManaPerMinute/ManaPercent)라 한글 로케일에서도 "Mana" 가
   * 그대로 노출됐다(실측: 젬 툴팁 "소모: Mana 29", 진행표 "10 Mana").
   *
   * <p>모르는 값이 오면 원문을 그대로 돌려준다 — 새 자원이 추가돼도 화면이 비지 않는다.
   */
  public static String costType(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String key =
        switch (raw) {
          case "Mana" -> "poe.gems.costtype.mana";
          case "Life" -> "poe.gems.costtype.life";
          case "ES" -> "poe.gems.costtype.es";
          case "ManaPerMinute" -> "poe.gems.costtype.manaperminute";
          case "ManaPercent" -> "poe.gems.costtype.manapercent";
          default -> null;
        };
    return key == null ? raw : MessageUtil.getMessage(key);
  }

  /** 로케일에 맞는 라인 목록 (한국어면 ko, 없으면 en 폴백) */
  public static List<String> lines(List<String> ko, List<String> en) {
    return isKorean() && ko != null && !ko.isEmpty() ? ko : en;
  }

  /**
   * 인게임 인벤토리 칸 크기 {가로, 세로} — 아이템 목록에서 아이콘을 실제 게임 비율로 보여주기 위함. 베이스는 itemClass("Body Armour"…), 유니크는
   * category("body"/"helmet"…) 두 키셋을 모두 수용한다. 유니크 무기 category 는 1손/2손을 구분하지 못해 보편값(2x3)으로 두되 활/지팡이만
   * 2x4 로 처리한다.
   */
  public static int[] invCells(String key) {
    if (key == null) {
      return new int[] {2, 2};
    }
    switch (key) {
      // ── 베이스 itemClass ──
      case "Body Armour":
      case "Shield":
      case "Quiver":
      case "Sceptre":
      case "One Hand Sword":
      case "One Hand Axe":
      case "One Hand Mace":
        return new int[] {2, 3};
      case "Helmet":
      case "Gloves":
      case "Boots":
      case "Claw":
        return new int[] {2, 2};
      case "Belt":
        return new int[] {2, 1};
      case "Amulet":
      case "Ring":
      case "Jewel":
      case "AbyssJewel":
        return new int[] {1, 1};
      case "Dagger":
      case "Rune Dagger":
      case "Wand":
        return new int[] {1, 3};
      case "Thrusting One Hand Sword":
        return new int[] {1, 4};
      case "Two Hand Sword":
      case "Two Hand Axe":
      case "Two Hand Mace":
      case "Staff":
      case "Warstaff":
      case "Bow":
        return new int[] {2, 4};
      case "LifeFlask":
      case "ManaFlask":
      case "HybridFlask":
      case "UtilityFlask":
        return new int[] {1, 2};
      // ── 유니크 category (소문자) ──
      case "body":
      case "shield":
      case "quiver":
      case "axe":
      case "mace":
      case "sword":
      case "sceptre":
      case "fishing":
        return new int[] {2, 3};
      case "helmet":
      case "gloves":
      case "boots":
      case "claw":
        return new int[] {2, 2};
      case "belt":
        return new int[] {2, 1};
      case "amulet":
      case "ring":
      case "jewel":
        return new int[] {1, 1};
      case "dagger":
      case "wand":
        return new int[] {1, 3};
      case "bow":
      case "staff":
        return new int[] {2, 4};
      case "flask":
      case "tincture":
        return new int[] {1, 2};
      default:
        return new int[] {2, 2};
    }
  }

  /** 변동 수치(숫자·범위·%·+/-) 토큰 — 예: +25%, (80-120), 30~50, 1.15, +2 */
  private static final java.util.regex.Pattern VALUE_TOKEN =
      java.util.regex.Pattern.compile(
          "[+\\-]?\\(?\\d+(?:\\.\\d+)?(?:\\s*[-~–]\\s*\\d+(?:\\.\\d+)?)?\\)?%?");

  /**
   * 모드 라인의 변동 수치를 강조(poe-val: 다크 레이어=흰색, 테마 배경=본문 강조색)한 안전 HTML 문자열. HTML 이스케이프 후 숫자 토큰만 흰색 span 으로
   * 감싼다. JTE 에서 {@code $unsafe{PoeText.highlightValues(line)}} 로 쓴다.
   */
  public static String highlightValues(String line) {
    if (line == null || line.isBlank()) {
      return "";
    }
    String escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    return VALUE_TOKEN
        .matcher(escaped)
        .replaceAll(
            match ->
                "<span class=\"poe-val\">"
                    + java.util.regex.Matcher.quoteReplacement(match.group())
                    + "</span>");
  }
}
