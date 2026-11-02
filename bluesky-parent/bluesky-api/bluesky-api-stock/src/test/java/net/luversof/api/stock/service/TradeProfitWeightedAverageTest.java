package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.TradeProfit;

/**
 * 한 종목을 여러 계좌에 나눠 든 경우의 평단 합치기(mergeByAccount 의 수량 가중 평균). 시험 원장이 계좌 하나뿐이라 이 갈래를 보는 시험이 없었다(13차 검토
 * 2026-10-31).
 */
class TradeProfitWeightedAverageTest {

  private static TradeProfit part(String averageBuyPrice, int holdingQuantity) {
    TradeProfit part = new TradeProfit();
    part.setAverageBuyPrice(averageBuyPrice == null ? null : new BigDecimal(averageBuyPrice));
    part.setHoldingQuantity(holdingQuantity);
    return part;
  }

  private static BigDecimal weightedBuy(TradeProfit... parts) {
    return TradeProfitService.weighted(
        List.of(parts), TradeProfit::getAverageBuyPrice, TradeProfit::getHoldingQuantity);
  }

  @Test
  void 수량으로_가중한다() {
    // (100 x 10 + 200 x 30) / 40 = 175
    assertThat(weightedBuy(part("100", 10), part("200", 30))).isEqualByComparingTo("175");
  }

  @Test
  void 전량_매도한_계좌와_가격_없는_계좌는_빼고_스케일_10_으로_둔다() {
    assertThat(weightedBuy(part("100.00", 1), part("200.00", 2), part("999", 0), part(null, 5)))
        .as("1/3 자리에서 2 자리로 자르지 않는다")
        .isEqualByComparingTo("166.6666666667");
  }

  @Test
  void 모두_보유_0_이면_0() {
    assertThat(weightedBuy(part("100", 0), part(null, 3))).isEqualByComparingTo("0");
  }
}
