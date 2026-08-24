package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendView;

/**
 * 배당 화면의 기간 일수 계산을 고정한다.
 *
 * <p>이 일수는 <b>일평균 원금의 분모</b>이고 연 환산 수익률의 기준이다. 하루만 어긋나도 수익률이 흔들린다.
 *
 * <p>특히 종료일 처리에 주의한다 &mdash; 게이트는 종료를 "그 다음 날 00:00"(배타적)으로 실어 보내므로, 날짜만 떼면 하루가 더 세어진다.
 */
class DividendPeriodDayCountTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Test
  void 배타적_종료시각은_전날로_바뀐다() {
    // 2026-08-23 00:00 KST 로 들어오면 실제 마지막 날은 2026-08-22 다.
    Instant exclusiveEnd = LocalDate.of(2026, 8, 23).atStartOfDay(KST).toInstant();
    assertThat(StockDividendHtmxController.resolvePeriodEndDate(exclusiveEnd, null, KST))
        .isEqualTo(LocalDate.of(2026, 8, 22));
  }

  @Test
  void 종료시각이_없으면_대체값을_쓴다() {
    LocalDate fallback = LocalDate.of(2026, 1, 31);
    assertThat(StockDividendHtmxController.resolvePeriodEndDate(null, fallback, KST))
        .isEqualTo(fallback);
    assertThat(StockDividendHtmxController.resolvePeriodEndDate(null, null, KST)).isNull();
  }

  @Test
  void 하루짜리_기간은_1일이다() {
    Map<Integer, Long> counts =
        StockDividendHtmxController.buildPeriodDayCountsByYear(
            LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 22));
    assertThat(counts).containsExactly(Map.entry(2026, 1L));
  }

  @Test
  void 연도를_넘으면_연도별로_나뉜다() {
    Map<Integer, Long> counts =
        StockDividendHtmxController.buildPeriodDayCountsByYear(
            LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2));
    assertThat(counts).containsExactly(Map.entry(2025, 2L), Map.entry(2026, 2L));
    assertThat(counts.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(4);
  }

  /** 2028 은 윤년이라 366 일이다. */
  @Test
  void 윤년은_366일이다() {
    Map<Integer, Long> counts =
        StockDividendHtmxController.buildPeriodDayCountsByYear(
            LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));
    assertThat(counts).containsExactly(Map.entry(2028, 366L));
    assertThat(
            StockDividendHtmxController.buildPeriodDayCountsByYear(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
        .containsExactly(Map.entry(2026, 365L));
  }

  @Test
  void 역전되거나_비어있는_기간은_빈_결과다() {
    assertThat(
            StockDividendHtmxController.buildPeriodDayCountsByYear(
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 21)))
        .isEmpty();
    assertThat(StockDividendHtmxController.buildPeriodDayCountsByYear(null, LocalDate.now()))
        .isEmpty();
    assertThat(StockDividendHtmxController.buildPeriodDayCountsByYear(LocalDate.now(), null))
        .isEmpty();
  }

  /**
   * 배타적 종료를 그대로 날짜로 떼면 하루가 더 세어진다는 것.
   *
   * <p>전체 기간(약 15년)이면 분모가 하루만큼 커지는 정도지만, 한 달 구간에서는 3.3% 차이가 된다.
   */
  @Test
  void 나노초를_빼지_않으면_하루가_더_세어진다() {
    Instant exclusiveEnd = LocalDate.of(2026, 9, 1).atStartOfDay(KST).toInstant();
    LocalDate correct = StockDividendHtmxController.resolvePeriodEndDate(exclusiveEnd, null, KST);
    LocalDate naive = exclusiveEnd.atZone(KST).toLocalDate();

    long correctDays =
        StockDividendHtmxController.buildPeriodDayCountsByYear(LocalDate.of(2026, 8, 1), correct)
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();
    long naiveDays =
        StockDividendHtmxController.buildPeriodDayCountsByYear(LocalDate.of(2026, 8, 1), naive)
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();

    assertThat(correctDays).isEqualTo(31);
    assertThat(naiveDays).isEqualTo(32);
  }

  @Test
  void 기준일이_없으면_지급일을_쓴다() {
    Instant record = Instant.parse("2026-08-01T00:00:00Z");
    Instant pay = Instant.parse("2026-08-15T00:00:00Z");
    assertThat(StockDividendHtmxController.resolveBasisDate(dividend(record, pay), KST))
        .isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(StockDividendHtmxController.resolveBasisDate(dividend(null, pay), KST))
        .isEqualTo(LocalDate.of(2026, 8, 15));
    assertThat(StockDividendHtmxController.resolveBasisDate(dividend(null, null), KST)).isNull();
  }

  @Test
  void 수량이_없거나_0이면_금액을_만들지_않는다() {
    assertThat(StockDividendHtmxController.multiplyQuantity(new BigDecimal("100"), 3))
        .isEqualByComparingTo("300");
    assertThat(StockDividendHtmxController.multiplyQuantity(new BigDecimal("100"), 0)).isNull();
    assertThat(StockDividendHtmxController.multiplyQuantity(new BigDecimal("100"), null)).isNull();
    assertThat(StockDividendHtmxController.multiplyQuantity(null, 3)).isNull();
  }

  private static DividendView dividend(Instant recordDate, Instant payDate) {
    return new DividendView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "계좌",
        UUID.randomUUID(),
        "종목",
        1,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        BigDecimal.ONE,
        recordDate,
        payDate,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
