package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/**
 * 배당수익률의 <b>분모</b>가 되는 보유 원금 계산을 고정한다.
 *
 * <p>매도로 빠져나가는 원가(COGS)는 증권사 기록 실현손익에서 역산하는데, 기록 실현손익의 정의는 {@code 매도금액 - 원가 - 세금} 으로 <b>매도 수수료는 빼지
 * 않는다</b>. 실측(사용자 매도 중 실현손익이 기록된 54 건 전부): 40 건이 이 정의와 1 원 이내로 일치했고 수수료까지 뺀 정의와 일치한 건은 0 건이었다.
 *
 * <p>실수령(수수료까지 뺀 금액)에서 역산하면 COGS 가 수수료만큼 작아져 원금이 부풀어 남는다. 실측: 이 화면의 삼성전자 원금이 362,531,274 로 계산돼
 * api-stock 의 362,525,079 보다 6,195 컸다(= 마지막 전량매도 이후 매도 수수료 4,611 + 1,584). 분모가 크면 배당수익률이 낮게 표시된다.
 */
class CostBasisStateTest {

  private static TradeResponse trade(
      TradeType type, int quantity, String price, String fee, String tax, String realizedProfit) {
    return new TradeResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "종목",
        type,
        quantity,
        new BigDecimal(price),
        new BigDecimal(fee),
        new BigDecimal(tax),
        new BigDecimal(price).multiply(BigDecimal.valueOf(quantity)),
        realizedProfit == null ? null : new BigDecimal(realizedProfit),
        Instant.parse("2026-01-02T00:00:00Z"));
  }

  /** 삼성전자 2026-02-11 실측: 868주 x 166,900, 수수료 4,611, 세금 289,707, 기록손익 82,181,760. */
  @Test
  void 매도_원가는_수수료를_되돌려주지_않는다() {
    var state = new StockDividendHtmxController.CostBasisState();
    // 원가 200,000,000 짜리 보유를 만든 뒤 실측 매도를 적용한다.
    state.apply(trade(TradeType.BUY, 1000, "200000", "0", "0", null));
    assertThat(state.averageCost()).isEqualByComparingTo("200000.00");

    state.apply(trade(TradeType.SELL, 868, "166900", "4611", "289707", "82181760"));

    // COGS = 144,869,200 - 289,707 - 82,181,760 = 62,397,733
    // 남는 원금 = 200,000,000 - 62,397,733 = 137,602,267, 남는 수량 132주
    assertThat(state.rawQuantity()).isEqualTo(132);
    assertThat(state.averageCost())
        .as("수수료를 실수령에서 빼면 원가가 4,611 만큼 덜 빠져 분모가 부풀어 오른다")
        .isEqualByComparingTo(
            new BigDecimal("137602267")
                .divide(BigDecimal.valueOf(132), 2, java.math.RoundingMode.HALF_UP));
  }

  /**
   * 두 정의의 차이가 정확히 매도 수수료임을 총액으로 보인다.
   *
   * <p>평균원가는 2 자리로 반올림되므로 (평균 x 수량) 으로 총액을 되돌리면 오차가 섞인다. 총액끼리 비교한다.
   */
  @Test
  void 실수령에서_역산하면_수수료만큼_원금이_남는다() {
    BigDecimal sellAmount = new BigDecimal("144869200");
    BigDecimal fee = new BigDecimal("4611");
    BigDecimal tax = new BigDecimal("289707");
    BigDecimal recordedProfit = new BigDecimal("82181760");

    BigDecimal correctCogs = sellAmount.subtract(tax).subtract(recordedProfit);
    BigDecimal legacyCogs = sellAmount.subtract(fee).subtract(tax).subtract(recordedProfit);

    assertThat(correctCogs.subtract(legacyCogs))
        .as("실수령에서 역산하면 원가를 수수료만큼 덜 뺀다")
        .isEqualByComparingTo(fee);

    // 그 차이가 그대로 남은 원금에 얹힌다.
    BigDecimal opening = new BigDecimal("200000000");
    assertThat(opening.subtract(legacyCogs).subtract(opening.subtract(correctCogs)))
        .isEqualByComparingTo(fee);
  }

  @Test
  void 전량_매도하면_원금과_수량이_0으로_정리된다() {
    var state = new StockDividendHtmxController.CostBasisState();
    state.apply(trade(TradeType.BUY, 100, "1000", "0", "0", null));
    state.apply(trade(TradeType.SELL, 100, "1500", "75", "150", "49850"));

    assertThat(state.rawQuantity()).isZero();
    assertThat(state.averageCost()).as("보유가 없으면 평균원가는 만들지 않는다").isNull();
  }

  @Test
  void 보유량보다_많이_팔아도_음수로_남지_않는다() {
    var state = new StockDividendHtmxController.CostBasisState();
    state.apply(trade(TradeType.BUY, 100, "1000", "0", "0", null));
    state.apply(trade(TradeType.SELL, 250, "1500", "0", "0", "100000"));

    assertThat(state.rawQuantity()).isZero();
    assertThat(state.averageCost()).isNull();
  }

  @Test
  void 수량이_0이하인_거래는_무시한다() {
    var state = new StockDividendHtmxController.CostBasisState();
    state.apply(trade(TradeType.BUY, 0, "1000", "0", "0", null));
    state.apply(trade(TradeType.SELL, -5, "1000", "0", "0", "0"));

    assertThat(state.rawQuantity()).isZero();
    assertThat(state.averageCost()).isNull();
  }

  /** 매수만 있으면 평균원가는 단순 가중평균이다(수수료는 원가에 넣지 않는다). */
  @Test
  void 매수만_있으면_가중평균이다() {
    var state = new StockDividendHtmxController.CostBasisState();
    state.apply(trade(TradeType.BUY, 100, "1000", "100", "0", null));
    state.apply(trade(TradeType.BUY, 100, "1200", "120", "0", null));

    assertThat(state.rawQuantity()).isEqualTo(200);
    assertThat(state.averageCost()).isEqualByComparingTo("1100.00");
  }
}
