package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 기간 손익은 평가손익 변동 + 그 기간의 실현손익 + 그 기간의 배당과 같아야 한다.
 *
 * <p>화면 두 곳이 이 등식에 기대고 있다. 종목 상세는 세 값을 나란히 적고 "더해서 검산할 수 있다"고 말하고, 자산 성장의 기간 손익 카드도 같은 쪼갬을 쓴다. 등식이
 * 깨지면 한 화면 안의 숫자끼리 맞지 않는데, 어느 쪽이 틀렸는지는 화면만 봐서는 알 수 없다.
 *
 * <p>실측 2026-09-12(운영 데이터, 다섯 구간: 올해 · 최근 1년 · 2020년 · 2015~2017 · 전체): 다섯 구간 모두 차이 0 원이었다. 이 테스트는
 * 그 등식을 계산 쪽에 고정한다.
 */
class PeriodProfitIdentityTest {

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

  /**
   * 등식을 검사한다.
   *
   * @param zeroOpening 전체 기간 조회처럼 기초를 0 으로 보는가
   */
  private void assertIdentity(List<TradeProfitTimeSeriesPoint> series, boolean zeroOpening) {
    TradeProfitTimeSeriesSummary summary =
        TradeProfitService.summarizeSeries(series, KST, zeroOpening);
    TradeProfitTimeSeriesPoint first = series.get(0);
    TradeProfitTimeSeriesPoint last = series.get(series.size() - 1);

    BigDecimal realizedInPeriod =
        zeroOpening
            ? last.cumulativeRealizedProfit()
            : last.cumulativeRealizedProfit().subtract(first.cumulativeRealizedProfit());
    BigDecimal dividendInPeriod =
        zeroOpening
            ? last.cumulativeDividend()
            : last.cumulativeDividend().subtract(first.cumulativeDividend());
    BigDecimal unrealizedDelta = summary.unrealizedEnd().subtract(summary.unrealizedStart());

    BigDecimal expected = unrealizedDelta.add(realizedInPeriod).add(dividendInPeriod);
    assertEquals(
        0,
        expected.compareTo(summary.periodProfit()),
        "기간 손익 "
            + summary.periodProfit()
            + " != 평가변동 "
            + unrealizedDelta
            + " + 실현 "
            + realizedInPeriod
            + " + 배당 "
            + dividendInPeriod);
  }

  /** 평가만 오르는 구간. */
  @Test
  void 평가만_움직여도_등식이_성립한다() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(
            point(LocalDate.of(2024, 3, 4), "1000", "900", "0", "0"),
            point(LocalDate.of(2024, 3, 5), "1100", "900", "0", "0"),
            point(LocalDate.of(2024, 3, 6), "1250", "900", "0", "0"));
    assertIdentity(series, false);
    assertIdentity(series, true);
  }

  /** 실현손익과 배당이 함께 늘어나고 원금도 바뀌는 구간. */
  @Test
  void 실현과_배당이_함께_늘어도_등식이_성립한다() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(
            point(LocalDate.of(2024, 3, 4), "1000", "900", "10", "5"),
            point(LocalDate.of(2024, 3, 5), "1400", "1200", "40", "25"),
            point(LocalDate.of(2024, 3, 6), "1350", "1200", "70", "60"));
    assertIdentity(series, false);
    assertIdentity(series, true);
  }

  /** 손실 구간에서도 같다 - 부호가 뒤집혀도 쪼갬은 그대로다. */
  @Test
  void 손실_구간에서도_등식이_성립한다() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(
            point(LocalDate.of(2024, 3, 4), "1000", "900", "0", "0"),
            point(LocalDate.of(2024, 3, 5), "700", "900", "-50", "0"),
            point(LocalDate.of(2024, 3, 6), "650", "900", "-120", "10"));
    assertIdentity(series, false);
    assertIdentity(series, true);
  }

  /** 보유가 0 이 되는 날(전량 매도)도 같다. */
  @Test
  void 전량_매도로_보유가_0이_되어도_등식이_성립한다() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(
            point(LocalDate.of(2024, 3, 4), "1000", "900", "0", "0"),
            point(LocalDate.of(2024, 3, 5), "1200", "900", "0", "0"),
            point(LocalDate.of(2024, 3, 6), "0", "0", "300", "15"));
    assertIdentity(series, false);
    assertIdentity(series, true);
  }

  /** 지점이 하나뿐인 구간(하루 조회)도 같다. */
  @Test
  void 하루짜리_구간에서도_등식이_성립한다() {
    List<TradeProfitTimeSeriesPoint> series =
        List.of(point(LocalDate.of(2024, 3, 4), "1000", "900", "20", "5"));
    assertIdentity(series, false);
    assertIdentity(series, true);
  }
}
