package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 배당 목록의 보유 손익 지도 여덟이 어느 행에서 나오는가(2026-10-22 dividendList 에서 addHoldingProfitAttributes 로 옮기며 처음 둔
 * 시험).
 *
 * <p>평가 금액 · 평가 손익은 지금 보유 계산의 <b>들고 있는 행</b>, 실현 손익은 <b>기간 계산</b>(기간을 골랐을 때), 지금까지 산 총액은 지금 보유 계산의
 * <b>모든 행</b>이다 - 셋이 섞이면 합산 수익률의 분자와 분모가 다른 범위에서 나온다.
 */
class StockDividendHtmxControllerHoldingProfitTest {

  private static final UUID A1 = UUID.randomUUID();
  private static final UUID A2 = UUID.randomUUID();
  private static final UUID S1 = UUID.randomUUID();
  private static final UUID S2 = UUID.randomUUID();

  private static TradeProfit row(
      UUID account,
      UUID stock,
      int quantity,
      String evaluation,
      String evaluationProfit,
      String realized,
      String buyCost) {
    return new TradeProfit(
        stock,
        "종목",
        account,
        "계좌",
        null,
        null,
        0,
        null,
        null,
        dec(realized),
        quantity,
        null,
        dec(evaluation),
        dec(evaluationProfit),
        null,
        null,
        null,
        null,
        dec(buyCost),
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static BigDecimal dec(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<UUID, BigDecimal> map(ExtendedModelMap model, String key) {
    return (Map<UUID, BigDecimal>) model.get(key);
  }

  @Test
  void 평가는_들고_있는_행_실현은_기간_계산_산_총액은_모든_행에서() {
    List<TradeProfit> current =
        List.of(
            row(A1, S1, 10, "1000", "100", "50", "900"),
            // 다 팔았다 - 평가 손익을 0 이 아닌 값으로 둔다(0 이면 모든 행을 더해도 같아 "들고 있는 행만" 을 못 본다)
            row(A1, S2, 0, "0", "-20", "30", "400"));
    List<TradeProfit> period = List.of(row(A2, S1, 0, null, null, "70", "600"));

    ExtendedModelMap model = new ExtendedModelMap();
    StockDividendHtmxController.addHoldingProfitAttributes(model, current, period);

    assertThat(model.keySet())
        .containsExactlyInAnyOrder(
            "currentValueByStock",
            "currentValueByAccount",
            "evaluationProfitByStock",
            "evaluationProfitByAccount",
            "realizedProfitByStock",
            "realizedProfitByAccount",
            "totalBuyCostByStock",
            "totalBuyCostByAccount");
    assertThat(map(model, "currentValueByStock")).containsOnlyKeys(S1);
    assertThat(map(model, "currentValueByStock").get(S1)).isEqualByComparingTo("1000");
    assertThat(map(model, "currentValueByAccount").get(A1)).isEqualByComparingTo("1000");
    assertThat(map(model, "evaluationProfitByStock")).containsOnlyKeys(S1);
    assertThat(map(model, "evaluationProfitByAccount").get(A1))
        .as("다 판 행의 평가 손익 -20 은 빠진다")
        .isEqualByComparingTo("100");
    assertThat(map(model, "realizedProfitByStock")).as("기간 계산의 실현 손익만").containsOnlyKeys(S1);
    assertThat(map(model, "realizedProfitByStock").get(S1)).isEqualByComparingTo("70");
    assertThat(map(model, "realizedProfitByAccount")).containsOnlyKeys(A2);
    assertThat(map(model, "totalBuyCostByStock").get(S1)).isEqualByComparingTo("900");
    assertThat(map(model, "totalBuyCostByStock").get(S2))
        .as("다 판 종목도 산 총액에 든다")
        .isEqualByComparingTo("400");
    assertThat(map(model, "totalBuyCostByAccount")).containsOnlyKeys(A1);
    assertThat(map(model, "totalBuyCostByAccount").get(A1)).isEqualByComparingTo("1300");
  }

  @Test
  void 기간을_고르지_않으면_실현_손익도_지금_보유_계산에서() {
    List<TradeProfit> current =
        List.of(
            row(A1, S1, 10, "1000", "100", "50", "900"), row(A1, S2, 0, "0", "-20", "30", "400"));

    ExtendedModelMap model = new ExtendedModelMap();
    StockDividendHtmxController.addHoldingProfitAttributes(model, current, current);

    assertThat(map(model, "realizedProfitByAccount").get(A1)).isEqualByComparingTo("80");
  }
}
