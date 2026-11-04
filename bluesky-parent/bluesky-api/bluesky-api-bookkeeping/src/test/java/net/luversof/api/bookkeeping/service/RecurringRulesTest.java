package net.luversof.api.bookkeeping.service;

import static net.luversof.api.bookkeeping.service.RulesTest.assertError;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.constant.RecurringCycle;
import net.luversof.api.bookkeeping.domain.BookRecurring;

/** 반복 회차 계산: 말일 처리 · 주 · 해 · 시작/종료 경계. */
class RecurringRulesTest {

  static BookRecurring r(
      RecurringCycle cycle, Integer dom, Integer dow, Integer moy, String start, String end) {
    return new BookRecurring(
        UUID.randomUUID(),
        UUID.randomUUID(),
        EntryType.EXPENSE,
        BigDecimal.ONE,
        null,
        UUID.randomUUID(),
        null,
        UUID.randomUUID(),
        null,
        null,
        cycle,
        dom,
        dow,
        moy,
        LocalDate.parse(start),
        end == null ? null : LocalDate.parse(end),
        UUID.randomUUID(),
        Instant.now());
  }

  static String dates(BookRecurring r, String from, String to) {
    return RecurringRules.occurrences(r, LocalDate.parse(from), LocalDate.parse(to)).toString();
  }

  @Test
  void monthlyClampsToLastDayAndRespectsStartEnd() {
    var r = r(RecurringCycle.MONTHLY, 31, null, null, "2024-01-15", "2024-04-29");
    assertThat(dates(r, "2024-01-01", "2024-12-31"))
        .isEqualTo("[2024-01-31, 2024-02-29, 2024-03-31]"); // 4/30 은 종료일 뒤
    assertThat(dates(r, "2024-02-01", "2024-02-28")).isEqualTo("[]");
    assertThat(
            dates(
                r(RecurringCycle.MONTHLY, 15, null, null, "2026-01-20", null),
                "2026-01-01",
                "2026-02-28"))
        .isEqualTo("[2026-02-15]"); // 시작일 전 1/15 는 없다
  }

  @Test
  void weeklyAndYearly() {
    var mondays = r(RecurringCycle.WEEKLY, null, 1, null, "2026-11-01", null);
    assertThat(dates(mondays, "2026-11-01", "2026-11-17"))
        .isEqualTo("[2026-11-02, 2026-11-09, 2026-11-16]");
    var leap = r(RecurringCycle.YEARLY, 29, null, 2, "2024-01-01", null);
    assertThat(dates(leap, "2024-01-01", "2026-12-31"))
        .isEqualTo("[2024-02-29, 2025-02-28, 2026-02-28]");
    assertThat(RecurringRules.isOccurrence(leap, LocalDate.parse("2025-02-28"))).isTrue();
    assertThat(RecurringRules.isOccurrence(leap, LocalDate.parse("2025-02-27"))).isFalse();
  }

  @Test
  void cycleColumnsKeepOnlyWhatTheCycleUses() {
    assertThat(RecurringRules.cycleColumns(RecurringCycle.YEARLY, 5, 3, 12))
        .containsExactly(5, null, 12);
    assertThat(RecurringRules.cycleColumns(RecurringCycle.WEEKLY, 5, 7, 12))
        .containsExactly(null, 7, null);
    assertError(
        () -> RecurringRules.cycleColumns(RecurringCycle.MONTHLY, 0, null, null),
        BookkeepingErrorCode.INVALID_RECURRING_CYCLE);
    assertError(
        () -> RecurringRules.cycleColumns(RecurringCycle.YEARLY, 1, null, 13),
        BookkeepingErrorCode.INVALID_RECURRING_CYCLE);
    assertError(
        () -> RecurringRules.cycleColumns(null, 1, 1, 1),
        BookkeepingErrorCode.INVALID_RECURRING_CYCLE);
  }
}
