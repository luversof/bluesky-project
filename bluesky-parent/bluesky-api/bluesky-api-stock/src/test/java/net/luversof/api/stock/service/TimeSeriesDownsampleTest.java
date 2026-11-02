package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/**
 * 시계열 주 · 달 줄이기 - 시험이 DAILY 와 짧은 AUTO 만 써서 이 갈래를 보는 시험이 없었다(13차 검토 2026-10-31). 상태값은 구간 마지막 날 값,
 * 흐름값은 구간 합이다.
 */
class TimeSeriesDownsampleTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static TradeProfitTimeSeriesPoint point(
      String date, String holdingsValue, String dailyRealized, long tradeCount, long volume) {
    LocalDate day = LocalDate.parse(date);
    return new TradeProfitTimeSeriesPoint(
        day.atStartOfDay(ZoneOffset.UTC).toInstant(),
        BigDecimal.ZERO,
        dailyRealized == null ? null : new BigDecimal(dailyRealized),
        tradeCount,
        tradeCount,
        volume,
        new BigDecimal(holdingsValue),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        day,
        BigDecimal.ZERO,
        BigDecimal.ZERO);
  }

  @Test
  void 연말을_넘는_ISO_주는_한_구간이고_흐름값은_더하고_상태값은_마지막_날() {
    var weekly =
        TradeProfitService.downsampleWeekly(
            List.of(
                point("2026-12-28", "100", "10", 1, 5), // 월
                point("2026-12-31", "110", null, 2, 0),
                point("2027-01-01", "120", "-3", 0, 7), // 같은 ISO 주(2026-W53)
                // 일요일 - ISO(월요일 시작)면 12-28 주, 일요일 시작 주면 01-04 쪽으로 간다.
                point("2027-01-03", "125", "2", 1, 1),
                point("2027-01-04", "130", "1", 1, 1)), // 다음 주 월요일
            KST);

    assertThat(weekly).hasSize(2);
    var first = weekly.get(0);
    assertThat(first.date()).isEqualTo(LocalDate.parse("2027-01-03"));
    assertThat(first.totalHoldingsValue()).isEqualByComparingTo("125");
    assertThat(first.dailyRealizedProfit()).as("null 은 0 으로 셈").isEqualByComparingTo("9");
    assertThat(first.tradeCount()).isEqualTo(4);
    assertThat(first.tradeVolume()).isEqualTo(13);
    assertThat(weekly.get(1).date()).isEqualTo(LocalDate.parse("2027-01-04"));
  }

  @Test
  void 달은_말일과_다음_달_1일을_나눈다() {
    var monthly =
        TradeProfitService.downsampleMonthly(
            List.of(
                point("2026-01-30", "100", "1", 1, 1),
                point("2026-01-31", "101", "2", 1, 1),
                point("2026-02-01", "102", "4", 1, 1)),
            KST);

    assertThat(monthly)
        .extracting(TradeProfitTimeSeriesPoint::date)
        .containsExactly(LocalDate.parse("2026-01-31"), LocalDate.parse("2026-02-01"));
    assertThat(monthly.get(0).dailyRealizedProfit()).isEqualByComparingTo("3");
  }
}
