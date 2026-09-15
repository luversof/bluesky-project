package net.luversof.web.gate.stock.util;

import java.util.List;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 목록에서 상세로 가는 링크의 주소.
 *
 * <p>실측 2026-09-14: 목록의 종목·계좌 링크 <b>1,027 개가 전부</b> {@code /stock/item?stockItemId=...} 만 적고 있었다.
 * 기간은 브라우저 저장값이 받쳐 주므로 누른 사람 화면에서는 이어져 보이지만, <b>그 주소를 공유하면</b> 받는 쪽에는 다른 기간이 열린다 &mdash; 같은 이유로 상세
 * 화면의 전환기({@link StockTabLinkUtil#switchHref})와 배당 캘린더의 달 이동은 이미 고쳤다. 한 앱 안에서 규칙이 갈라져 있었다.
 *
 * <p>들고 가는 것은 <b>기간 묶음과 보기 설정</b>뿐이다. 쪽·정렬·목록 필터는 그 목록의 것이지 상세의 것이 아니다 &mdash; 상세로 넘기면 엉뚱한 범위를 좁히거나
 * 무시된 채 주소만 지저분해진다.
 *
 * <p>질의는 풀었다 다시 묶지 않고 {@code &} 로 잘라 조각째 옮긴다 &mdash; 다시 묶으면 인코딩이 바뀐다({@code
 * timeZone=Asia%2FSeoul}).
 *
 * <p>지금 주소는 요청에서 읽는다. 템플릿이 이미 {@code LocaleContextHolder} 를 쓰는 것과 같은 방식이라, 조각마다 질의를 파라미터로 들고 다니지
 * 않아도 된다(조각 26 곳이 대상이다).
 */
public final class StockDetailLinkUtil {

  /** 기간 세 키는 한 묶음이고, 시간대·로케일은 같은 화면을 여는 데 필요하다. */
  private static final List<String> CARRIED =
      List.of("startDate", "endDate", "rangeMode", "timeZone", "locale");

  private StockDetailLinkUtil() {}

  /** 종목 상세 주소. */
  public static String item(Object stockItemId) {
    return detailHref("/stock/item", currentQuery(), "stockItemId", stockItemId);
  }

  /** 계좌 상세 주소. */
  public static String account(Object accountId) {
    return detailHref("/stock/account", currentQuery(), "accountId", accountId);
  }

  /**
   * @param currentQuery 지금 주소의 질의 문자열, 없으면 null
   * @param value 대상 아이디. 비어 있으면 아예 적지 않는다
   */
  static String detailHref(String basePath, String currentQuery, String paramName, Object value) {
    StringBuilder sb = new StringBuilder();
    if (currentQuery != null && !currentQuery.isBlank()) {
      for (String part : currentQuery.split("&")) {
        if (part.isEmpty() || !CARRIED.contains(nameOf(part))) {
          continue;
        }
        sb.append(sb.length() == 0 ? "" : "&").append(part);
      }
    }
    String id = value == null ? "" : String.valueOf(value).trim();
    if (!id.isEmpty()) {
      sb.append(sb.length() == 0 ? "" : "&").append(paramName).append('=').append(id);
    }
    return sb.length() == 0 ? basePath : basePath + "?" + sb;
  }

  private static String nameOf(String part) {
    int eq = part.indexOf('=');
    return eq < 0 ? part : part.substring(0, eq);
  }

  /** 조각은 htmx 요청으로도 그려진다 - 그때도 기간은 그 요청의 질의에 실려 온다. */
  private static String currentQuery() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      return servletAttributes.getRequest().getQueryString();
    }
    return null;
  }
}
