package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * 활동 내역 월별 막대 두 개(배당 · 실현손익)의 칸.
 *
 * <p>예전에는 활동이 있는 달만 칸으로 그려 2010-03 막대 바로 옆에 2014-11 막대가 붙었다(실측 2026-10-27 전체 기간: 203 개월 중 132 개월
 * 생략, 안내 문구로만 밝힘). 매매 화면 월별 막대(stock-charts.ts buildMonthlyData)와 같은 규칙으로 바꾼다 - 처음 달부터 끝 달까지 빈 달도
 * 칸을 두고, 60 개월을 넘으면 해 단위로 묶는다(빈 해도 칸이 남아 시간축이 맞다).
 */
public final class ActivityChartBuckets {

  /** 이 개수를 넘으면 해 단위. 매매 화면 MONTHLY_BAR_LIMIT 와 같은 값. */
  static final int MONTH_LIMIT = 60;

  private ActivityChartBuckets() {}

  /** 칸 라벨과 두 값(배당 · 실현손익). */
  public record Buckets(
      List<String> labels, List<BigDecimal> dividend, List<BigDecimal> realized) {}

  /**
   * 달별 합(값 [0] 배당, [1] 실현손익)을 칸으로 바꾼다.
   *
   * @param monthSums 활동 있는 달만 들어 있는 달별 합(달 순)
   */
  public static Buckets of(SortedMap<YearMonth, BigDecimal[]> monthSums) {
    if (monthSums == null || monthSums.isEmpty()) {
      return new Buckets(List.of(), List.of(), List.of());
    }
    YearMonth first = monthSums.firstKey();
    YearMonth last = monthSums.lastKey();
    boolean byYear = java.time.temporal.ChronoUnit.MONTHS.between(first, last) + 1 > MONTH_LIMIT;
    Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
    for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
      String label =
          byYear
              ? String.valueOf(month.getYear())
              : month.getYear() + "." + String.format("%02d", month.getMonthValue());
      BigDecimal[] sum =
          buckets.computeIfAbsent(label, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      BigDecimal[] monthSum = monthSums.get(month);
      if (monthSum != null) {
        sum[0] = sum[0].add(monthSum[0]);
        sum[1] = sum[1].add(monthSum[1]);
      }
    }
    List<String> labels = new ArrayList<>(buckets.keySet());
    List<BigDecimal> dividend = new ArrayList<>();
    List<BigDecimal> realized = new ArrayList<>();
    buckets
        .values()
        .forEach(
            sum -> {
              dividend.add(sum[0]);
              realized.add(sum[1]);
            });
    return new Buckets(labels, dividend, realized);
  }

  /** 인라인 스크립트용: activityChartLabels · activityChartDividend · activityChartRealized 세 배열. */
  public static String js(SortedMap<YearMonth, BigDecimal[]> monthSums) {
    Buckets buckets = of(monthSums);
    return "var activityChartLabels="
        + ChartSeriesJs.stringArray(buckets.labels())
        + ";var activityChartDividend=["
        + join(buckets.dividend())
        + "];var activityChartRealized=["
        + join(buckets.realized())
        + "];";
  }

  private static String join(List<BigDecimal> values) {
    StringBuilder sb = new StringBuilder();
    for (BigDecimal value : values) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(value.toPlainString());
    }
    return sb.toString();
  }
}
