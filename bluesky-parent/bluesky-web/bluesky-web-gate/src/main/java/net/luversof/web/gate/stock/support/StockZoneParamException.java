package net.luversof.web.gate.stock.support;

/**
 * 주소에서 받은 <b>타임존 문자열</b>을 읽지 못했을 때. 어느 값이 문제였는지 이름을 들고 다닌다.
 *
 * <p>실측 2026-09-11({@code ?timeZone=Not/AZone} 으로 다섯 화면): 자산성장만 "입력한 값(기간·필터 등)을 확인해 주세요" 를 띄웠고 나머지
 * 넷은 <b>아무 말 없이 그려졌다</b>. 게이트가 알 수 없는 존을 조용히 서버 존으로 바꿔 쓰면서, 원문은 그대로 api-stock 에 넘겼기 때문이다.
 *
 * <p>조용한 폴백은 값을 틀리게 만든다 &mdash; 일자 경계가 옮겨간다(실측: 같은 요청의 {@code UTC} 응답은 {@code Asia/Seoul} 과 달랐다).
 * granularity · breakdown · 날짜 파라미터와 같은 규칙으로, 값이 없으면 종전 기본값이고 값을 줬는데 모르는 값이면 무엇이 문제인지 말한다.
 */
public class StockZoneParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  public StockZoneParamException(String name, String value, Throwable cause) {
    super("time zone parameter cannot be read: " + name + "=" + value, cause);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
