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
  /**
   * 스냅샷 수량이 원장의 현재 보유와 얼마나 어긋났는지.
   *
   * @param staleCount 수량이 달라진 종목 수
   * @param totalAtCurrentQuantity 1주당 배당(스냅샷)은 그대로 두고 수량만 현재 값으로 바꿔 다시 낸 월배당 합계
   */
  public record CurrentQuantitySummary(long staleCount, BigDecimal totalAtCurrentQuantity) {}

  /**
   * 스냅샷의 보유 수량은 사람이 갱신한 시점의 값이라 원장과 어긋난다(실측 2026-08-23: 8 종목 중 7 종목, 예상 월배당이 1.66% 낮게 잡혔다).
   *
   * <p>1주당 배당은 스냅샷 그대로 두고 <b>수량만</b> 현재 값으로 바꿔 합계를 다시 낸다 &mdash; 스냅샷의 {@code
   * expectedMonthlyDividend} 가 정확히 {@code averageMonthlyDividendPerShare1y x heldQuantity} 임을 실측으로
   * 확인했다(8 건 전부 일치).
   *
   * <p>현재 수량을 알 수 없는 행(보유 목록에 없는 종목 등)은 스냅샷 값을 그대로 더하고 어긋난 것으로 세지 않는다.
   *
   * <p>같은 계산이 요약 화면 안에만 있었다. 월배당 시뮬레이터의 합계 카드도 같은 스냅샷 수량으로 계산되는데 안내가 없어, 행에는 "현재 N 주" 경고가 뜨는데 헤드라인
   * 합계만 조용히 옛 수량 기준이었다.
   */
  public static CurrentQuantitySummary currentQuantitySummary(
      List<MonthlyDividendSnapshotResponse> rows,
      java.util.Map<java.util.UUID, Integer> currentQuantityByStockItem) {
    long staleCount = 0;
    BigDecimal total = BigDecimal.ZERO;
    if (rows == null) {
      return new CurrentQuantitySummary(0, total);
    }
    java.util.Map<java.util.UUID, Integer> quantities =
        currentQuantityByStockItem != null ? currentQuantityByStockItem : java.util.Map.of();
    for (MonthlyDividendSnapshotResponse row : rows) {
      BigDecimal perShare = row.averageMonthlyDividendPerShare1y();
      Integer currentQuantity = quantities.get(row.stockItemId());
      if (perShare == null || row.heldQuantity() == null || currentQuantity == null) {
        total = total.add(safe(row.expectedMonthlyDividend()));
        continue;
      }
      if (row.heldQuantity().intValue() != currentQuantity.intValue()) {
        staleCount++;
      }
      total = total.add(perShare.multiply(BigDecimal.valueOf(currentQuantity)));
    }
    return new CurrentQuantitySummary(staleCount, total);
  }

  public MonthlyDividendSimulatorSummaryView buildSimulatorSummary(
      List<MonthlyDividendSnapshotResponse> rows) {
    return buildSimulatorSummary(rows, java.util.Map.of());
  }

  /**
   * 시뮬레이터 스냅샷 목록의 합계/예상 수익 집계 + 최선 종목 선정. payoutWindowBySymbol(정규화된 심볼 -> MID_MONTH/MONTH_END)이
   * 주어지면 최근 월 배당금을 월중/월말로 나눠 함께 집계한다.
   */
  public MonthlyDividendSimulatorSummaryView buildSimulatorSummary(
      List<MonthlyDividendSnapshotResponse> rows,
      java.util.Map<String, String> payoutWindowBySymbol) {
    java.util.Map<String, String> windowBySymbol =
        payoutWindowBySymbol != null ? payoutWindowBySymbol : java.util.Map.of();
    BigDecimal totalLatestMonthlyDividend = BigDecimal.ZERO;
    BigDecimal totalLatestMonthlyDividendMidMonth = BigDecimal.ZERO;
    BigDecimal totalLatestMonthlyDividendMonthEnd = BigDecimal.ZERO;
    for (MonthlyDividendSnapshotResponse row : rows) {
      BigDecimal rowLatest =
          safe(row.latestMonthlyDividendPerShare())
              .multiply(
                  BigDecimal.valueOf(
                      row.heldQuantity() != null ? row.heldQuantity().longValue() : 0L));
      totalLatestMonthlyDividend = totalLatestMonthlyDividend.add(rowLatest);
      String symbol =
          row.stockItemSymbol() != null
              ? row.stockItemSymbol().trim().toUpperCase(Locale.ROOT)
              : null;
      String window = symbol != null ? windowBySymbol.get(symbol) : null;
      if ("MID_MONTH".equals(window)) {
        totalLatestMonthlyDividendMidMonth = totalLatestMonthlyDividendMidMonth.add(rowLatest);
      } else if ("MONTH_END".equals(window)) {
        totalLatestMonthlyDividendMonthEnd = totalLatestMonthlyDividendMonthEnd.add(rowLatest);
      }
    }
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
        totalLatestMonthlyDividendMidMonth,
        totalLatestMonthlyDividendMonthEnd,
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
