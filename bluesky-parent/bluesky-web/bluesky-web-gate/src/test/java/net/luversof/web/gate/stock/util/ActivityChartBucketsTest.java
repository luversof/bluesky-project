package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/** 활동 내역 월별 막대 칸: 빈 달 채우기 · 60 개월 넘으면 해 단위 합. */
class ActivityChartBucketsTest {

  private static BigDecimal[] sum(long dividend, long realized) {
    return new BigDecimal[] {BigDecimal.valueOf(dividend), BigDecimal.valueOf(realized)};
  }

  @Test
  void 빈_달은_0_으로_채운다() {
    TreeMap<YearMonth, BigDecimal[]> sums = new TreeMap<>();
    sums.put(YearMonth.of(2026, 1), sum(100, 0));
    sums.put(YearMonth.of(2026, 3), sum(50, -20));

    var buckets = ActivityChartBuckets.of(sums);

    assertThat(buckets.labels()).containsExactly("2026.01", "2026.02", "2026.03");
    assertThat(buckets.dividend())
        .containsExactly(BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(50));
    assertThat(buckets.realized())
        .containsExactly(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(-20));
  }

  @Test
  void 육십_개월까지는_달_넘으면_해_단위_합() {
    TreeMap<YearMonth, BigDecimal[]> sixty = new TreeMap<>();
    sixty.put(YearMonth.of(2021, 1), sum(1, 0));
    sixty.put(YearMonth.of(2025, 12), sum(1, 0));
    assertThat(ActivityChartBuckets.of(sixty).labels()).hasSize(60);

    TreeMap<YearMonth, BigDecimal[]> sums = new TreeMap<>();
    sums.put(YearMonth.of(2020, 12), sum(10, 5));
    sums.put(YearMonth.of(2025, 2), sum(7, 0));
    sums.put(YearMonth.of(2025, 12), sum(3, -1));

    var buckets = ActivityChartBuckets.of(sums);

    assertThat(buckets.labels()).containsExactly("2020", "2021", "2022", "2023", "2024", "2025");
    assertThat(buckets.dividend().get(5)).isEqualByComparingTo("10");
    assertThat(buckets.realized().get(5)).isEqualByComparingTo("-1");
    assertThat(buckets.dividend().get(2)).isEqualByComparingTo("0");
  }

  @Test
  void 활동이_없으면_칸도_없다() {
    assertThat(ActivityChartBuckets.of(new TreeMap<>()).labels()).isEmpty();
    assertThat(ActivityChartBuckets.js(new TreeMap<>()))
        .isEqualTo(
            "var activityChartLabels=[];var activityChartDividend=[];var activityChartRealized=[];");
  }
}
