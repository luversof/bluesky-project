package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.view.DividendCalendarView;

/**
 * 실수령 배당의 '달력 보기' &mdash; 고른 기간이 <b>달력 한 달</b>이면 그 달에 실제로 받은 배당을 받은 날에 놓는다.
 *
 * <p>2026-09-17 까지 배당 화면에는 '배당 캘린더' 탭이 따로 있었다. 지난 달을 넘겨 보면 그 탭은 원장의 실지급을 그렸는데, 같은 금액은 실수령 배당 탭을 그
 * 달로 좁혀도 거의 다 있었다 &mdash; 탭만의 것은 날짜 격자뿐이었다. 사용자 결정(2026-09-17): 캘린더를 실수령 배당에 통합하고, 기간이 달력 한 달일 때
 * 자동으로 달력을 보인다(계좌 · 종목 · 태그 필터를 그대로 따른다). 앞으로 받을 예정은 화면 위 '다가올 배당' 이 따로 말한다.
 *
 * <p>달 이동은 기간 선택기가 한다 &mdash; '이번달' 에서 ‹ 이전 을 누르면 직전 달 1 일 ~ 말일이 되고, 이번 달에 닿으면 다시 '이번달' 이 된다.
 */
public final class DividendMonthCalendarUtil {

  private DividendMonthCalendarUtil() {}

  /**
   * 기간이 달력 한 달인가. 그렇다면 그 달, 아니면 {@code null}.
   *
   * <p>한 달은 1 일 0 시부터 다음 달 1 일 0 시(배타)까지다. 이번 달은 '이번달' 프리셋처럼 오늘까지(내일 0 시 배타)여도 그 달로 본다 &mdash; 아직
   * 오지 않은 날은 받은 게 없으니 격자의 뜻이 같다.
   *
   * @param endExclusive 기간의 끝(배타)
   * @param zone 날짜를 가를 존(화면의 다른 날짜와 같은 요청 존)
   */
  public static YearMonth calendarMonth(
      Instant start, Instant endExclusive, ZoneId zone, LocalDate today) {
    if (start == null || endExclusive == null || zone == null) {
      return null;
    }
    ZonedDateTime from = start.atZone(zone);
    ZonedDateTime to = endExclusive.atZone(zone);
    if (!from.toLocalTime().equals(LocalTime.MIDNIGHT)
        || !to.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      return null;
    }
    LocalDate first = from.toLocalDate();
    if (first.getDayOfMonth() != 1) {
      return null;
    }
    YearMonth month = YearMonth.from(first);
    LocalDate endDate = to.toLocalDate();
    if (endDate.equals(first.plusMonths(1))) {
      return month;
    }
    if (today != null && month.equals(YearMonth.from(today)) && endDate.equals(today.plusDays(1))) {
      return month;
    }
    return null;
  }

  /**
   * 그 달의 (화면 필터를 거친) 배당을 받은 날에 놓는다.
   *
   * <p>한 종목이 한 달에 여러 번 받았으면 <b>받은 날마다</b> 놓는다 &mdash; 옛 캘린더 탭은 종목마다 한 칸(가장 늦은 날)에 합쳤지만, 여기서는 목록의 행을
   * 그대로 쓰므로 날짜를 뭉개지 않는다. 금액은 목록과 같은 값이다(세전 · 실수령 · 과세표준, 과세이연 계좌는 세금 0).
   *
   * <p>지나간 달에 받은 게 없으면 "그 달엔 받은 배당이 없다" 고 말하는 빈 달력을, 이번 달에 아직 받은 게 없으면 {@code null}(그리지 않음)을 돌려준다
   * &mdash; 빈 격자가 "고장" 으로 읽히지 않게, 그리고 이번 달 예정은 위 '다가올 배당' 이 말한다.
   */
  public static DividendCalendarView build(
      List<DividendView> dividends, YearMonth month, LocalDate today, ZoneId zone) {
    if (month == null || zone == null) {
      return null;
    }
    Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
    Map<String, String> names = new LinkedHashMap<>();
    Map<String, Integer> days = new LinkedHashMap<>();
    if (dividends != null) {
      for (DividendView dividend : dividends) {
        if (dividend == null || dividend.payDate() == null) {
          continue;
        }
        LocalDate payDate = dividend.payDate().atZone(zone).toLocalDate();
        if (!YearMonth.from(payDate).equals(month)) {
          continue;
        }
        String item =
            dividend.stockItemId() != null
                ? dividend.stockItemId().toString()
                : String.valueOf(dividend.stockItemName());
        String key = item + "|" + payDate.getDayOfMonth();
        BigDecimal[] sum =
            sums.computeIfAbsent(
                key, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        sum[0] = sum[0].add(nz(dividend.grossAmount()));
        sum[1] = sum[1].add(nz(dividend.netAmount()));
        sum[2] = sum[2].add(nz(dividend.taxableAmount()));
        names.putIfAbsent(key, dividend.stockItemName());
        days.putIfAbsent(key, payDate.getDayOfMonth());
      }
    }

    boolean past = today != null && month.isBefore(YearMonth.from(today));
    if (sums.isEmpty() && !past) {
      return null;
    }

    List<DividendCalendarView.Entry> entries = new ArrayList<>();
    sums.forEach(
        (key, sum) -> {
          int day = days.get(key);
          entries.add(
              new DividendCalendarView.Entry(
                  null,
                  names.get(key),
                  sum[1],
                  sum[0],
                  sum[2],
                  null,
                  day,
                  day,
                  day,
                  0,
                  true,
                  true));
        });
    return new DividendCalendarView(
        month,
        DividendCalendarGridUtil.build(
            month, today, DividendCalendarGridUtil.groupByDay(entries, month)),
        List.of(),
        List.of(),
        past && entries.isEmpty());
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }
}
