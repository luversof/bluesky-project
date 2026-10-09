package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.PayoutTrendCalculator.PayoutTrend;

/** 분배금 추세(사용자 결정 2026-09-22: 최근 3 회 평균을 최근 12 회 평균과 견준다). */
class PayoutTrendCalculatorTest {

  /** 최근 것이 앞. 실제 저장소도 지급일 내림차순으로 준다. */
  private static List<BigDecimal> amounts(String... values) {
    List<BigDecimal> list = new ArrayList<>();
    for (String value : values) {
      list.add(value == null ? null : new BigDecimal(value));
    }
    return list;
  }

  private static List<BigDecimal> repeat(String value, int count) {
    List<BigDecimal> list = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      list.add(new BigDecimal(value));
    }
    return list;
  }

  @Test
  void 하한이_최근_구간보다_커야_견줄_것이_남는다() {
    // 계산기는 이 전제 위에 서 있다. 하한을 3 이하로 내리면 최근 3 회와 견줄 구간이 같은 건들이 돼
    // 추세가 항상 0.00% 로 나온다 - 그 0 은 "안정적" 으로 읽혀 거짓이 된다.
    assertThat(PayoutTrendCalculator.MINIMUM_PAYOUTS)
        .as("하한이 최근 구간보다 커야 견줄 평균에 다른 건이 섞인다")
        .isGreaterThan(PayoutTrendCalculator.RECENT_COUNT);
    assertThat(PayoutTrendCalculator.BASE_COUNT).isGreaterThan(PayoutTrendCalculator.RECENT_COUNT);
  }

  @Test
  void 줄어드는_분배금을_음수로_알린다() {
    // 최근 3 회 100 원, 그 앞 9 회 200 원 => 12 회 평균 175 원, 최근 3 회는 그보다 42.86% 적다.
    List<BigDecimal> values = new ArrayList<>(repeat("100", 3));
    values.addAll(repeat("200", 9));

    PayoutTrend trend = PayoutTrendCalculator.compute(values);

    assertThat(trend).isNotNull();
    assertThat(trend.recentAveragePerShare()).isEqualByComparingTo("100.00");
    assertThat(trend.baseAveragePerShare()).isEqualByComparingTo("175.00");
    assertThat(trend.changePct()).as("(100 - 175) / 175").isEqualByComparingTo("-42.86");
    assertThat(trend.recentCount()).isEqualTo(3);
    assertThat(trend.baseCount()).isEqualTo(12);
  }

  @Test
  void 늘어나는_분배금을_양수로_알린다() {
    List<BigDecimal> values = new ArrayList<>(repeat("300", 3));
    values.addAll(repeat("100", 9));

    PayoutTrend trend = PayoutTrendCalculator.compute(values);

    assertThat(trend).isNotNull();
    assertThat(trend.changePct()).as("(300 - 150) / 150").isEqualByComparingTo("100.00");
  }

  @Test
  void 늘_같은_금액이면_0이다() {
    PayoutTrend trend = PayoutTrendCalculator.compute(repeat("33", 12));

    assertThat(trend).isNotNull();
    assertThat(trend.changePct()).isEqualByComparingTo("0.00");
  }

  @Test
  void 지급이_모자라면_추세를_내지_않는다() {
    // 두 회뿐이면 최근 3 회와 최근 12 회가 같은 두 건이라 언제나 0.00% 가 된다 - 그 0 은 "안정적" 으로 읽혀 거짓이다.
    assertThat(PayoutTrendCalculator.compute(amounts("160", "160"))).isNull();
    assertThat(PayoutTrendCalculator.compute(repeat("100", 5)))
        .as("다섯 회도 최근 3 회가 견줄 구간의 대부분이다")
        .isNull();
    assertThat(PayoutTrendCalculator.compute(repeat("100", 6))).as("여섯 회부터 낸다").isNotNull();
  }

  @Test
  void 열두_회가_안_되면_있는_만큼으로_견준다() {
    // 여덟 회뿐이면 최근 3 회 대 최근 8 회다. 그 사실을 센 횟수로 알린다.
    List<BigDecimal> values = new ArrayList<>(repeat("50", 3));
    values.addAll(repeat("100", 5));

    PayoutTrend trend = PayoutTrendCalculator.compute(values);

    assertThat(trend).isNotNull();
    assertThat(trend.baseCount()).isEqualTo(8);
    assertThat(trend.baseAveragePerShare()).isEqualByComparingTo("81.25");
    assertThat(trend.changePct()).isEqualByComparingTo("-38.46");
  }

  @Test
  void 견줄_평균이_0이면_내지_않는다() {
    // 0 으로 나누면 터진다. 분배금이 전부 0 인 이력은 추세를 말할 수 없다.
    assertThat(PayoutTrendCalculator.compute(repeat("0", 12))).isNull();
  }

  @Test
  void 빈_값과_없는_목록을_견딘다() {
    assertThat(PayoutTrendCalculator.compute(null)).isNull();
    assertThat(PayoutTrendCalculator.compute(List.of())).isNull();
    assertThat(PayoutTrendCalculator.compute(Arrays.asList(null, null, null, null, null, null)))
        .as("전부 빈 값이면 셀 것이 없다")
        .isNull();
  }

  @Test
  void 반올림_전_평균으로_견준다() {
    // 평균을 먼저 2 자리로 줄이고 나누면 두 번 반올림한 수가 된다. 금액이 작을수록 그 차가 벌어져 여기서 드러난다.
    List<BigDecimal> values = new ArrayList<>(amounts("1", "1", "2"));
    values.addAll(repeat("1", 9));

    PayoutTrend trend = PayoutTrendCalculator.compute(values);

    assertThat(trend).isNotNull();
    assertThat(trend.recentAveragePerShare()).as("4/3 = 1.3333...").isEqualByComparingTo("1.33");
    assertThat(trend.baseAveragePerShare()).as("13/12 = 1.0833...").isEqualByComparingTo("1.08");
    // 반올림 전: (1.3333... - 1.0833...) / 1.0833... = 23.08%. 1.33 과 1.08 로 내면 23.15% 가 된다.
    assertThat(trend.changePct()).isEqualByComparingTo("23.08");
  }

  private static List<BigDecimal> desc(String... values) {
    return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
  }

  @Test
  void 감소_횟수는_직전_회보다_줄어든_것만_센다() {
    // 최근 것이 앞: 100 <- 90 <- 100 <- 100 <- 110 <- 100 (오래된 것이 뒤)
    // 오래된 쪽부터: 100 -> 110(증가) -> 100(감소) -> 100 -> 90(감소) -> 100(증가)
    var cuts = PayoutTrendCalculator.countCuts(desc("100", "90", "100", "100", "110", "100"));
    assertThat(cuts.cutCount()).isEqualTo(2);
    assertThat(cuts.pairCount()).isEqualTo(5);
  }

  @Test
  void 크게_늘어난_종목은_깎이지_않는다() {
    // 변동계수로 재면 들쭉날쭉이지만 줄어든 적은 없다(실측 0094M0 같은 증가형)
    var cuts = PayoutTrendCalculator.countCuts(desc("200", "180", "150", "120", "100", "100"));
    assertThat(cuts.cutCount()).isZero();
  }

  @Test
  void 평균의_1퍼센트_이하로_줄어든_끝수는_세지_않는다() {
    // 평균 약 100 - 99.5 로 0.5 줄어든 것은 끝수, 98 로 2 줄어든 것은 감소
    var cuts = PayoutTrendCalculator.countCuts(desc("98", "100", "99.5", "100", "100", "100"));
    assertThat(cuts.cutCount()).isEqualTo(1);
  }

  @Test
  void 최근_12회만_보고_이력이_모자라면_내지_않는다() {
    assertThat(PayoutTrendCalculator.countCuts(desc("100", "90", "80", "70", "60"))).isNull();
    assertThat(PayoutTrendCalculator.countCuts(null)).isNull();
    // 13 회째(가장 오래된 것)의 감소는 창 밖이다: 12 회는 모두 같고, 13 회째 200 -> 100 은 안 센다
    var cuts =
        PayoutTrendCalculator.countCuts(
            desc(
                "100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100", "100",
                "200"));
    assertThat(cuts.cutCount()).isZero();
    assertThat(cuts.pairCount()).isEqualTo(11);
  }
}
