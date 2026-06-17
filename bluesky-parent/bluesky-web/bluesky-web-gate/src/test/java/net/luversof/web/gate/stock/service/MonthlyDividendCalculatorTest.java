package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView;

class MonthlyDividendCalculatorTest {

  private final MonthlyDividendCalculator calculator = new MonthlyDividendCalculator();

  @Test
  void referenceSummary_emptyPayouts_returnsZeros() {
    MonthlyDividendReferenceSummaryView summary =
        calculator.buildReferenceSummary("069500", List.of());

    assertThat(summary.payoutCount()).isZero();
    assertThat(summary.latestDividendAmountPerShare()).isEqualByComparingTo("0");
    assertThat(summary.averageDividendAmountPerShare1y()).isEqualByComparingTo("0");
    assertThat(summary.averageTaxableBaseRatio1y()).isEqualByComparingTo("0");
    assertThat(summary.latestRecordDate()).isNull();
    assertThat(summary.latestPayDate()).isNull();
  }

  @Test
  void referenceSummary_computesLatestAverageAndTaxableRatio() {
    // 최신 행이 목록의 첫 번째. 평균/과세표준비율은 최근 12건(여기선 2건) 기준.
    var payouts =
        List.of(
            payout("100", "20", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10)),
            payout("200", "0", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)));

    MonthlyDividendReferenceSummaryView summary = calculator.buildReferenceSummary("o", payouts);

    assertThat(summary.stockItemSymbol()).isEqualTo("O"); // trim + uppercase
    assertThat(summary.payoutCount()).isEqualTo(2);
    assertThat(summary.latestDividendAmountPerShare()).isEqualByComparingTo("100");
    assertThat(summary.averageDividendAmountPerShare1y())
        .isEqualByComparingTo("150"); // (100+200)/2
    assertThat(summary.averageTaxableBaseRatio1y()).isEqualByComparingTo("10"); // (20% + 0%)/2
    assertThat(summary.latestRecordDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(summary.latestPayDate()).isEqualTo(LocalDate.of(2026, 2, 10));
  }

  @Test
  void simulatorSummary_aggregatesTotalsAndPicksBestChoice() {
    var rows =
        List.of(
            snapshot("A", 10, "100", "1000", "120", "20000", "12", "5"),
            snapshot("B", 5, "200", "2000", "80", "15000", "8", "9"));

    MonthlyDividendSimulatorSummaryView summary = calculator.buildSimulatorSummary(rows);

    assertThat(summary.itemCount()).isEqualTo(2);
    assertThat(summary.totalLatestMonthlyDividend()).isEqualByComparingTo("2000"); // 100*10 + 200*5
    assertThat(summary.totalBuyAmount()).isEqualByComparingTo("20000"); // 1000*10 + 2000*5
    assertThat(summary.totalExpectedMonthlyDividend()).isEqualByComparingTo("200"); // 120 + 80
    assertThat(summary.totalExpectedAnnualDividend()).isEqualByComparingTo("2400"); // 200 * 12
    assertThat(summary.totalCurrentMarketValue()).isEqualByComparingTo("35000");
    assertThat(summary.portfolioExpectedAnnualYieldPct())
        .isEqualByComparingTo("6.86"); // 2400/35000
    assertThat(summary.bestChoice()).isNotNull();
    assertThat(summary.bestChoice().stockItemSymbol()).isEqualTo("B"); // higher combined return
  }

  private static MonthlyDividendPayoutResponse payout(
      String dividendPerShare,
      String taxableBasePerShare,
      LocalDate recordDate,
      LocalDate payDate) {
    return new MonthlyDividendPayoutResponse(
        null,
        null,
        null,
        null,
        recordDate,
        payDate,
        null,
        new BigDecimal(dividendPerShare),
        new BigDecimal(taxableBasePerShare),
        null);
  }

  private static MonthlyDividendSnapshotResponse snapshot(
      String symbol,
      int heldQuantity,
      String latestMonthlyPerShare,
      String averageBuyPrice,
      String expectedMonthlyDividend,
      String currentMarketValue,
      String expectedTaxableBaseAmount,
      String expectedCombinedReturnPct) {
    return new MonthlyDividendSnapshotResponse(
        null,
        null,
        null,
        symbol,
        null,
        null,
        new BigDecimal(latestMonthlyPerShare),
        null,
        null,
        heldQuantity,
        new BigDecimal(averageBuyPrice),
        null,
        new BigDecimal(currentMarketValue),
        new BigDecimal(expectedMonthlyDividend),
        null,
        null,
        null,
        null,
        new BigDecimal(expectedTaxableBaseAmount),
        null,
        new BigDecimal(expectedCombinedReturnPct),
        null);
  }
}
