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
}
