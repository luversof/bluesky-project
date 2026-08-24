package net.luversof.api.stock.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesResult;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;

/**
 * {@code includeSeries=false} 가 시리즈만 비우고 요약·연도별은 그대로 두는지 고정한다.
 *
 * <p>왜 필요한가(실측, 사용자 실데이터 · {@code granularity=DAILY}): 게이트의 {@code
 * /stock/asset-growth/period-return} 은 이 엔드포인트를 부른 뒤 {@code summary()} 만 꺼내 쓰고 시리즈는 버린다. 전체 기간이면 응답
 * 1,655,289 바이트 중 실제로 쓰이는 부분은 8,420 바이트로 <b>99.5% 가 버려졌다</b>(6,442 포인트). 5 년 99.3%, 1 년 98.4%.
 *
 * <p>기본값은 반드시 {@code true} 여야 한다 — 차트를 그리는 호출자가 파라미터를 주지 않기 때문이다.
 */
class TimeSeriesIncludeSeriesTest {

  private static final TradeProfitTimeSeriesSummary SUMMARY = TradeProfitTimeSeriesSummary.empty();

  /** 시리즈를 실제로 만들지 않고, 컨트롤러가 결과를 어떻게 다시 포장하는지만 본다. */
  private TradeProfitController controllerReturning(TradeProfitTimeSeriesResult result) {
    var controller = new TradeProfitController();
    controller.setStockProfitService(
        new TradeProfitService(null, null, null, null, null, null) {
          @Override
          public TradeProfitTimeSeriesResult aggregateTimeSeriesWithSummary(
              TradeProfitRequest request, String granularity) {
            return result;
          }
        });
    return controller;
  }

  private TradeProfitTimeSeriesResult sample() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(
            new TradeProfitTimeSeriesPoint(
                null, null, null, 0L, 0L, 0L, null, null, null, null, null));
    return new TradeProfitTimeSeriesResult(series, SUMMARY, List.of());
  }

  @Test
  void 기본값은_시리즈를_포함한다() {
    var full = sample();
    var response =
        controllerReturning(full).timeSeriesWithSummary(new TradeProfitRequest(), "DAILY", true);

    assertSame(full, response, "기본 경로는 서비스 결과를 그대로 돌려줘야 한다");
    assertEquals(1, response.series().size());
  }

  @Test
  void 시리즈를_빼면_요약과_연도별은_남는다() {
    var full = sample();
    var response =
        controllerReturning(full).timeSeriesWithSummary(new TradeProfitRequest(), "DAILY", false);

    assertTrue(response.series().isEmpty(), "시리즈는 비어야 한다");
    assertSame(SUMMARY, response.summary(), "요약은 그대로여야 한다");
    assertSame(full.yearly(), response.yearly(), "연도별도 그대로여야 한다");
  }

  @Test
  void 결과가_없으면_그대로_널이다() {
    assertEquals(
        null,
        controllerReturning(null).timeSeriesWithSummary(new TradeProfitRequest(), "DAILY", false));
  }
}
