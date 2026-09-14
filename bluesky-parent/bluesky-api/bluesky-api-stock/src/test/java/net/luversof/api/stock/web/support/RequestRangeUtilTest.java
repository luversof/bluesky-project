package net.luversof.api.stock.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

/**
 * 뒤집힌 조회 구간은 앞뒤를 바로잡는다.
 *
 * <p>뒤집힌 구간은 어느 질의에도 걸리지 않아 <b>빈 결과</b>가 되고, 그 빈 결과는 화면에서 "자료가 없습니다" 와 구분되지 않는다. 실측 2026-09-12(같은
 * 사용자, 같은 두 날짜를 앞뒤만 바꿔서):
 *
 * <pre>
 *   /api/yearlyCost                        14 행 → 0 행
 *   /api/tradeProfit/calculateProfit       43 행 → 0 행
 *   /api/tradeProfit/timeSeriesWithSummary 10,864 B → 506 B
 *   /api/periodSummary                        275 B → 206 B
 * </pre>
 *
 * <p>게이트는 이미 들머리에서 바로잡아 보내므로 화면에 보이는 증상은 없었다. 계약 쪽을 막아 다음 호출자가 같은 함정을 밟지 않게 한다.
 */
class RequestRangeUtilTest {

  private static final Instant EARLY = Instant.parse("2009-01-01T00:00:00Z");
  private static final Instant LATE = Instant.parse("2026-09-11T00:00:00Z");

  @Test
  void 뒤집혀_있으면_바꾼다() {
    Instant[] range = RequestRangeUtil.ordered(LATE, EARLY);

    assertThat(range[0]).isEqualTo(EARLY);
    assertThat(range[1]).isEqualTo(LATE);
  }

  @Test
  void 바른_순서는_그대로_둔다() {
    Instant[] range = RequestRangeUtil.ordered(EARLY, LATE);

    assertThat(range[0]).isEqualTo(EARLY);
    assertThat(range[1]).isEqualTo(LATE);
  }

  @Test
  void 한쪽이_비면_판단하지_않는다() {
    assertThat(RequestRangeUtil.ordered(null, LATE)).containsExactly(null, LATE);
    assertThat(RequestRangeUtil.ordered(EARLY, null)).containsExactly(EARLY, null);
    assertThat(RequestRangeUtil.ordered(null, null)).containsExactly(null, null);
  }

  @Test
  void 같은_시각은_그대로다() {
    assertThat(RequestRangeUtil.ordered(EARLY, EARLY)).containsExactly(EARLY, EARLY);
  }

  /** 손익 조회 요청도 같은 규칙을 쓴다 - 컨트롤러가 여럿이라 요청 객체 한 곳에서 바로잡는다. */
  @Test
  void 손익_요청도_바로잡는다() {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setStartDate(LATE);
    request.setEndDate(EARLY);

    assertThat(request.getStartDate()).isEqualTo(EARLY);
    assertThat(request.getEndDate()).isEqualTo(LATE);
  }
}
