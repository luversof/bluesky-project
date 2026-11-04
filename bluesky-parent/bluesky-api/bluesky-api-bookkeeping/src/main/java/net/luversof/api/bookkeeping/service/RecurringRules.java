package net.luversof.api.bookkeeping.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.RecurringCycle;
import net.luversof.api.bookkeeping.domain.BookRecurring;

/** 반복 주기 검증 · 회차 계산(순수 함수). */
final class RecurringRules {

  /** 한 번에 계산하는 기간 상한(화면은 한 달씩 묻는다). */
  static final int MAX_RANGE_DAYS = 400;

  private RecurringRules() {}

  /** cycle 에 맞는 칸만 남긴 [dayOfMonth, dayOfWeek, monthOfYear]. 맞지 않으면 INVALID_RECURRING_CYCLE. */
  static Integer[] cycleColumns(
      RecurringCycle cycle, Integer dayOfMonth, Integer dayOfWeek, Integer monthOfYear) {
    if (cycle == null) {
      throw BookkeepingErrorCode.INVALID_RECURRING_CYCLE.exception();
    }
    return switch (cycle) {
      case MONTHLY -> new Integer[] {between(dayOfMonth, 1, 31), null, null};
      case WEEKLY -> new Integer[] {null, between(dayOfWeek, 1, 7), null};
      case YEARLY -> new Integer[] {between(dayOfMonth, 1, 31), null, between(monthOfYear, 1, 12)};
    };
  }

  /** from ~ to(양 끝 포함) 안의 회차, 날짜순. 시작일 전 · 종료일 뒤는 없다. */
  static List<LocalDate> occurrences(BookRecurring r, LocalDate from, LocalDate to) {
    var start = r.startDate().isAfter(from) ? r.startDate() : from;
    var end = r.endDate() != null && r.endDate().isBefore(to) ? r.endDate() : to;
    var dates = new ArrayList<LocalDate>();
    if (start.isAfter(end)) {
      return dates;
    }
    switch (r.cycle()) {
      case WEEKLY -> {
        var d = start.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(r.dayOfWeek())));
        for (; !d.isAfter(end); d = d.plusWeeks(1)) {
          dates.add(d);
        }
      }
      case MONTHLY -> {
        for (var m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
          addIfIn(dates, day(m, r.dayOfMonth()), start, end);
        }
      }
      case YEARLY -> {
        for (int y = start.getYear(); y <= end.getYear(); y++) {
          addIfIn(dates, day(YearMonth.of(y, r.monthOfYear()), r.dayOfMonth()), start, end);
        }
      }
    }
    return dates;
  }

  static boolean isOccurrence(BookRecurring r, LocalDate date) {
    return occurrences(r, date, date).contains(date);
  }

  /** 그 달에 없는 날(31 일 · 2 월 29 일)은 말일. */
  private static LocalDate day(YearMonth month, int dayOfMonth) {
    return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
  }

  private static void addIfIn(List<LocalDate> dates, LocalDate d, LocalDate start, LocalDate end) {
    if (!d.isBefore(start) && !d.isAfter(end)) {
      dates.add(d);
    }
  }

  private static Integer between(Integer value, int min, int max) {
    if (value == null || value < min || value > max) {
      throw BookkeepingErrorCode.INVALID_RECURRING_CYCLE.exception();
    }
    return value;
  }
}
