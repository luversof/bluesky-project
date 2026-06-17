package net.luversof.web.gate.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView;
import net.luversof.web.gate.stock.dto.view.MonthlyDividendSimulatorSummaryView;

/** 월배당 기준 데이터(payout)와 시뮬레이터 스냅샷에 대한 순수 집계 계산. 컨트롤러에서 분리해 단위 테스트가 가능하도록 한다. */
@Component
public class MonthlyDividendCalculator {

  /** 최근 12개월 지급 이력 기준 요약(최신/평균 주당 배당, 평균 과세표준 비율). */
  public MonthlyDividendReferenceSummaryView buildReferenceSummary(
      String symbol, List<MonthlyDividendPayoutResponse> payouts) {
    if (!StringUtils.hasText(symbol) || payouts == null || payouts.isEmpty()) {
      return new MonthlyDividendReferenceSummaryView(
          safeString(symbol), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    List<MonthlyDividendPayoutResponse> lastYearRows = payouts.stream().limit(12).toList();
    BigDecimal latestDividendAmountPerShare =
        safe(lastYearRows.get(0).dividendAmountPerShare()).setScale(4, RoundingMode.HALF_UP);
    BigDecimal averageDividendAmountPerShare1y =
        lastYearRows.stream()
            .map(MonthlyDividendPayoutResponse::dividendAmountPerShare)
            .map(MonthlyDividendCalculator::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(lastYearRows.size()), 4, RoundingMode.HALF_UP);

    List<BigDecimal> taxableBaseRatios =
        lastYearRows.stream()
            .filter(row -> safe(row.dividendAmountPerShare()).signum() > 0)
            .map(
                row ->
                    safe(row.taxableBasePerShare())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(safe(row.dividendAmountPerShare()), 2, RoundingMode.HALF_UP))
            .toList();
    BigDecimal averageTaxableBaseRatio1y =
        taxableBaseRatios.isEmpty()
            ? BigDecimal.ZERO
            : taxableBaseRatios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(taxableBaseRatios.size()), 2, RoundingMode.HALF_UP);

    MonthlyDividendPayoutResponse latestRow = lastYearRows.get(0);
    return new MonthlyDividendReferenceSummaryView(
        symbol.trim().toUpperCase(Locale.ROOT),
        payouts.size(),
        latestDividendAmountPerShare,
        averageDividendAmountPerShare1y,
        averageTaxableBaseRatio1y,
        latestRow.recordDate(),
        latestRow.payDate());
  }

  /** 시뮬레이터 스냅샷 목록의 합계/예상 수익 집계 + 최선 종목 선정. */
  public MonthlyDividendSimulatorSummaryView buildSimulatorSummary(
      List<MonthlyDividendSnapshotResponse> rows) {
    BigDecimal totalLatestMonthlyDividend =
        rows.stream()
            .map(
                row ->
                    safe(row.latestMonthlyDividendPerShare())
                        .multiply(
                            BigDecimal.valueOf(
                                row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalBuyAmount =
        rows.stream()
            .map(
                row ->
                    safe(row.averageBuyPrice())
                        .multiply(
                            BigDecimal.valueOf(
                                row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalExpectedMonthlyDividend =
        rows.stream()
            .map(MonthlyDividendSnapshotResponse::expectedMonthlyDividend)
            .map(MonthlyDividendCalculator::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalExpectedAnnualDividend =
        totalExpectedMonthlyDividend.multiply(BigDecimal.valueOf(12));
    BigDecimal totalExpectedTaxableBaseAmount =
        rows.stream()
            .map(MonthlyDividendSnapshotResponse::expectedTaxableBaseAmount)
            .map(MonthlyDividendCalculator::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalExpectedAnnualTaxableBaseAmount =
        totalExpectedTaxableBaseAmount.multiply(BigDecimal.valueOf(12));
    BigDecimal totalCurrentMarketValue =
        rows.stream()
            .map(MonthlyDividendSnapshotResponse::currentMarketValue)
            .map(MonthlyDividendCalculator::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal portfolioExpectedAnnualYieldPct = BigDecimal.ZERO;
    if (totalCurrentMarketValue.signum() > 0) {
      portfolioExpectedAnnualYieldPct =
          totalExpectedAnnualDividend
              .multiply(BigDecimal.valueOf(100))
              .divide(totalCurrentMarketValue, 2, RoundingMode.HALF_UP);
    }

    MonthlyDividendSnapshotResponse bestChoice = resolveBestChoice(rows);
    return new MonthlyDividendSimulatorSummaryView(
        rows.size(),
        totalLatestMonthlyDividend,
        totalExpectedMonthlyDividend,
        totalExpectedAnnualDividend,
        totalExpectedTaxableBaseAmount,
        totalExpectedAnnualTaxableBaseAmount,
        totalBuyAmount,
        totalCurrentMarketValue,
        portfolioExpectedAnnualYieldPct,
        bestChoice);
  }

  private MonthlyDividendSnapshotResponse resolveBestChoice(
      List<MonthlyDividendSnapshotResponse> rows) {
    if (rows == null || rows.isEmpty()) {
      return null;
    }

    Comparator<MonthlyDividendSnapshotResponse> comparator =
        Comparator.comparing(
                (MonthlyDividendSnapshotResponse row) -> safe(row.expectedCombinedReturnPct()))
            .thenComparing(row -> safe(row.expectedAnnualYieldOnCostPct()))
            .thenComparing(row -> safe(row.expectedAnnualYieldPct()))
            .thenComparing(
                (MonthlyDividendSnapshotResponse row) -> safe(row.averageTaxableBaseRatio1y()),
                Comparator.reverseOrder())
            .thenComparing(
                row -> safeString(row.stockItemSymbol()), String.CASE_INSENSITIVE_ORDER.reversed());

    return rows.stream().max(comparator).orElse(null);
  }

  private static BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String safeString(String value) {
    return value != null ? value : "";
  }
}
