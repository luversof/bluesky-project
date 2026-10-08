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
  /** 한국 서버 거래소(옛 poe.game.daum.net 은 poe.kakaogames.com 으로 301, 10-08). */
  public static final String TRADE_HOST = "https://poe.kakaogames.com";

  /** PoE1 거래소 검색 주소 — 리그는 주소에 없다(지금 리그가 기본). 쿼리가 없으면 null. */
  public static String tradeUrl(String query) {
    return query == null || query.isBlank()
        ? null
        : TRADE_HOST
            + "/trade/search?q="
            + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
  }

  /**
   * 고유 이름 거래소 쿼리(10-08 C173, 고유 상세 화면) — API PoeTradeQueries.unique 와 같은 모양(한국 서버라 한국어 이름, 즉시 구입 ·
   * 가격순). 상세 화면은 API 쿼리를 받지 않아(고유 데이터 레코드에 쿼리를 싣지 않는다) 여기서 같은 모양으로 만든다. 이름이 없으면 null.
   */
  public static String uniqueTradeQuery(String nameKo, String name) {
    String n = nameKo != null && !nameKo.isBlank() ? nameKo : name;
    if (n == null || n.isBlank()) {
      return null;
    }
    return "{\"query\":{\"status\":{\"option\":\"securable\"},\"name\":\""
        + n.replace("\\", "").replace("\"", "")
        + "\",\"stats\":[{\"type\":\"and\",\"filters\":[]}]},\"sort\":{\"price\":\"asc\"}}";
  }

  /**
   * 베이스 거래소 쿼리(10-08 C174, 베이스 상세 화면) — 이 베이스의 고유 아닌 아이템(일반 · 마법 · 레어), 즉시 구입 · 가격순. 한국 서버라 한국어 베이스
   * 이름. 이름이 없으면 null.
   */
  public static String baseTradeQuery(String nameKo, String name) {
    String n = nameKo != null && !nameKo.isBlank() ? nameKo : name;
    if (n == null || n.isBlank()) {
      return null;
    }
    return "{\"query\":{\"status\":{\"option\":\"securable\"},\"type\":\""
        + n.replace("\\", "").replace("\"", "")
        + "\",\"stats\":[{\"type\":\"and\",\"filters\":[]}],"
        + "\"filters\":{\"type_filters\":{\"filters\":{\"rarity\":{\"option\":\"nonunique\"}}}}},"
        + "\"sort\":{\"price\":\"asc\"}}";
  }

  /** PoE2 거래소 검색 주소 — 리그가 경로에 들어간다(없으면 Standard). 쿼리가 없으면 null. */
  public static String trade2Url(String league, String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    String l = league == null || league.isBlank() ? "Standard" : league;
    return TRADE_HOST
        + "/trade2/search/poe2/"
        + java.net.URLEncoder.encode(l, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")
        + "?q="
        + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
  }

  private static final java.util.regex.Pattern GEM_LEVEL_SUFFIX =
      java.util.regex.Pattern.compile(" \\(Lv(\\d+)\\)$");

  /**
   * 최적화 결과 보조젬 이름 꼬리 "(Lv16)"(속성 요구치로 낮춘 계산 레벨) → 젬 상세 주소의 "&level=16". 꼬리가 없으면 빈 문자열(상세 기본 20레벨 =
   * 계산과 같음). 예전엔 툴팁이 늘 20레벨이라 낮춘 레벨의 계산과 달랐다(10-05 C155).
   */
  public static String gemLevelParam(String name) {
    if (name == null) {
      return "";
    }
    java.util.regex.Matcher m = GEM_LEVEL_SUFFIX.matcher(name);
    return m.find() ? "&level=" + m.group(1) : "";
  }

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

  /**
   * 옵션 줄 종류 → 인게임 툴팁 글자색 클래스(10-04 C138): 제작(crafted) 연보라 · 분열(fractured) 금색 · 인챈트(enchant) 하늘색, 그
   * 밖엔 마법 파랑. 종류 목록이 줄 수와 다르면(옛 응답) 모두 파랑.
   */
  public static String modKindClass(List<String> kinds, int lineCount, int index) {
    String kind = kinds != null && kinds.size() == lineCount ? kinds.get(index) : "";
    return switch (kind == null ? "" : kind) {
      case "crafted" -> "text-[#b4b4ff]";
      case "fractured" -> "text-[#a29162]";
      case "enchant" -> "text-[#b8daf2]";
      default -> "text-[#8888ff]";
    };
  }

  /** 로케일에 맞는 라인 목록 (한국어면 ko, 없으면 en 폴백) */
  public static List<String> lines(List<String> ko, List<String> en) {
    return isKorean() && ko != null && !ko.isEmpty() ? ko : en;
  }

  /**
   * 줄별 리마인더(인게임 회색 부연)를 data 속성 문자열로 — 줄은 줄바꿈, 한 줄 안 여러 개는 탭, 없는 줄은 빈 칸(10-04 C125, 트리 주얼 칸 툴팁).
   * 없으면 빈 문자열.
   */
  public static String reminderData(List<List<String>> ko, List<List<String>> en) {
    List<List<String>> r = isKorean() && ko != null ? ko : en;
    if (r == null) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < r.size(); i++) {
      if (i > 0) {
        out.append('\n');
      }
      if (r.get(i) != null) {
        // 문구 안 줄바꿈은 공백으로 — 줄 구분자와 겹치면 줄 수가 어긋난다(10-04 C127 검토: 문구 34개가 줄바꿈을 품음)
        out.append(
            String.join(
                "\t", r.get(i).stream().map(x -> x == null ? "" : x.replace('\n', ' ')).toList()));
      }
    }
    return out.toString();
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
