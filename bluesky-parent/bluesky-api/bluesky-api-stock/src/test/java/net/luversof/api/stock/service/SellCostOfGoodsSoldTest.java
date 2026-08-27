package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * 매도 시 보유 원가에서 빼는 금액(COGS)의 기준을 고정한다.
 *
 * <p>시계열은 매도할 때 WMA 를 다시 계산하지 않고, 증권사가 기록한 실현손익에서 원가를 역산한다. 기록 실현손익의 정의는 {@code 매도금액 - 원가 - 세금} 이라
 * <b>매도 수수료를 빼지 않는다</b>. 실수령(수수료까지 뺀 금액)에서 역산하면 COGS 가 수수료만큼 작아져 보유 원가가 계속 부풀어 남는다.
 *
 * <p>실측(사용자 매도 54건): 40건이 "수수료 미차감" 정의와 1원 이내로 일치했고 "수수료 차감" 정의와 일치한 건은 0건이었다. 부푼 원가는 한 종목에서 관측됐고,
 * 그 크기가 마지막 전량매도 이후 매도 수수료 합과 1원 오차 없이 같았다.
 */
class SellCostOfGoodsSoldTest {

  /** 실제 매도와 같은 모양의 표본: 800주 x 150,000 = 120,000,000, 수수료 4,000, 세금 240,000. */
  @Test
  void 매도의_원가는_수수료를_되돌려주지_않는다() {
    BigDecimal sellAmount = new BigDecimal("120000000");
    BigDecimal tax = new BigDecimal("240000");
    BigDecimal recordedProfit = new BigDecimal("60000000");

    BigDecimal cogs = TradeProfitService.costOfGoodsSold(sellAmount, tax, recordedProfit);

    // 매도금액 - 세금 - 기록손익 = 원가 (수수료 4,000 은 개입하지 않는다)
    assertEquals(0, new BigDecimal("59760000").compareTo(cogs));
  }

  /**
   * 수수료를 뺀 실수령에서 역산하던 예전 방식과의 차이가 정확히 수수료라는 것.
   *
   * <p>이 차이가 매도할 때마다 보유 원가에 남아 쌓인다(전량매도 시 0 으로 정리되므로 마지막 전량매도 이후의 수수료 합만 남는다).
   */
  @Test
  void 실수령에서_역산하면_수수료만큼_원가를_덜_뺀다() {
    BigDecimal sellAmount = new BigDecimal("120000000");
    BigDecimal fee = new BigDecimal("4000");
    BigDecimal tax = new BigDecimal("240000");
    BigDecimal recordedProfit = new BigDecimal("60000000");

    BigDecimal correct = TradeProfitService.costOfGoodsSold(sellAmount, tax, recordedProfit);
    BigDecimal legacy = sellAmount.subtract(fee).subtract(tax).subtract(recordedProfit);

    assertEquals(0, fee.compareTo(correct.subtract(legacy)));
  }

  @Test
  void 값이_없으면_0으로_본다() {
    assertEquals(
        0,
        new BigDecimal("1000")
            .compareTo(TradeProfitService.costOfGoodsSold(new BigDecimal("1000"), null, null)));
    assertEquals(
        0, BigDecimal.ZERO.compareTo(TradeProfitService.costOfGoodsSold(null, null, null)));
  }

  /** 손실 매도(기록손익 음수)면 원가가 매도금액보다 크다. */
  @Test
  void 손실_매도는_원가가_매도금액보다_크다() {
    BigDecimal cogs =
        TradeProfitService.costOfGoodsSold(
            new BigDecimal("1000000"), new BigDecimal("2000"), new BigDecimal("-150000"));
    assertEquals(0, new BigDecimal("1148000").compareTo(cogs));
  }
}
