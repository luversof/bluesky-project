package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 배당 내역 위의 '다가올 배당' &mdash; 이번 달 남은 날과 다음 달의 예정 지급.
 *
 * <p>2026-09-17 까지 이 일은 '배당 캘린더' 탭의 이번 달 · 다음 달 달력이 했다. 지난 달의 실지급은 실수령 배당과 겹쳐 캘린더 탭을 실수령 배당에 통합하면서
 * (사용자 결정), 캘린더에만 있던 '앞으로' 를 이 구역으로 옮겼다. 범위도 사용자가 골랐다: 이번 달 남은 날 + 다음 달.
 *
 * <p>날짜는 지급 이력(그 달 발표일이 있으면 그 날, 없으면 최빈일), 금액은 월배당 기준 데이터(지금 보유 수량 기준 예상)다.
 *
 * @param today 기준일(서버 존). 이번 달의 '남은 날' 을 가른다
 * @param days 예정 지급일(날짜순, 종목이 있는 날만)
 * @param undated 지급 이력이 없어 날짜를 지어낼 수 없는 종목
 * @param overdue 이번 달 예정일이 지났는데 원장에 받은 기록이 아직 없는 종목(늦는 지급 · 원장 미반영). 안 적으면 조용히 사라진다
 */
public record UpcomingDividendScheduleView(
    LocalDate today,
    List<DividendCalendarView.Day> days,
    List<DividendCalendarView.Entry> undated,
    List<DividendCalendarView.Entry> overdue) {

  public boolean hasDays() {
    return days != null && !days.isEmpty();
  }

  public boolean hasUndated() {
    return undated != null && !undated.isEmpty();
  }

  public boolean hasOverdue() {
    return overdue != null && !overdue.isEmpty();
  }

  /** 이번 달 남은 날의 예정 합(최근 기준, 칸마다 보이는 값을 더한다). */
  public BigDecimal thisMonthTotal() {
    return monthTotal(today != null ? YearMonth.from(today) : null);
  }

  /** 다음 달 예정 합(최근 기준). */
  public BigDecimal nextMonthTotal() {
    return monthTotal(today != null ? YearMonth.from(today).plusMonths(1) : null);
  }

  private BigDecimal monthTotal(YearMonth month) {
    BigDecimal total = BigDecimal.ZERO;
    if (month == null || days == null) {
      return total;
    }
    for (DividendCalendarView.Day day : days) {
      if (day != null && day.date() != null && YearMonth.from(day.date()).equals(month)) {
        total = total.add(day.latestTotal());
      }
    }
    return total;
  }
}
