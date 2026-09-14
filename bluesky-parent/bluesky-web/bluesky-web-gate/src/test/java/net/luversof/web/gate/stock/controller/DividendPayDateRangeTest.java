package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendResponse;

/**
 * The dividend screen already downloads the whole ledger for the monthly chart, so the shown period
 * and the previous period are picked out of it instead of asking the backend once per period.
 *
 * <p>That only holds while this filter matches the backend's, which is {@code payDate >= start AND
 * payDate < end} with rows that have no pay date left out. Measured 2026-09-10 against
 * bluesky-api-stock: this year 116 rows, previous year 56, one month 18, three months 52, a
 * single-day window 1 - every one identical to the server's answer, fields and order included.
 */
class DividendPayDateRangeTest {

  private static final Instant DAY_1 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant DAY_2 = Instant.parse("2026-01-02T00:00:00Z");
  private static final Instant DAY_3 = Instant.parse("2026-01-03T00:00:00Z");

  private static DividendResponse dividend(Instant payDate) {
    return new DividendResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "name",
        "CASH",
        1,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        BigDecimal.ONE,
        payDate,
        payDate);
  }

  @Test
  void includesTheStartAndExcludesTheEnd() {
    var first = dividend(DAY_1);
    var second = dividend(DAY_2);
    var third = dividend(DAY_3);

    var picked =
        StockDividendHtmxController.inPayDateRange(List.of(first, second, third), DAY_1, DAY_3);

    assertThat(picked)
        .as("the backend uses payDate >= start AND payDate < end")
        .containsExactly(first, second);
  }

  @Test
  void leavesOutRowsWithoutAPayDate() {
    var dated = dividend(DAY_2);
    var undated = dividend(null);

    assertThat(StockDividendHtmxController.inPayDateRange(List.of(dated, undated), DAY_1, DAY_3))
        .as("a row with no pay date is outside every period the backend answers")
        .containsExactly(dated);
  }

  @Test
  void keepsEveryRowWhenNoPeriodIsGiven() {
    var rows = List.of(dividend(DAY_1), dividend(null), dividend(DAY_3));

    assertThat(StockDividendHtmxController.inPayDateRange(rows, null, null))
        .as("the all-period view asks for no dates, so nothing may be dropped")
        .isEqualTo(rows);
  }

  @Test
  void handlesAnOpenEndedPeriod() {
    var first = dividend(DAY_1);
    var third = dividend(DAY_3);

    assertThat(StockDividendHtmxController.inPayDateRange(List.of(first, third), DAY_2, null))
        .containsExactly(third);
    assertThat(StockDividendHtmxController.inPayDateRange(List.of(first, third), null, DAY_2))
        .containsExactly(first);
  }

  @Test
  void keepsTheOrderItWasGiven() {
    var newest = dividend(DAY_3);
    var oldest = dividend(DAY_1);

    assertThat(StockDividendHtmxController.inPayDateRange(List.of(newest, oldest), DAY_1, null))
        .as("the backend orders by payDate DESC, so the filtered list must stay in that order")
        .containsExactly(newest, oldest);
  }
}
