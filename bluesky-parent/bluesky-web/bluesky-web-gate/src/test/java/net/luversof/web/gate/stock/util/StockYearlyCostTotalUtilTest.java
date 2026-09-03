package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.YearlyCostSummary;

/**
 * 연도별 세금·비용 표의 <b>합계</b> 줄.
 *
 * <p>연말정산·금투세 때 필요한 수는 대개 "그 해" 가 아니라 "다 합쳐서 얼마" 인데, 표가 해마다 한 줄씩만 적고 있어 눈으로 더해야 했다.
 *
 * <p>성과 표와 달리 여기는 일곱 열이 모두 그냥 더하면 되는 값이다 &mdash; 곱해서 이어야 하는 수익률도, 합계라는 것이 없는 기말 평가액도 없다.
 */
class StockYearlyCostTotalUtilTest {

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 실데이터와 같은 모양: 실현손익이 양수인 해와 음수인 해, 배당이 아예 없던 해. */
  private static List<YearlyCostSummary> rows() {
    return List.of(
        new YearlyCostSummary(
            2026,
            bd("20933"),
            bd("616963"),
            bd("131261409"),
            bd("28580530"),
            bd("8040065"),
            bd("1418030"),
            bd("27162500")),
        new YearlyCostSummary(
            2025,
            bd("21141"),
            bd("157182"),
            bd("6085372"),
            bd("16632675"),
            bd("13705309"),
            bd("2108300"),
            bd("14524375")),
        // 매매만 있고 배당이 없던 해. 손실로 끝난 해이기도 하다.
        new YearlyCostSummary(
            2018,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            bd("-661700"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO));
  }

  /** 일곱 열이 모두 그냥 더한 값이다. */
  @Test
  void 일곱_열을_모두_더한다() {
    var totals = StockYearlyCostTotalUtil.of(rows());

    assertThat(totals.tradeFee()).isEqualByComparingTo(bd("42074"));
    assertThat(totals.tradeTax()).isEqualByComparingTo(bd("774145"));
    assertThat(totals.dividendGross()).isEqualByComparingTo(bd("45213205"));
    assertThat(totals.dividendTaxable()).isEqualByComparingTo(bd("21745374"));
    assertThat(totals.dividendTax()).isEqualByComparingTo(bd("3526330"));
    assertThat(totals.dividendNet()).isEqualByComparingTo(bd("41686875"));
  }

  /** 실현손익은 해마다 부호가 다르다. 절댓값을 더하면 손실 난 해가 이익처럼 얹힌다. */
  @Test
  void 실현손익은_부호를_살려_더한다() {
    var totals = StockYearlyCostTotalUtil.of(rows());

    assertThat(totals.realizedProfit())
        .as("131,261,409 + 6,085,372 - 661,700 = 136,685,081")
        .isEqualByComparingTo(bd("136685081"));
  }

  /**
   * 줄마다 성립하는 식이 합계에서도 성립한다.
   *
   * <p>세후 = 세전 - 세금 - 수수료 인데 배당 수수료는 늘 0 이라 세후 = 세전 - 세금 이다. 합계 줄이 이 식을 깨면 같은 표의 마지막 줄만 앞뒤가 안 맞는 것이
   * 된다. 실측 2026-09-03(14 개 해): 세후 합 65,652,134 = 세전 합 73,423,094 - 세금 합 7,770,960.
   */
  @Test
  void 세후_합은_세전_합에서_세금_합을_뺀_값이다() {
    var totals = StockYearlyCostTotalUtil.of(rows());

    assertThat(totals.dividendNet())
        .isEqualByComparingTo(totals.dividendGross().subtract(totals.dividendTax()));
  }

  /** null 값은 0 으로 읽는다. 원격이 어떤 해의 한 열을 못 낸 채 보낼 수 있다. */
  @Test
  void null_값과_null_줄을_0_으로_읽는다() {
    var totals =
        StockYearlyCostTotalUtil.of(
            Arrays.asList(
                new YearlyCostSummary(2026, null, null, bd("100"), null, null, null, null), null));

    assertThat(totals.realizedProfit()).isEqualByComparingTo(bd("100"));
    assertThat(totals.tradeFee()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** 빈 목록과 null 은 0 으로 답한다. */
  @Test
  void 자료가_없으면_0_이다() {
    List<List<YearlyCostSummary>> inputs = new java.util.ArrayList<>();
    inputs.add(List.of());
    inputs.add(null);
    for (List<YearlyCostSummary> input : inputs) {
      var totals = StockYearlyCostTotalUtil.of(input);
      assertThat(totals.realizedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(totals.dividendNet()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }
}
