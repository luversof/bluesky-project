package net.luversof.web.gate.poe;

import java.util.List;

import org.springframework.context.i18n.LocaleContextHolder;

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

  /** 로케일에 맞는 라인 목록 (한국어면 ko, 없으면 en 폴백) */
  public static List<String> lines(List<String> ko, List<String> en) {
    return isKorean() && ko != null && !ko.isEmpty() ? ko : en;
  }

  /** 변동 수치(숫자·범위·%·+/-) 토큰 — 예: +25%, (80-120), 30~50, 1.15, +2 */
  private static final java.util.regex.Pattern VALUE_TOKEN =
      java.util.regex.Pattern.compile(
          "[+\\-]?\\(?\\d+(?:\\.\\d+)?(?:\\s*[-~–]\\s*\\d+(?:\\.\\d+)?)?\\)?%?");

  /**
   * 모드 라인의 변동 수치를 흰색으로 강조한 안전 HTML 문자열. HTML 이스케이프 후 숫자 토큰만 흰색 span 으로 감싼다. JTE 에서 {@code
   * $unsafe{PoeText.highlightValues(line)}} 로 쓴다.
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
                "<span class=\"text-white\">"
                    + java.util.regex.Matcher.quoteReplacement(match.group())
                    + "</span>");
  }
}
