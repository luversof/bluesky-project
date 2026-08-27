package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.api.stock.web.dto.response.TradeProfitYearlySummary;

/**
 * 시작일을 주지 않은 조회('전체' 기간)는 첫 지점을 기초로 소비하면 안 된다.
 *
 * <p>그렇게 하면 사용자가 처음 매수한 날의 손익과 원금이 통째로 집계에서 빠진다(실측: 첫날의 손익과 원금이 '전체' 합계에서 누락되고 기초 평가손익이 0 이 아니라
 * +39,400 으로 표시됐다). 기간을 명시하면 시리즈 앞에 값이 0 인 날이 들어와 값이 맞았던 것과 대비된다.
 */
class TradeProfitSummaryOpeningTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private TradeProfitTimeSeriesPoint point(
      LocalDate date, String value, String cost, String realized, String dividend) {
    BigDecimal holdingsValue = new BigDecimal(value);
    BigDecimal holdingsCost = new BigDecimal(cost);
    BigDecimal cumulativeRealized = new BigDecimal(realized);
    BigDecimal cumulativeDividend = new BigDecimal(dividend);
    return new TradeProfitTimeSeriesPoint(
        date.atStartOfDay(KST).toInstant(),
        cumulativeRealized,
        BigDecimal.ZERO,
        0L,
        0L,
        0L,
        holdingsValue,
        holdingsCost,
        holdingsValue.subtract(holdingsCost).add(cumulativeRealized).add(cumulativeDividend),
        cumulativeDividend,
        date);
  }

  /** 첫날 매수(원가 900 -> 평가 1,000), 이후 평가 상승과 추가 매수. */
  private List<TradeProfitTimeSeriesPoint> series() {
    return List.of(
        point(LocalDate.of(2024, 3, 4), "1000", "900", "0", "0"),
        point(LocalDate.of(2024, 3, 5), "1100", "900", "0", "0"),
        point(LocalDate.of(2024, 3, 6), "1250", "1000", "0", "10"));
  }

  @Test
  void 기간을_지정한_조회는_첫_지점을_기초로_쓴다() {
    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series(), KST, false);

    assertEquals(0, new BigDecimal("1000").compareTo(summary.openingValue()));
    assertEquals(0, new BigDecimal("100").compareTo(summary.unrealizedStart()));
    // 기말 누적손익 260 - 기초 누적손익 100
    assertEquals(0, new BigDecimal("160").compareTo(summary.periodProfit()));
    assertEquals(0, new BigDecimal("100").compareTo(summary.principalDelta()));
  }

  @Test
  void 전체_기간_조회는_기초를_0으로_보고_첫날을_포함한다() {
    TradeProfitTimeSeriesSummary summary = TradeProfitService.summarizeSeries(series(), KST, true);

    assertEquals(0, BigDecimal.ZERO.compareTo(summary.openingValue()));
    assertEquals(0, BigDecimal.ZERO.compareTo(summary.unrealizedStart()));
    // 첫날 평가손익 100 이 빠지지 않는다: 기말 누적손익 260 전부가 기간 손익
    assertEquals(0, new BigDecimal("260").compareTo(summary.periodProfit()));
    // 원금도 첫날 매수 900 을 포함한 1,000
    assertEquals(0, new BigDecimal("1000").compareTo(summary.principalDelta()));
  }

  @Test
  void 기초_처리가_달라도_TWR_과_기말값은_같다() {
    TradeProfitTimeSeriesSummary ranged = TradeProfitService.summarizeSeries(series(), KST, false);
    TradeProfitTimeSeriesSummary full = TradeProfitService.summarizeSeries(series(), KST, true);

    // TWR 은 직전 평가액이 있는 날부터 이어 붙이므로 기초 처리와 무관하다.
    assertEquals(ranged.timeWeightedReturnPct(), full.timeWeightedReturnPct());
    assertEquals(0, ranged.closingValue().compareTo(full.closingValue()));
    assertEquals(0, ranged.unrealizedEnd().compareTo(full.unrealizedEnd()));
  }

  @Test
  void 연도별은_첫_해에만_기초를_0으로_보고_이후는_전년말을_기초로_쓴다() {
    List<TradeProfitTimeSeriesPoint> twoYears =
        List.of(
            point(LocalDate.of(2024, 12, 30), "1000", "900", "0", "0"),
            point(LocalDate.of(2024, 12, 31), "1100", "900", "0", "0"),
            point(LocalDate.of(2025, 1, 2), "1300", "900", "0", "0"));

    List<TradeProfitYearlySummary> yearly = TradeProfitService.summarizeByYear(twoYears, KST, true);
    assertEquals(2, yearly.size());

    TradeProfitYearlySummary y2025 = yearly.get(0);
    TradeProfitYearlySummary y2024 = yearly.get(1);
    assertEquals(2025, y2025.year());
    assertEquals(2024, y2024.year());

    // 첫 해(2024)는 기초 0 — 첫날 평가손익 100 이 포함된다(기말 누적손익 200).
    assertEquals(0, BigDecimal.ZERO.compareTo(y2024.summary().openingValue()));
    assertEquals(0, new BigDecimal("200").compareTo(y2024.summary().periodProfit()));

    // 다음 해(2025)는 전년도 마지막 지점(1,100)을 기초로 쓴다.
    assertEquals(0, new BigDecimal("1100").compareTo(y2025.summary().openingValue()));
    assertEquals(0, new BigDecimal("200").compareTo(y2025.summary().periodProfit()));
    assertTrue(y2025.fromDate().getYear() == 2025);
  }
}
