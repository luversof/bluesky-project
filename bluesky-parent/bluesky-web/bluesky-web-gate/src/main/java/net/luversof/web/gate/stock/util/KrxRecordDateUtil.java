package net.luversof.web.gate.stock.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import net.luversof.web.gate.stock.dto.view.RecordDateBuyByView;
import net.luversof.web.gate.util.KoreanHolidays;

/**
 * 월배당 ETF 의 분배금 지급 기준일과 그 분배금을 받으려면 언제까지 사야 하는지(2026-10-20 자율 개선).
 *
 * <p>적립식으로 매달 사는데, 기준일 하루 전(배당락일)에 사면 그 달 분배금을 못 받는다 &mdash; 국내 주식 · ETF 는 체결 다음 다음 거래일(T+2)에 결제돼
 * 기준일에 주주로 올라 있으려면 <b>기준일의 2 거래일 전</b>까지 사야 한다.
 *
 * <p>기준일 규칙(실측 2026-10-20, 지급 이력 2025-09 ~ 2026-10 의 월중 9 종목 · 월말 12 종목이 모두 따른다):
 *
 * <ul>
 *   <li>월중 = 그 달 15 일, 거래일이 아니면 그 앞 거래일(2026-08-15 토 &rarr; 08-14, 2026-02-15 일 &rarr; 02-13).
 *   <li>월말 = 그 달 마지막 거래일(2026-05-31 일 &rarr; 05-29, 2025-12-31 휴장 &rarr; 12-30).
 * </ul>
 *
 * <p>거래일 = 주말 · 공휴일({@link KoreanHolidays}) · 거래소 휴장일(근로자의 날 5 월 1 일 · 연말 휴장 = 12 월 마지막 평일)이 아닌 날.
 * 공휴일 표가 없는 해는 주말 · 휴장일만 빼므로 {@link RecordDateBuyByView#holidaysCovered()} 로 밝힌다.
 */
public final class KrxRecordDateUtil {

  /** 결제일(T+2) - 기준일에 주주로 오르려면 그만큼 앞서 사야 한다. */
  public static final int SETTLEMENT_TRADING_DAYS = 2;

  private static final int MID_MONTH_DAY = 15;

  private KrxRecordDateUtil() {}

  /** 거래소가 여는 날인가. */
  public static boolean isTradingDay(LocalDate date) {
    if (date == null) {
      return false;
    }
    DayOfWeek day = date.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
      return false;
    }
    if ((date.getMonthValue() == 5 && date.getDayOfMonth() == 1)
        || date.equals(yearEndClosure(date.getYear()))) {
      return false;
    }
    return KoreanHolidays.holidayName(date, Locale.KOREAN) == null;
  }

  /**
   * 연말 휴장일 = 12 월의 마지막 평일(2026-10-20 검토 반영). 12 월 31 일이 주말이면 그 앞 금요일에 쉰다 - 시세 행으로 확인: 2022-12-30(31
   * 일 토) · 2023-12-29 · 2017-12-29(31 일 일) 에 거래가 없다. 31 일로 박아 두면 2028 · 2033 · 2034 년 월말 기준일과 마감이
   * 하루씩 늦게 나온다.
   */
  static LocalDate yearEndClosure(int year) {
    LocalDate day = LocalDate.of(year, 12, 31);
    while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
      day = day.minusDays(1);
    }
    return day;
  }

  /** 그날이 거래일이면 그날, 아니면 그 앞 거래일. */
  public static LocalDate previousOrSameTradingDay(LocalDate date) {
    LocalDate day = date;
    while (!isTradingDay(day)) {
      day = day.minusDays(1);
    }
    return day;
  }

  /** {@code count} 거래일 앞(그날은 세지 않는다). */
  public static LocalDate minusTradingDays(LocalDate date, int count) {
    LocalDate day = date;
    int left = count;
    while (left > 0) {
      day = day.minusDays(1);
      if (isTradingDay(day)) {
        left--;
      }
    }
    return day;
  }

  /** {@code count} 거래일 뒤(그날은 세지 않는다). */
  public static LocalDate plusTradingDays(LocalDate date, int count) {
    LocalDate day = date;
    int left = count;
    while (left > 0) {
      day = day.plusDays(1);
      if (isTradingDay(day)) {
        left--;
      }
    }
    return day;
  }

  /** {@code from} 다음 날부터 {@code to} 까지 거래일 수(to 가 앞이면 0). 기준일 &rarr; 지급일 지연을 잰다. */
  public static int tradingDaysBetween(LocalDate from, LocalDate to) {
    int count = 0;
    for (LocalDate day = from.plusDays(1); !day.isAfter(to); day = day.plusDays(1)) {
      if (isTradingDay(day)) {
        count++;
      }
    }
    return count;
  }

  /** 오늘까지 지난(오늘 포함) 가장 최근 기준일. 이번 달 기준일이 아직이면 앞 달 것. 규칙 없는 시기는 null. */
  public static LocalDate latestRecordDateOnOrBefore(LocalDate today, String payoutWindow) {
    if (today == null) {
      return null;
    }
    LocalDate record = recordDate(YearMonth.from(today), payoutWindow);
    if (record != null && record.isAfter(today)) {
      record = recordDate(YearMonth.from(today).minusMonths(1), payoutWindow);
    }
    return record;
  }

  /** 지급 시기({@code MID_MONTH} · {@code MONTH_END})의 그 달 기준일. 다른 시기는 규칙이 없어 null. */
  public static LocalDate recordDate(YearMonth month, String payoutWindow) {
    if (month == null) {
      return null;
    }
    if ("MID_MONTH".equals(payoutWindow)) {
      return previousOrSameTradingDay(month.atDay(MID_MONTH_DAY));
    }
    if ("MONTH_END".equals(payoutWindow)) {
      return previousOrSameTradingDay(month.atEndOfMonth());
    }
    return null;
  }

  /** 월중 · 월말 두 지급 창의 다음 매수 마감(정할 수 없는 창은 뺀다). 시뮬레이터 · 월배당 ETF · 대시보드 카드가 같은 목록을 쓴다. */
  public static java.util.List<RecordDateBuyByView> nextBuyByAllWindows(LocalDate today) {
    return java.util.stream.Stream.of("MID_MONTH", "MONTH_END")
        .map(window -> nextBuyBy(today, window))
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  /**
   * 오늘 사서 받을 수 있는 가장 가까운 기준일과 그 매수 마감일. 이번 달 마감이 지났으면 다음 달 것이다.
   *
   * @return 지급 시기에 규칙이 없으면 null
   */
  public static RecordDateBuyByView nextBuyBy(LocalDate today, String payoutWindow) {
    if (today == null || recordDate(YearMonth.from(today), payoutWindow) == null) {
      return null;
    }
    YearMonth month = YearMonth.from(today);
    for (int i = 0; i < 3; i++, month = month.plusMonths(1)) {
      LocalDate record = recordDate(month, payoutWindow);
      LocalDate buyBy = minusTradingDays(record, SETTLEMENT_TRADING_DAYS);
      if (!buyBy.isBefore(today)) {
        boolean covered =
            KoreanHolidays.covered(record.getYear()) && KoreanHolidays.covered(buyBy.getYear());
        return new RecordDateBuyByView(
            payoutWindow, record, buyBy, ChronoUnit.DAYS.between(today, buyBy), covered);
      }
    }
    return null;
  }
}
