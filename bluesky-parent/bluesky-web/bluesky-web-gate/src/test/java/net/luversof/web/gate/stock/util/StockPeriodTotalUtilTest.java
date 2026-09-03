package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 구간별 성과 표의 <b>전체 기간</b> 줄.
 *
 * <p>표는 줄마다의 성과만 적고 다 합치면 얼마인지가 없었다. 문제는 열마다 합치는 규칙이 다르다는 점이다 &mdash; 수익률을 더하면 복리를 놓쳐 실제보다 작게 나오고,
 * 기말 평가액은 아예 합계라는 것이 없다.
 */
class StockPeriodTotalUtilTest {

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 이 검사가 쓰는 값만 채운 요약. 나머지는 null 이어도 합계 계산에 쓰이지 않는다. */
  private static TradeProfitTimeSeriesSummary summary(
      String profit, String unrealizedStart, String unrealizedEnd, String principal, Double twr) {
    return new TradeProfitTimeSeriesSummary(
        null, // openingValue
        null, // closingValue
        null, // growthRatePct
        twr, // timeWeightedReturnPct
        profit == null ? null : bd(profit), // periodProfit
        principal == null ? null : bd(principal), // principalDelta
        unrealizedStart == null ? null : bd(unrealizedStart),
        unrealizedEnd == null ? null : bd(unrealizedEnd),
        // 아래는 합계 계산에 쓰이지 않는 나머지 12 개.
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private List<TradeProfitTimeSeriesSummary> threeYears() {
    return List.of(
        summary("300", "1000", "1200", "50", 20.0d),
        summary("-100", "800", "1000", "30", -10.0d),
        summary("500", "0", "800", "700", 50.0d));
  }

  /** 손익·원금 변동은 그냥 더한다. */
  @Test
  void 금액은_더한다() {
    var totals = StockPeriodTotalUtil.of(threeYears());

    assertThat(totals.profit()).isEqualByComparingTo(bd("700"));
    assertThat(totals.principalDelta()).isEqualByComparingTo(bd("780"));
  }

  /**
   * 수익률은 <b>곱해서 잇는다</b>.
   *
   * <p>1.20 x 0.90 x 1.50 = 1.62 이므로 +62%. 그냥 더하면 20-10+50 = +60% 로 <b>복리를 놓쳐</b> 작게 나온다. 실측
   * 2026-09-03: 전체 기간 15 행의 연쇄곱 1463.6231% 가 요약 카드의 투자 수익률과 소수점 넷째 자리까지 같았다.
   */
  @Test
  void 수익률은_곱해서_잇는다() {
    Double chained = StockPeriodTotalUtil.of(threeYears()).chainedReturnPct();

    assertThat(chained).isNotNull();
    assertThat(chained).isCloseTo(62.0d, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(chained).as("더하면 +60% 가 되어 복리를 놓친다").isNotEqualTo(60.0d);
  }

  /**
   * 평가 변동은 구간이 맞닿아 있어 <b>망원경처럼 접힌다</b>.
   *
   * <p>(1200-1000) + (1000-800) + (800-0) = 1200 은 전체 기간의 0 -> 1200 과 같다. 실현+배당은 손익에서 이 값을 뺀 잔차다.
   */
  @Test
  void 평가_변동은_접혀서_전체_기간의_변동이_된다() {
    var totals = StockPeriodTotalUtil.of(threeYears());

    assertThat(totals.unrealizedDelta()).isEqualByComparingTo(bd("1200"));
    assertThat(totals.realizedAndDividend())
        .as("손익 = 평가 변동 + (실현+배당) - 두 자리가 같은 뜻을 말해야 한다")
        .isEqualByComparingTo(bd("-500"));
  }

  /** 자본이 없던 구간은 수익률이 없다(null). 배수 1 이라 건너뛴다 - 0% 로 치면 결과가 같지만, 없는 값을 0 으로 읽는 습관은 다른 곳에서 틀린다. */
  @Test
  void 수익률이_없는_구간은_건너뛴다() {
    var totals =
        StockPeriodTotalUtil.of(
            List.of(summary("100", null, null, null, 20.0d), summary("0", null, null, null, null)));

    assertThat(totals.chainedReturnPct())
        .isCloseTo(20.0d, org.assertj.core.data.Offset.offset(1e-9));
  }

  /** 수익률을 낼 수 있는 구간이 하나도 없으면 null 이다. 0% 로 내면 '원금 그대로' 라는 뜻이 되어 거짓말이 된다. */
  @Test
  void 수익률을_낼_수_없으면_null_이다() {
    var totals = StockPeriodTotalUtil.of(List.of(summary("0", null, null, null, null)));

    assertThat(totals.chainedReturnPct()).isNull();
    assertThat(totals.profit()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** 더하기도 곱하기도 순서를 타지 않는다. 표는 최신이 위, 계산은 순서 무관이어야 한다. */
  @Test
  void 줄_순서를_타지_않는다() {
    var forward = StockPeriodTotalUtil.of(threeYears());
    var reversed = StockPeriodTotalUtil.of(threeYears().reversed());

    assertThat(reversed.profit()).isEqualByComparingTo(forward.profit());
    assertThat(reversed.chainedReturnPct())
        .isCloseTo(forward.chainedReturnPct(), org.assertj.core.data.Offset.offset(1e-9));
  }

  /** 빈 목록과 null 은 0 으로 답한다 - 표가 없을 때 부르는 자리가 있다. */
  @Test
  void 자료가_없으면_0_이다() {
    List<List<TradeProfitTimeSeriesSummary>> inputs = new java.util.ArrayList<>();
    inputs.add(List.of());
    inputs.add(null);
    for (List<TradeProfitTimeSeriesSummary> input : inputs) {
      var totals = StockPeriodTotalUtil.of(input);
      assertThat(totals.profit()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(totals.chainedReturnPct()).isNull();
    }
  }

  /** null 줄이 섞여도 죽지 않는다. 원격이 요약을 못 낸 구간이 그렇게 온다. */
  @Test
  void null_줄을_건너뛴다() {
    var totals =
        StockPeriodTotalUtil.of(Arrays.asList(summary("100", null, null, null, 10.0d), null));

    assertThat(totals.profit()).isEqualByComparingTo(bd("100"));
  }
}
