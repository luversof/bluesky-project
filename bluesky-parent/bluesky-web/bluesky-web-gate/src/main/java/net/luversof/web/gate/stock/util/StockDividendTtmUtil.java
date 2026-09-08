package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.luversof.web.gate.stock.dto.response.DividendResponse;

/**
 * 월별 <b>최근 12 개월 배당 합</b>(TTM).
 *
 * <p>배당 화면의 월별 막대는 "이번 달에 얼마 받았나" 를 답하지만, 배당 ETF 를 늘려 가는 사람에게 필요한 건 "12 개월 합이 매달 어떻게 자라나" 다. 막대만으로는
 * 4 월 500 만 · 5 월 430 만 같은 들쭉날쭉만 보이고 추세가 안 보였다(실측 2026-09-08).
 *
 * <p>표시 기간이 '올해' 면 브라우저에는 올해 배당만 실려 있어 1 월의 12 개월 합을 낼 수 없다. 그래서 서버가 <b>전체 원장</b>으로 달마다 합을 내어 넘긴다.
 * 달의 기준은 차트와 같은 <b>실지급일</b>이고, 실지급일이 없는 배당은 차트처럼 빠진다.
 */
public final class StockDividendTtmUtil {

  private StockDividendTtmUtil() {}

  /**
   * 첫 배당 달부터 마지막 배당 달까지 빠짐없이, 달 &rarr; 그 달을 끝으로 하는 12 개월 세후 합.
   *
   * @return 키는 {@code yyyy-MM}, 달 순서
   */
  public static Map<String, BigDecimal> byMonth(List<DividendResponse> dividends, ZoneId zone) {
    TreeMap<YearMonth, BigDecimal> perMonth = new TreeMap<>();
    if (dividends != null) {
      for (DividendResponse dividend : dividends) {
        if (dividend == null || dividend.payDate() == null) {
          continue;
        }
        YearMonth month = YearMonth.from(dividend.payDate().atZone(zone));
        BigDecimal net = dividend.netAmount() != null ? dividend.netAmount() : BigDecimal.ZERO;
        perMonth.merge(month, net, BigDecimal::add);
      }
    }
    Map<String, BigDecimal> ttm = new LinkedHashMap<>();
    if (perMonth.isEmpty()) {
      return ttm;
    }
    YearMonth first = perMonth.firstKey();
    YearMonth last = perMonth.lastKey();
    for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
      BigDecimal sum = BigDecimal.ZERO;
      for (Map.Entry<YearMonth, BigDecimal> e :
          perMonth.subMap(month.minusMonths(11), true, month, true).entrySet()) {
        sum = sum.add(e.getValue());
      }
      ttm.put(month.toString(), sum);
    }
    return ttm;
  }

  /** {@code {"2026-01":123,...}} &mdash; 조각의 인라인 스크립트에 그대로 심는다. 키는 yyyy-MM 이라 이스케이프할 것이 없다. */
  public static String toJs(Map<String, BigDecimal> ttm) {
    StringBuilder sb = new StringBuilder("{");
    boolean firstEntry = true;
    for (Map.Entry<String, BigDecimal> e : ttm.entrySet()) {
      if (!firstEntry) {
        sb.append(',');
      }
      firstEntry = false;
      sb.append('"').append(e.getKey()).append("\":").append(e.getValue().toPlainString());
    }
    return sb.append('}').toString();
  }
}
