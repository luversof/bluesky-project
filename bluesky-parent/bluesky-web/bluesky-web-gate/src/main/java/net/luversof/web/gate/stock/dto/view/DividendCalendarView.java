package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 배당 캘린더 한 달치.
 *
 * <p>2026-09-14 까지 이 화면은 이름만 캘린더였다 &mdash; 날짜가 하나도 없고 월중/월말 두 덩어리로만 나뉘었다. 그 덩어리 정보는 월배당 시뮬레이터의 요약
 * 카드에도 이미 있어(총 최근 월 배당금 옆의 월중/월말 분해), 이 화면이 홀로 주는 것이 사실상 "어느 종목이 어느 덩어리인가" 하나뿐이었다.
 *
 * <p>그래서 실제 달력으로 만든다 &mdash; 지급이력의 <b>종목별 최빈 지급일</b>에 종목을 놓는다({@link
 * net.luversof.web.gate.stock.util.MonthlyDividendPayDayUtil}). 시뮬레이터는 "어느 종목이 유리한가" 를, 이 화면은 "언제
 * 얼마가 들어오나" 를 답한다.
 */
public record DividendCalendarView(YearMonth month, List<Week> weeks, List<Entry> undated) {

  /**
   * 달력 한 칸에 놓이는 종목 하나.
   *
   * @param taxable 예상 과세표준액(평균 기준). 배당과 달리 '최근' 기준은 스냅샷에 없다
   * @param historyTaxableRatioPct 지급이력이 말하는 과세표준 비중(최근 12건, 백분율). 저장값이 0 일 때 화면이 이걸 근거로 밝힌다
   * @param payDay 그 달에 놓인 일자
   * @param earliestDay 지급이력에서 관측된 가장 이른 일자
   * @param latestDay 관측된 가장 늦은 일자
   * @param sampleCount 근거가 된 지급 건수
   */
  public record Entry(
      String symbol,
      String name,
      BigDecimal average,
      BigDecimal latest,
      BigDecimal taxable,
      BigDecimal historyTaxableRatioPct,
      int payDay,
      int earliestDay,
      int latestDay,
      int sampleCount) {

    /**
     * 저장된 과세표준이 0 인데 지급이력은 과세가 있었다고 말한다.
     *
     * <p>그 0 은 "세금이 없다" 가 아니라 "기준 데이터에 안 적혀 있다" 다 &mdash; 실측 2026-09-14: 8 종목 중 2 종목이 저장값 0 인데 지급이력
     * 기준으로는 31.3% · 25.1% 가 과세였다. 0 만 적으면 비과세로 읽히므로 화면이 이 사실을 함께 적는다.
     */
    public boolean taxableLooksUnregistered() {
      return net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(taxable) == 0
          && historyTaxableRatioPct != null
          && historyTaxableRatioPct.signum() > 0;
    }

    /** 실제 지급일이 한 날에 모여 있지 않다 &mdash; 화면은 이때 폭을 함께 적는다. */
    public boolean spread() {
      return latestDay > earliestDay;
    }
  }

  /** 달력 한 칸. 앞뒤 달의 날짜도 자리를 채우려고 들어온다({@code inMonth = false}). */
  public record Day(LocalDate date, boolean inMonth, boolean today, List<Entry> entries) {

    public boolean hasEntries() {
      return entries != null && !entries.isEmpty();
    }

    public BigDecimal averageTotal() {
      return sum(true);
    }

    public BigDecimal latestTotal() {
      return sum(false);
    }

    /** 이 칸의 예상 과세표준 합. 보이는 값을 더한다(위 합계들과 같은 규칙). */
    public BigDecimal taxableTotal() {
      if (entries == null) {
        return BigDecimal.ZERO;
      }
      BigDecimal total = BigDecimal.ZERO;
      for (Entry entry : entries) {
        total =
            total.add(
                BigDecimal.valueOf(
                    net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(entry.taxable())));
      }
      return total;
    }

    /**
     * <b>화면에 보이는 값</b>을 더한다.
     *
     * <p>원 단위로 반올림한 뒤 더해야 칸의 합계가 그 칸 숫자를 손으로 더한 값과 같다. 원값을 더한 뒤 한 번 반올림하면 종목 수만큼 어긋난다 &mdash; 같은
     * 이유로 월/연 합계도 이미 그렇게 낸다(실측 2026-08-23: 8 종목에서 2 원 차이).
     */
    private BigDecimal sum(boolean average) {
      if (entries == null) {
        return BigDecimal.ZERO;
      }
      BigDecimal total = BigDecimal.ZERO;
      for (Entry entry : entries) {
        BigDecimal value = average ? entry.average() : entry.latest();
        total =
            total.add(
                BigDecimal.valueOf(
                    net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(value)));
      }
      return total;
    }
  }

  /** 달력 한 줄(일요일 시작). */
  public record Week(List<Day> days) {}

  /** 지급일을 추정할 이력이 없는 종목. 달력에 놓으면 없는 일정을 지어내는 셈이라 따로 적는다. */
  public boolean hasUndated() {
    return undated != null && !undated.isEmpty();
  }
}
