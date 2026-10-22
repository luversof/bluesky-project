package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/** 자산 현황 계좌 표의 재료(2026-10-22 assetStatus 에서 accountStatusTables 로 옮기며 처음 둔 시험). */
class StockPortfolioAccountStatusTablesTest {

  // 고정 id - 묶기(groupingBy)의 해시 차례(BROKERAGE 1 → PENSION 2)가 이름 차례(연금저축 → 위탁)와 반대여야 정렬을 본다.
  private static final UUID PENSION = new UUID(0, 2);
  private static final UUID BROKERAGE = new UUID(0, 1);
  private static final UUID S1 = UUID.randomUUID();
  private static final UUID S2 = UUID.randomUUID();

  private static TradeProfit holding(
      UUID account,
      String accountName,
      UUID stock,
      String stockName,
      int quantity,
      String avgBuy,
      String evaluation,
      String evaluationProfit) {
    return new TradeProfit(
        stock,
        stockName,
        account,
        accountName,
        null,
        new BigDecimal(avgBuy),
        0,
        null,
        null,
        BigDecimal.ZERO,
        quantity,
        null,
        new BigDecimal(evaluation),
        new BigDecimal(evaluationProfit),
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

  private static StockPortfolioHtmxController controller() {
    return new StockPortfolioHtmxController(null, null, null, null, null, null, null, null);
  }

  @Test
  void 수동_원금이_있으면_그것이_수익률_기준이다() {
    List<TradeProfit> holdings =
        List.of(
            holding(PENSION, "연금저축", S1, "A", 10, "1000", "12000", "2000"),
            holding(BROKERAGE, "위탁", S2, "B", 5, "2000", "9000", "-1000"));

    var tables =
        controller()
            .accountStatusTables(
                holdings, Map.of(PENSION, new BigDecimal("11000")), new BigDecimal("21000"));

    assertThat(tables.accountProfitBasisMap().get(PENSION)).isEqualByComparingTo("11000");
    assertThat(tables.accountProfitBasisMap().get(BROKERAGE))
        .as("수동 원금이 없으면 지금 보유 원가(평가 - 평가 손익)")
        .isEqualByComparingTo("10000");
    assertThat(tables.manualPrincipalAccountIds()).containsExactly(PENSION);
    assertThat(tables.accountTotalMap().get(PENSION).evaluationAmount())
        .isEqualByComparingTo("12000");
    assertThat(tables.accountHoldingMap().get(BROKERAGE)).hasSize(1);
  }

  @Test
  void 계좌는_이름_차례() {
    List<TradeProfit> holdings =
        List.of(
            holding(PENSION, "연금저축", S1, "A", 10, "1000", "12000", "2000"),
            holding(BROKERAGE, "위탁", S2, "B", 5, "2000", "9000", "-1000"));

    var tables = controller().accountStatusTables(holdings, Map.of(), new BigDecimal("21000"));

    assertThat(tables.accountTotalMap().keySet()).containsExactly(PENSION, BROKERAGE);
    assertThat(tables.manualPrincipalAccountIds()).isEmpty();
  }
}
