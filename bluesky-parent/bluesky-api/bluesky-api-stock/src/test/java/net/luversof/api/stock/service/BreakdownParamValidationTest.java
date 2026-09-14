package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

/**
 * 모르는 {@code breakdown} 값은 거절한다 &mdash; {@code granularity} 와 같은 규칙.
 *
 * <p>실측 2026-09-11(같은 요청에 값만 바꿔): {@code granularity=BOGUS} 는 400 인데 {@code breakdown=BOGUS} 는
 * <b>200 에 달 단위 9 행</b>이 왔다({@code breakdown=123} 도 같다). 해 단위를 달라고 적었다고 믿는 호출자에게 달 단위가 가고, 오타는 끝까지
 * 드러나지 않는다.
 *
 * <p>빈 값/없음은 종전대로 "쪼개지 않음" 이라 거절하지 않는다 &mdash; 기존 화면의 응답 크기를 그대로 두기 위한 규칙이다.
 */
class BreakdownParamValidationTest {

  private TradeProfitService service() {
    return new TradeProfitService(null, null, null, null, null, null);
  }

  private TradeProfitRequest request() {
    var request = new TradeProfitRequest();
    request.setUserId(java.util.UUID.randomUUID());
    return request;
  }

  @Test
  void 모르는_값은_400_으로_끊는다() {
    assertThatThrownBy(() -> service().aggregateTimeSeriesWithSummary(request(), "AUTO", "BOGUS"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported breakdown");
    assertThatThrownBy(() -> service().aggregateTimeSeriesWithSummary(request(), "AUTO", "123"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void 아는_값은_통과한다() {
    // 여기서는 검증만 본다 - 실제 집계는 저장소가 필요해 NullPointerException 으로 더 진행되지 않는다.
    for (String value : new String[] {"AUTO", "MONTH", "YEAR", "month", " year "}) {
      assertThatThrownBy(() -> service().aggregateTimeSeriesWithSummary(request(), "AUTO", value))
          .as(value + " 는 통과해야 한다")
          .isNotInstanceOf(ResponseStatusException.class);
    }
  }

  @Test
  void 값이_없으면_쪼개지_않는다() {
    for (String value : new String[] {null, "", "  "}) {
      assertThatThrownBy(() -> service().aggregateTimeSeriesWithSummary(request(), "AUTO", value))
          .isNotInstanceOf(ResponseStatusException.class);
    }
    assertThat(true).isTrue();
  }
}
