package net.luversof.api.stock.web.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 조회 기준(groupBy)의 기본값이 빈 입력에 지워지지 않는지 고정한다.
 *
 * <p>실측: {@code /api/tradeProfit/calculateProfit?groupBy=} 는 빈 문자열이 {@code null} 로 바인딩되면서 선언된 기본값
 * {@code ACCOUNT_AND_STOCKITEM} 을 덮었고, 서비스의 {@code switch} 가 {@code NullPointerException} 을 던졌다. 그
 * 예외는 공통 처리로 떨어져 별표 {@code Accept} 에서 200 · 본문 0 바이트로 나갔다 — 호출자는 "데이터 없음" 과 구분할 수 없다.
 *
 * <p>파라미터를 아예 안 주는 것과 빈 값으로 주는 것은 같은 뜻이므로 둘 다 기본값이어야 한다.
 */
class TradeProfitRequestGroupDefaultTest {

  @Test
  void 값을_주지_않으면_계좌_종목_기준이다() {
    assertEquals(
        TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM, new TradeProfitRequest().getGroupBy());
  }

  @Test
  void 빈_값이_기본값을_지우지_않는다() {
    var request = new TradeProfitRequest();
    request.setGroupBy(null);
    assertEquals(TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM, request.getGroupBy());
  }

  @Test
  void 생성자에_널을_줘도_기본값이_남는다() {
    var request = new TradeProfitRequest(null, null, null, null, null, null);
    assertEquals(TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM, request.getGroupBy());
  }

  @Test
  void 명시한_값은_그대로_쓴다() {
    var request = new TradeProfitRequest();
    request.setGroupBy(TradeProfitRequestGroup.STOCKITEM);
    assertEquals(TradeProfitRequestGroup.STOCKITEM, request.getGroupBy());
  }
}
