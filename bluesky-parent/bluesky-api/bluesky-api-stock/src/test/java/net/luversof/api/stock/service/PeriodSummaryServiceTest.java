package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.PeriodDividendSummary;
import net.luversof.api.stock.domain.PeriodTradeSummary;
import net.luversof.api.stock.web.dto.response.PeriodSummary;

/**
 * 기간 매매·배당 합계.
 *
 * <p>대시보드는 전기간 합계만 보여 주고 "올해 얼마 벌었나" 는 화면 네 곳에 흩어져 있었다. 그 값을 얻자고 원장을 통째로 내려받으면 응답이 원장 크기를 따라간다 - 실측
 * 2026-09-10(로컬 api-stock, 올해 구간): 매매 목록 33,160 바이트, 배당 목록 48,577 바이트인데 이 집계는 268 바이트다.
 *
 * <p>기간에 자료가 없으면 집계 쿼리가 null 을 돌려줄 수 있다. 화면이 "0" 과 "못 가져왔다" 를 구분할 필요는 없으므로 0 으로 채운다 - 여기서 그것을 못
 * 박는다.
 */
class PeriodSummaryServiceTest {

  private static final UUID USER = UUID.randomUUID();
  private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-09-11T00:00:00Z");

  private static PeriodSummaryService service(
      PeriodTradeSummary trade, PeriodDividendSummary dividend) {
    PeriodSummaryService target = new PeriodSummaryService();
    net.luversof.api.stock.repository.TradeRepository tradeRepository =
        org.mockito.Mockito.mock(net.luversof.api.stock.repository.TradeRepository.class);
    net.luversof.api.stock.repository.DividendRepository dividendRepository =
        org.mockito.Mockito.mock(net.luversof.api.stock.repository.DividendRepository.class);
    org.mockito.Mockito.when(
            tradeRepository.findPeriodSummary(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(trade);
    org.mockito.Mockito.when(
            dividendRepository.findPeriodSummary(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(dividend);
    target.setTradeRepository(tradeRepository);
    target.setDividendRepository(dividendRepository);
    return target;
  }

  @Test
  void 배당_실수령은_세전에서_세금과_수수료를_뺀_값이다() {
    PeriodSummary summary =
        service(
                new PeriodTradeSummary(
                    new BigDecimal("100"),
                    new BigDecimal("200"),
                    new BigDecimal("3"),
                    new BigDecimal("4"),
                    new BigDecimal("50"),
                    9L,
                    1L),
                new PeriodDividendSummary(
                    new BigDecimal("1000"),
                    new BigDecimal("300"),
                    new BigDecimal("40"),
                    new BigDecimal("5"),
                    7L))
            .findPeriodSummary(USER, FROM, TO);

    assertThat(summary.buyAmount()).isEqualByComparingTo("100");
    assertThat(summary.sellAmount()).isEqualByComparingTo("200");
    assertThat(summary.buyCount()).isEqualTo(9L);
    assertThat(summary.sellCount()).isEqualTo(1L);
    assertThat(summary.dividendNet()).as("세전 1000 - 세금 40 - 수수료 5").isEqualByComparingTo("955");
    assertThat(summary.dividendCount()).isEqualTo(7L);
  }

  @Test
  void 기간에_자료가_없으면_0_으로_채운다() {
    PeriodSummary summary = service(null, null).findPeriodSummary(USER, FROM, TO);

    assertThat(summary.buyAmount()).isEqualByComparingTo("0");
    assertThat(summary.sellAmount()).isEqualByComparingTo("0");
    assertThat(summary.realizedProfit()).isEqualByComparingTo("0");
    assertThat(summary.dividendNet()).isEqualByComparingTo("0");
    assertThat(summary.buyCount()).isZero();
    assertThat(summary.dividendCount()).isZero();
  }
}
