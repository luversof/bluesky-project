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
public record DividendCalendarView(
    YearMonth month,
    List<Week> weeks,
    List<Entry> undated,
    List<Missing> missing,
    boolean settledEmpty) {

  /** 달력이 그리지 못한 그 달의 실제 배당. 없는 것이 기본이다(앞으로 올 달엔 원장이 아직 비어 있다). */
  public DividendCalendarView(YearMonth month, List<Week> weeks, List<Entry> undated) {
    this(month, weeks, undated, List.of(), false);
  }

  public DividendCalendarView(
      YearMonth month, List<Week> weeks, List<Entry> undated, List<Missing> missing) {
    this(month, weeks, undated, missing, false);
  }

  /**
   * 그 달 원장에는 있는데 달력에는 놓지 못한 종목.
   *
   * <p>달력은 <b>월배당 기준 데이터가 있는 종목</b>만 그린다. 지나간 달을 보는 사람에게는 그 달 배당의 일부가 통째로 안 보이는 셈이다 &mdash; 실측
   * 2026-09-15: 2026-08 은 분기배당인 삼성전자 1,886,082 원이 빠져 그 달 배당(세전 4,982,406 원)의 <b>37.9%</b> 가 달력 밖에
   * 있었다. 금액을 적어 합이 안 맞는 까닭을 밝힌다.
   *
   * @param amount 세전 금액. 달력의 큰 숫자와 같은 축이라야 더해서 말이 된다
   */
  public record Missing(String name, BigDecimal amount) {}

  /**
   * 달력 한 칸에 놓이는 종목 하나.
   *
   * @param taxable 예상 과세표준액(평균 기준). 배당과 달리 '최근' 기준은 스냅샷에 없다
   * @param historyTaxableRatioPct 지급이력이 말하는 과세표준 비중(최근 12건, 백분율). 저장값이 0 일 때 화면이 이걸 근거로 밝힌다
   * @param payDay 그 달에 놓인 일자
   * @param earliestDay 지급이력에서 관측된 가장 이른 일자
   * @param latestDay 관측된 가장 늦은 일자
   * @param sampleCount 근거가 된 지급 건수
   * @param actualPayDate 그 달의 실제 지급일에 놓였다(추정이 아니다). 지나간 달은 이력이 답을 갖고 있다
   * @param actualAmount 금액도 추정이 아니다 &mdash; 그 달 원장에 적힌 실제 수령액이다
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
      int sampleCount,
      boolean actualPayDate,
      boolean actualAmount) {

    /** 금액은 추정이 기본이다 &mdash; 그게 이 화면이 오래도록 해 온 일이고, 앞으로 올 달엔 달리 방법이 없다. */
    public Entry(
        String symbol,
        String name,
        BigDecimal average,
        BigDecimal latest,
        BigDecimal taxable,
        BigDecimal historyTaxableRatioPct,
        int payDay,
        int earliestDay,
        int latestDay,
        int sampleCount,
        boolean actualPayDate) {
      this(
          symbol,
          name,
          average,
          latest,
          taxable,
          historyTaxableRatioPct,
          payDay,
          earliestDay,
          latestDay,
          sampleCount,
          actualPayDate,
          false);
    }

    /**
     * 저장된 과세표준이 0 인데 지급이력은 과세가 있었다고 말한다.
     *
     * <p>그 0 은 "세금이 없다" 가 아니라 "기준 데이터에 안 적혀 있다" 다 &mdash; 실측 2026-09-14: 8 종목 중 2 종목이 저장값 0 인데 지급이력
     * 기준으로는 31.3% · 25.1% 가 과세였다. 0 만 적으면 비과세로 읽히므로 화면이 이 사실을 함께 적는다.
     */
    public boolean taxableLooksUnregistered() {
      // 금액이 원장에서 온 달에는 0 이 곧 사실이다 - 그 달 과세표준이 정말 0 이었다는 뜻이라
      // '기준 데이터에 없다' 고 물음표를 달면 확인된 값을 의심하게 만든다(실측 2026-09-15:
      // 2026-08 에 네 종목이 실제 과세표준 0 인데 물음표가 붙었다).
      return !actualAmount
          && net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(taxable) == 0
          && historyTaxableRatioPct != null
          && historyTaxableRatioPct.signum() > 0;
    }

    /**
     * 실제 지급일이 한 날에 모여 있지 않다 &mdash; 화면은 이때 폭을 함께 적는다.
     *
     * <p>날짜가 <b>확정</b>이면(그 달 이력이 있다) 폭은 말할 필요가 없다. 추정일 때만 쓰는 말이다.
     */
    public boolean spread() {
      return !actualPayDate && latestDay > earliestDay;
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

  /**
   * 달력에 놓인 것 중 <b>추정</b>으로 놓인 것이 하나라도 있나.
   *
   * <p>지나간 달은 그 달 지급이력이 있어 전부 확정이다. 그때까지 "날짜는 추정" 이라고 적으면 화면이 거짓말을 한다.
   */
  public boolean hasEstimatedDate() {
    if (weeks == null) {
      return false;
    }
    for (Week week : weeks) {
      for (Day day : week.days()) {
        if (!day.hasEntries()) {
          continue;
        }
        for (Entry entry : day.entries()) {
          if (!entry.actualPayDate()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** 지급일을 추정할 이력이 없는 종목. 달력에 놓으면 없는 일정을 지어내는 셈이라 따로 적는다. */
  public boolean hasUndated() {
    return undated != null && !undated.isEmpty();
  }

  /** 금액이 <b>실제 수령액</b>으로 적힌 칸이 하나라도 있나. */
  public boolean hasActualAmount() {
    return anyEntry(true);
  }

  /**
   * 배당이 있는 날만, 달력 순서대로.
   *
   * <p>달력 아래 상세가 이걸 순회한다. 42 칸 격자에서 실제로 차는 칸은 <b>평균 2 개</b>다 &mdash; 실측 2026-09-15(최근 24 개월): 지급일이
   * 1~3 일뿐이고 늘 월초(2~6 일) · 월중(17~20 일) 두 덩어리라 격자의 95% 는 구조적으로 빈다. 그 빈 면적 때문에 정작 금액이 11px 로 눌려 있었다
   * &mdash; 격자는 "언제" 만 말하고 금액은 이 목록이 제 크기로 말한다.
   */
  public List<Day> payDays() {
    List<Day> result = new java.util.ArrayList<>();
    if (weeks == null) {
      return result;
    }
    for (Week week : weeks) {
      for (Day day : week.days()) {
        if (day.inMonth() && day.hasEntries()) {
          result.add(day);
        }
      }
    }
    return result;
  }

  /** 달력 칸에 놓인 종목이 하나라도 있나. 하나도 없는데 달력 밖에만 있는 달이 있다. */
  public boolean hasPlacedEntries() {
    return anyEntry(true) || anyEntry(false);
  }

  /** 금액이 아직 <b>예상치</b>인 칸이 하나라도 있나. 하나라도 있으면 화면은 예상치라고 말해야 한다. */
  public boolean hasEstimatedAmount() {
    return anyEntry(false);
  }

  private boolean anyEntry(boolean actual) {
    if (weeks == null) {
      return false;
    }
    for (Week week : weeks) {
      for (Day day : week.days()) {
        if (!day.hasEntries()) {
          continue;
        }
        for (Entry entry : day.entries()) {
          if (entry.actualAmount() == actual) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * 지나간 달인데 그 달 원장이 <b>비어 있다</b> &mdash; 받은 배당이 없었다는 뜻이다.
   *
   * <p>격자가 빈 것만으로는 "안 받았다" 와 "그릴 종목이 없다" 를 가를 수 없어 컨트롤러가 알려 준다. 빈 격자에 아무 말도 없으면 화면이 고장 난 것처럼 보인다.
   */
  public boolean isSettledEmpty() {
    return settledEmpty;
  }

  public boolean hasMissing() {
    return missing != null && !missing.isEmpty();
  }

  /** 달력 밖에 있는 그 달 배당의 합(세전). */
  public BigDecimal missingTotal() {
    BigDecimal total = BigDecimal.ZERO;
    if (missing == null) {
      return total;
    }
    for (Missing item : missing) {
      total =
          total.add(
              BigDecimal.valueOf(
                  net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(item.amount())));
    }
    return total;
  }

  /** 달력에 놓인 것 중 <b>실제 수령액</b>으로 적힌 것의 합(세전). 추정으로 적힌 칸은 빼고 센다. */
  public BigDecimal placedActualTotal() {
    BigDecimal total = BigDecimal.ZERO;
    if (weeks == null) {
      return total;
    }
    for (Week week : weeks) {
      for (Day day : week.days()) {
        if (!day.hasEntries()) {
          continue;
        }
        for (Entry entry : day.entries()) {
          if (entry.actualAmount()) {
            total =
                total.add(
                    BigDecimal.valueOf(
                        net.luversof.web.gate.stock.util.StockFormatUtil.displayWon(
                            entry.latest())));
          }
        }
      }
    }
    return total;
  }

  /** 그 달에 실제로 받은 배당 전부(세전) &mdash; 달력에 놓인 것 + 놓지 못한 것. */
  public BigDecimal actualMonthTotal() {
    return placedActualTotal().add(missingTotal());
  }
}
