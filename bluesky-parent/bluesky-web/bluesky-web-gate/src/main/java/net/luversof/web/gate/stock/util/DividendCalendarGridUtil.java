package net.luversof.web.gate.stock.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.luversof.web.gate.stock.dto.view.DividendCalendarView;

/**
 * 배당 캘린더의 달력 격자를 만든다.
 *
 * <p>칸은 <b>일요일</b>에서 시작한다. 앞뒤 달의 날짜로 첫 줄과 마지막 줄을 채워 줄마다 일곱 칸을 맞춘다 &mdash; 빈 칸으로 두면 표가 들쭉날쭉해진다. 그
 * 칸들은 {@code inMonth = false} 라 화면이 흐리게 그린다.
 *
 * <p>줄 수는 그 달에 필요한 만큼만 만든다(4~6). 언제나 6 줄로 두면 마지막 줄이 통째로 다음 달인 달이 생겨 빈 줄 하나가 남는다.
 */
public final class DividendCalendarGridUtil {

  private DividendCalendarGridUtil() {}

  /**
   * @param month 그릴 달
   * @param today 오늘(요청 존 기준). 그 달에 속하면 그 칸을 오늘로 표시한다
   * @param entriesByDay 일자(1~31) -> 그 날 지급이 예상되는 종목들
   */
  public static List<DividendCalendarView.Week> build(
      YearMonth month,
      LocalDate today,
      Map<Integer, List<DividendCalendarView.Entry>> entriesByDay) {
    List<DividendCalendarView.Week> weeks = new ArrayList<>();
    if (month == null) {
      return weeks;
    }
    LocalDate first = month.atDay(1);
    // 일요일 시작 - DayOfWeek 는 월요일이 1 이라 일요일(7)을 0 으로 옮긴다.
    int lead = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
    LocalDate cursor = first.minusDays(lead);
    LocalDate lastDay = month.atEndOfMonth();

    while (!cursor.isAfter(lastDay) || cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
      List<DividendCalendarView.Day> days = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        boolean inMonth = YearMonth.from(cursor).equals(month);
        List<DividendCalendarView.Entry> entries =
            inMonth && entriesByDay != null
                ? entriesByDay.getOrDefault(cursor.getDayOfMonth(), List.of())
                : List.of();
        days.add(new DividendCalendarView.Day(cursor, inMonth, cursor.equals(today), entries));
        cursor = cursor.plusDays(1);
      }
      weeks.add(new DividendCalendarView.Week(days));
      if (cursor.isAfter(lastDay)) {
        break;
      }
    }
    return weeks;
  }

  /**
   * 종목들을 일자별로 모은다.
   *
   * <p>같은 날에 여럿이면 금액이 큰 종목부터 &mdash; 달력 칸은 좁아 위에서 몇 줄만 눈에 들어온다.
   *
   * <p>그 달에 없는 일자(31 일 지급 종목의 2 월)는 그 달 마지막 날로 당긴다. 안 그러면 그 종목이 달력에서 통째로 사라진다.
   */
  public static Map<Integer, List<DividendCalendarView.Entry>> groupByDay(
      List<DividendCalendarView.Entry> entries, YearMonth month) {
    Map<Integer, List<DividendCalendarView.Entry>> byDay = new LinkedHashMap<>();
    if (entries == null || month == null) {
      return byDay;
    }
    for (DividendCalendarView.Entry entry : entries) {
      int day = MonthlyDividendPayDayUtil.dayInMonth(entry.payDay(), month);
      if (day <= 0) {
        continue;
      }
      byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(entry);
    }
    Comparator<DividendCalendarView.Entry> byAmountDesc =
        Comparator.comparing(
                (DividendCalendarView.Entry entry) ->
                    entry.average() != null ? entry.average() : java.math.BigDecimal.ZERO)
            .reversed();
    for (List<DividendCalendarView.Entry> sameDay : byDay.values()) {
      sameDay.sort(byAmountDesc);
    }
    return byDay;
  }
}
