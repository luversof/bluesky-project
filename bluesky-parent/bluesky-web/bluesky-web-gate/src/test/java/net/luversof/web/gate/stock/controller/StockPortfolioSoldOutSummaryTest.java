package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;

/** 자산 현황 "이 표에 없는 종목" 몫(2026-10-22 assetStatus 에서 soldOutSummary 로 옮기며 처음 둔 시험). */
class StockPortfolioSoldOutSummaryTest {

  private static final UUID HELD = UUID.randomUUID();
  private static final UUID SOLD = UUID.randomUUID();
  private static final UUID DIVIDEND_ONLY = UUID.randomUUID();

  private static TradeProfit row(UUID stock, int quantity, String realized) {
    return new TradeProfit(
        stock,
        "종목",
        null,
        null,
        null,
        null,
        0,
        null,
        null,
        new BigDecimal(realized),
        quantity,
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
        null,
        null,
        null,
        null);
  }

  @Test
  void 다_판_종목과_배당만_있는_종목을_센다() {
    var summary =
        StockPortfolioHtmxController.soldOutSummary(
            List.of(row(SOLD, 0, "500")),
            List.of(row(HELD, 10, "70")),
            Map.of(
                HELD,
                new BigDecimal("300"),
                SOLD,
                new BigDecimal("40"),
                DIVIDEND_ONLY,
                new BigDecimal("2100")));

    assertThat(summary.stockCount()).as("다 판 1 + 배당만 1").isEqualTo(2);
    assertThat(summary.realizedProfit()).as("다 판 종목의 실현손익만").isEqualByComparingTo("500");
    assertThat(summary.dividend())
        .as("보유 종목 배당 300 은 빼고, 배당만 있는 종목 2,100 은 넣는다(실측 2026-09-11 하나금융지주)")
        .isEqualByComparingTo("2140");
  }

  @Test
  void 없으면_0() {
    var summary = StockPortfolioHtmxController.soldOutSummary(List.of(), List.of(), Map.of());

    assertThat(summary.stockCount()).isZero();
    assertThat(summary.realizedProfit()).isEqualByComparingTo("0");
    assertThat(summary.dividend()).isEqualByComparingTo("0");
  }
}
