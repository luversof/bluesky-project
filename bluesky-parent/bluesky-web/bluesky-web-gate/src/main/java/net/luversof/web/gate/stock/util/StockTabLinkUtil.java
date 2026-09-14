package net.luversof.web.gate.stock.util;

/**
 * 같은 페이지 안 탭 링크의 주소.
 *
 * <p>탭만 바꾸고 지금 보고 있는 조건(계좌·종목 필터, 기간, 로케일)은 그대로 들고 간다.
 *
 * <p>실측 2026-09-12: 배당 화면의 두 탭은 {@code /stock/dividend?tab=history} 처럼 탭만 적고 있었다. 그래서 {@code
 * ?rangeMode=all&accountIdList=...} 로 들어와 한 계좌만 보던 화면에서 탭을 누르면 상세 목록이 <b>40 건에서 202 건(전체)</b> 으로 늘고
 * 계좌 선택도 비었다 &mdash; 필터가 걸려 있다는 표시는 그대로인데 내용만 전체로 바뀌므로 화면만 봐서는 알 수 없다. 공유받은 링크에서 특히 그렇다.
 *
 * <p>질의는 풀었다가 다시 묶지 않고 {@code &} 로 잘라 그대로 옮긴다 &mdash; 다시 묶으면 인코딩이 바뀌고 같은 이름이 여러 번 오는 값(계좌 목록)의 순서를
 * 건드릴 수 있다.
 */
public final class StockTabLinkUtil {

  private StockTabLinkUtil() {}

  /**
   * @param basePath 탭이 가리키는 경로(예: {@code /stock/dividend})
   * @param currentQuery 지금 주소의 질의 문자열({@code HttpServletRequest#getQueryString()}), 없으면 null
   * @param tabValue 이 탭의 {@code tab} 값
   */
  public static String tabHref(String basePath, String currentQuery, String tabValue) {
    StringBuilder sb = new StringBuilder();
    if (currentQuery != null && !currentQuery.isBlank()) {
      for (String part : currentQuery.split("&")) {
        if (part.isEmpty() || isTabParam(part)) {
          continue;
        }
        sb.append(sb.length() == 0 ? "" : "&").append(part);
      }
    }
    if (tabValue != null && !tabValue.isBlank()) {
      sb.append(sb.length() == 0 ? "" : "&").append("tab=").append(tabValue);
    }
    return sb.length() == 0 ? basePath : basePath + "?" + sb;
  }

  /**
   * 같은 화면에서 <b>대상만</b> 바꾸는 링크의 주소(종목 상세의 다른 종목, 계좌 상세의 다른 계좌).
   *
   * <p>실측 2026-09-13: 이 전환기 링크들은 대상 아이디만 적고 있었다(종목 9개·계좌 6개). 기간은 저장값이 받쳐 주어 화면에서는 그대로 보이지만, <b>그
   * 주소를 공유하면</b> 받는 쪽에는 다른 기간이 열린다 &mdash; 1 년(2025-09-14~2026-09-13)을 보다가 만든 링크가 받는 쪽에서는
   * 전체(2019-12-26~)로 열렸다.
   *
   * @param basePath 대상 화면 경로(예: {@code /stock/item})
   * @param currentQuery 지금 주소의 질의 문자열, 없으면 null
   * @param paramName 바꿀 대상 파라미터 이름(예: {@code stockItemId})
   * @param value 새 대상 값
   */
  public static String switchHref(
      String basePath, String currentQuery, String paramName, String value) {
    StringBuilder sb = new StringBuilder();
    if (currentQuery != null && !currentQuery.isBlank()) {
      for (String part : currentQuery.split("&")) {
        if (part.isEmpty() || isNamed(part, paramName)) {
          continue;
        }
        sb.append(sb.length() == 0 ? "" : "&").append(part);
      }
    }
    if (value != null && !value.isBlank()) {
      sb.append(sb.length() == 0 ? "" : "&").append(paramName).append("=").append(value);
    }
    return sb.length() == 0 ? basePath : basePath + "?" + sb;
  }

  private static boolean isNamed(String part, String paramName) {
    int eq = part.indexOf('=');
    String name = eq < 0 ? part : part.substring(0, eq);
    return name.equals(paramName);
  }

  /** {@code tab} 또는 {@code tab=...} 인 조각. 값이 비어 있어도(=tab=) 옛 탭이므로 버린다. */
  private static boolean isTabParam(String part) {
    int eq = part.indexOf('=');
    String name = eq < 0 ? part : part.substring(0, eq);
    return "tab".equals(name);
  }
}
