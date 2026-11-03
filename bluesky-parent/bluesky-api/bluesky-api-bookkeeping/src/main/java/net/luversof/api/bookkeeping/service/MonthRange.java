package net.luversof.api.bookkeeping.service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 가계부 한 달의 기간(양 끝 포함).
 *
 * <p>월 시작일이 25 면 "2026-11" = 2026-10-25 ~ 2026-11-24. 월 이름은 기간이 끝나는 달을 따른다. 시작일 1 은 달력 월과 같다.
 */
public record MonthRange(YearMonth month, LocalDate from, LocalDate to) {

  /** startDay 는 1~28(BookService 가 검증). */
  public static MonthRange of(YearMonth month, int startDay) {
    if (startDay == 1) {
      return new MonthRange(month, month.atDay(1), month.atEndOfMonth());
    }
    return new MonthRange(month, month.minusMonths(1).atDay(startDay), month.atDay(startDay - 1));
  }

  /** 날짜가 속한 가계부 월. */
  public static YearMonth monthOf(LocalDate date, int startDay) {
    var month = YearMonth.from(date);
    return startDay > 1 && date.getDayOfMonth() >= startDay ? month.plusMonths(1) : month;
  }
}
