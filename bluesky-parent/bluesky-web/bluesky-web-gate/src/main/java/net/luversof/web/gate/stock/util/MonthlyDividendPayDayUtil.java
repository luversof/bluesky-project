package net.luversof.web.gate.stock.util;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지급이력에서 <b>종목별</b> 대표 지급일(day-of-month)을 낸다.
 *
 * <p>기존에 같은 계산이 있었지만 <b>지급 시기 묶음</b>(월중/월말) 단위였다({@code
 * StockSummaryHtmxController.representativePayDay}). 배당 캘린더를 달력으로 그리려면 한 달 안에서 종목이 각자 제 날짜에 놓여야 하므로
 * 종목 단위가 필요하다.
 *
 * <p>규칙은 묶음 단위에서 검증된 것을 그대로 쓴다 &mdash; <b>지급 행 단위 최빈일</b>, 동률이면 최근에 나온 쪽. 그 규칙이 "최신 1건" 방식보다 나은 것은
 * 지급이력으로 되짚어 확인돼 있다(중순 평균오차 1.45 &rarr; 0.86 일, 월말 1.57 &rarr; 1.33 일).
 *
 * <p>실제 지급일은 한 날에 고정돼 있지 않다 &mdash; 실측 2026-09-11: 월중은 17 일 x39 · 19 일 x20 · 18 일 x10 · 20 일 x9.
 * 그래서 관측된 <b>가장 이른 날과 늦은 날</b>을 함께 돌려준다. 달력은 최빈일에 놓되 그 폭을 밝혀야 한다 &mdash; 날짜 하나만 적으면 사용자가 그 날 들어오는
 * 것으로 읽는다.
 */
public final class MonthlyDividendPayDayUtil {

  private MonthlyDividendPayDayUtil() {}

  /**
   * 한 종목의 지급일 추정.
   *
   * @param day 최빈 일자(1~31)
   * @param earliest 관측된 가장 이른 일자
   * @param latest 관측된 가장 늦은 일자
   * @param sampleCount 근거가 된 지급 건수
   */
  public record PayDay(int day, int earliest, int latest, int sampleCount) {

    /** 관측이 한 날에 모여 있지 않다. 화면은 이때 폭을 함께 적는다. */
    public boolean spread() {
      return latest > earliest;
    }
  }

  /** 지급일이 있는 한 건. 호출부가 DTO 를 그대로 넘기지 않게 해 유닛 테스트가 가벼워진다. */
  public record Payout(String symbol, LocalDate payDate) {}

  /** 종목 코드 정규화 &mdash; 지급이력과 스냅샷의 표기가 다를 수 있다(대소문자·공백). */
  public static String normalizeSymbol(String symbol) {
    if (symbol == null) {
      return null;
    }
    String trimmed = symbol.trim();
    return trimmed.isEmpty() ? null : trimmed.toUpperCase();
  }

  /**
   * 종목별 대표 지급일.
   *
   * <p>지급일이 없는 행과 종목 코드가 빈 행은 세지 않는다. 이력이 아예 없는 종목은 결과에 담기지 않는다 &mdash; 화면이 "날짜를 모른다" 를 따로 다뤄야 하기
   * 때문이다(0 이나 1 일로 채워 넣으면 없는 일정을 지어내는 셈이다).
   */
  /**
   * 그 달에 <b>실제로</b> 지급된 날(종목코드 &rarr; 일자).
   *
   * <p>달력은 지급이력의 최빈일로 날짜를 <b>추정</b>하는데, 이미 지나간 달은 추정할 이유가 없다 &mdash; 그 달의 지급이력이 곧 답이다. 실측
   * 2026-09-15: 2026-08 은 8 종목 전부 최빈일(2 일·17 일)에 놓였는데 실제 지급은 4 일·19 일이었다. 하필 2 일은 일요일, 17 일은
   * 대체공휴일(광복절)이라 <b>지급이 있을 수 없는 날</b>에 찍혀 있었다. 2026-07 도 4 종목이 17 일(실제 20 일)로 어긋났다.
   *
   * <p>한 달에 두 번 지급된 종목이 있으면 <b>가장 늦은 날</b>을 쓴다(달력 한 칸에 한 번만 놓는다).
   */
  public static Map<String, Integer> actualDayByStockItem(
      List<Payout> payouts, java.time.YearMonth month) {
    Map<String, Integer> bySymbol = new LinkedHashMap<>();
    if (payouts == null || month == null) {
      return bySymbol;
    }
    for (Payout payout : payouts) {
      if (payout == null || payout.payDate() == null) {
        continue;
      }
      if (!java.time.YearMonth.from(payout.payDate()).equals(month)) {
        continue;
      }
      String symbol = normalizeSymbol(payout.symbol());
      if (symbol == null) {
        continue;
      }
      bySymbol.merge(symbol, payout.payDate().getDayOfMonth(), Math::max);
    }
    return bySymbol;
  }

  public static Map<String, PayDay> byStockItem(List<Payout> payouts) {
    Map<String, Map<Integer, Integer>> countBySymbol = new LinkedHashMap<>();
    Map<String, Map<Integer, LocalDate>> latestDateBySymbol = new HashMap<>();
    Map<String, int[]> boundsBySymbol = new HashMap<>();
    Map<String, Integer> totalBySymbol = new HashMap<>();

    if (payouts != null) {
      for (Payout payout : payouts) {
        if (payout == null || payout.payDate() == null) {
          continue;
        }
        String symbol = normalizeSymbol(payout.symbol());
        if (symbol == null) {
          continue;
        }
        int day = payout.payDate().getDayOfMonth();
        countBySymbol.computeIfAbsent(symbol, key -> new HashMap<>()).merge(day, 1, Integer::sum);
        latestDateBySymbol
            .computeIfAbsent(symbol, key -> new HashMap<>())
            .merge(day, payout.payDate(), (left, right) -> left.isAfter(right) ? left : right);
        int[] bounds = boundsBySymbol.computeIfAbsent(symbol, key -> new int[] {day, day});
        bounds[0] = Math.min(bounds[0], day);
        bounds[1] = Math.max(bounds[1], day);
        totalBySymbol.merge(symbol, 1, Integer::sum);
      }
    }

    Map<String, PayDay> result = new LinkedHashMap<>();
    for (Map.Entry<String, Map<Integer, Integer>> entry : countBySymbol.entrySet()) {
      String symbol = entry.getKey();
      Map<Integer, LocalDate> latestByDay = latestDateBySymbol.get(symbol);
      // 같은 횟수인 날이 여럿이면 최근에 나온 쪽을 쓴다(일정이 옮겨간 경우를 따라가기 위해서다).
      int day =
          entry.getValue().entrySet().stream()
              .max(
                  Comparator.comparingInt(Map.Entry<Integer, Integer>::getValue)
                      .thenComparing(candidate -> latestByDay.get(candidate.getKey())))
              .map(Map.Entry::getKey)
              .orElse(0);
      if (day <= 0) {
        continue;
      }
      int[] bounds = boundsBySymbol.get(symbol);
      result.put(symbol, new PayDay(day, bounds[0], bounds[1], totalBySymbol.get(symbol)));
    }
    return result;
  }

  /**
   * 그 달의 실제 날짜로 옮긴다.
   *
   * <p>31 일에 지급하던 종목도 2 월에는 31 일이 없다 &mdash; 그 달의 마지막 날로 당긴다. 안 그러면 달력에서 그 종목이 통째로 사라진다.
   */
  public static int dayInMonth(int day, java.time.YearMonth month) {
    if (day <= 0 || month == null) {
      return 0;
    }
    return Math.min(day, month.lengthOfMonth());
  }

  /**
   * 추정한 지급일을 그 달 안의 영업일로 옮긴다 - 주말 · 공휴일이면 다음 영업일, 그 달 안에 없으면 앞 영업일.
   *
   * <p>추정일은 지급이력의 최빈 일자를 그 달에 그대로 놓은 것이라 요일을 보지 않았다. 실측 2026-10-14: 다가올 배당이 RISE 코리아밸류업을 10 월 17 일
   * <b>토요일</b>로 적었고(최빈 17 일), 대시보드도 "월중 · 10월 17일" 이었다. ETF 분배금은 영업일에만 들어온다 &mdash; 같은 날 기준일 종목들은
   * 10 월 19 일(월)로 잡혀 있었다. 달을 넘기지 않는 까닭: 다음 달로 넘기면 같은 종목이 다음 달 일정에 두 번 들어간다. 발표된 지급일(그 달 지급이력)은 옮기지
   * 않는다.
   *
   * <p>공휴일은 {@link net.luversof.web.gate.util.KoreanHolidays} 표(대체공휴일 포함)를 쓴다. 표가 없는 해는 주말만 본다.
   */
  public static LocalDate businessDayInMonth(LocalDate date) {
    if (date == null) {
      return null;
    }
    for (LocalDate day = date; day.getMonth() == date.getMonth(); day = day.plusDays(1)) {
      if (isBusinessDay(day)) {
        return day;
      }
    }
    for (LocalDate day = date.minusDays(1);
        day.getMonth() == date.getMonth();
        day = day.minusDays(1)) {
      if (isBusinessDay(day)) {
        return day;
      }
    }
    return date;
  }

  /** 토 · 일이 아니고 공휴일 표에 없는 날. */
  public static boolean isBusinessDay(LocalDate date) {
    java.time.DayOfWeek weekday = date.getDayOfWeek();
    return weekday != java.time.DayOfWeek.SATURDAY
        && weekday != java.time.DayOfWeek.SUNDAY
        && net.luversof.web.gate.util.KoreanHolidays.holidayName(date, java.util.Locale.KOREAN)
            == null;
  }

  /** 기준일과 지급일이 둘 다 있는 한 건(2026-10-20). */
  public record RecordedPayout(String symbol, LocalDate recordDate, LocalDate payDate) {}

  /**
   * 한 종목의 지급 규칙 &mdash; 기준일 규칙(월중 · 월말, {@link KrxRecordDateUtil#recordDate}) + 기준일 뒤 지급까지의 거래일 수.
   *
   * @param payoutWindow {@code MID_MONTH} · {@code MONTH_END}
   * @param lagTradingDays 기준일 다음 거래일부터 세어 지급일까지(대부분 2 - 결제일)
   * @param sampleCount 근거가 된 지급 건수
   */
  public record SettlementRule(String payoutWindow, int lagTradingDays, int sampleCount) {}

  /** 기준일 규칙을 확인할 최근 건수. 이만큼(적으면 있는 만큼, 최소 2) 모두 같은 규칙이어야 쓴다. */
  private static final int RULE_CHECK_RECENT = 3;

  /**
   * 종목별 지급 규칙(2026-10-20 자율 개선).
   *
   * <p>예상 지급일을 최빈 일자로 놓으면 달마다 1 ~ 3 일씩 어긋났다 &mdash; 지급일은 날짜가 아니라 <b>기준일 + 결제 거래일</b>로 정해지기 때문이다. 실측
   * 2026-10-20(지급 이력 209 건, 2025-10 ~ 2026-10, 그 전 이력만으로 맞히기): 최빈 일자 + 영업일 보정은 119 건(57%) 정확, 기준일
   * 규칙 + 종목별 거래일 수는 209 건(100%). 7 월 월중이 15 일(수) 기준 &rarr; 20 일(월) 지급이었던 것도 17 일 제헌절을 건너뛴 2 거래일이다.
   *
   * <p>최근 {@value #RULE_CHECK_RECENT} 건의 기준일이 모두 한 규칙(월중 15 일 · 월말 마지막 거래일, 아니면 앞 거래일)과 맞는 종목만 규칙을
   * 준다 &mdash; 안 맞는 종목은 예전 추정(최빈 일자)을 그대로 쓴다. 거래일 수는 최빈값(같으면 최근 것).
   */
  public static Map<String, SettlementRule> settlementRules(List<RecordedPayout> payouts) {
    Map<String, List<RecordedPayout>> bySymbol = new LinkedHashMap<>();
    if (payouts != null) {
      for (RecordedPayout payout : payouts) {
        if (payout == null
            || payout.recordDate() == null
            || payout.payDate() == null
            || payout.payDate().isBefore(payout.recordDate())) {
          continue;
        }
        String symbol = normalizeSymbol(payout.symbol());
        if (symbol != null) {
          bySymbol.computeIfAbsent(symbol, key -> new java.util.ArrayList<>()).add(payout);
        }
      }
    }
    Map<String, SettlementRule> rules = new LinkedHashMap<>();
    for (Map.Entry<String, List<RecordedPayout>> entry : bySymbol.entrySet()) {
      List<RecordedPayout> list = entry.getValue();
      list.sort(Comparator.comparing(RecordedPayout::recordDate));
      if (list.size() < 2) {
        continue;
      }
      List<RecordedPayout> recent =
          list.subList(Math.max(0, list.size() - RULE_CHECK_RECENT), list.size());
      String window = null;
      for (String candidate : new String[] {"MID_MONTH", "MONTH_END"}) {
        boolean all = true;
        for (RecordedPayout payout : recent) {
          LocalDate expected =
              KrxRecordDateUtil.recordDate(
                  java.time.YearMonth.from(payout.recordDate()), candidate);
          if (!payout.recordDate().equals(expected)) {
            all = false;
            break;
          }
        }
        if (all) {
          window = candidate;
          break;
        }
      }
      if (window == null) {
        continue;
      }
      Map<Integer, Integer> lagCount = new HashMap<>();
      Map<Integer, LocalDate> lagLatest = new HashMap<>();
      for (RecordedPayout payout : list) {
        int lag = KrxRecordDateUtil.tradingDaysBetween(payout.recordDate(), payout.payDate());
        lagCount.merge(lag, 1, Integer::sum);
        lagLatest.merge(
            lag, payout.recordDate(), (left, right) -> left.isAfter(right) ? left : right);
      }
      int lag =
          lagCount.entrySet().stream()
              .max(
                  Comparator.comparingInt(Map.Entry<Integer, Integer>::getValue)
                      .thenComparing(candidate -> lagLatest.get(candidate.getKey())))
              .map(Map.Entry::getKey)
              .orElse(-1);
      if (lag < 0 || lag > 10) {
        continue;
      }
      rules.put(entry.getKey(), new SettlementRule(window, lag, list.size()));
    }
    return rules;
  }

  /**
   * 그 달({@code payMonth})에 들어올 지급일. 월중은 그 달 기준일, 월말은 앞 달 기준일에서 센다. 지급일이 그 달을 벗어나면 null(예전 추정으로).
   */
  public static LocalDate projectedPayDate(java.time.YearMonth payMonth, SettlementRule rule) {
    if (payMonth == null || rule == null) {
      return null;
    }
    java.time.YearMonth recordMonth =
        "MID_MONTH".equals(rule.payoutWindow()) ? payMonth : payMonth.minusMonths(1);
    LocalDate record = KrxRecordDateUtil.recordDate(recordMonth, rule.payoutWindow());
    if (record == null) {
      return null;
    }
    LocalDate pay = KrxRecordDateUtil.plusTradingDays(record, rule.lagTradingDays());
    return java.time.YearMonth.from(pay).equals(payMonth) ? pay : null;
  }
}
