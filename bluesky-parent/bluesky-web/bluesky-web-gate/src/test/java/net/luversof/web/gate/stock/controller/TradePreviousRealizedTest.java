package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.util.StockFormatUtil;

/** 매매 내역 실현 손익 카드의 전기 줄(2026-10-28 12차 자율) - 배당 내역 "전기 대비" 와 같은 구간, 목록과 같은 계좌 · 종목 거르기. */
class TradePreviousRealizedTest {

  private static final UUID ACCOUNT_A = UUID.randomUUID();
  private static final UUID ACCOUNT_B = UUID.randomUUID();
  private static final UUID STOCK = UUID.randomUUID();

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static TradeResponse trade(UUID account, TradeType type, String realized) {
    return new TradeResponse(
        UUID.randomUUID(),
        account,
        STOCK,
        "s",
        type,
        1,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        realized == null ? null : new BigDecimal(realized),
        Instant.parse("2026-01-10T00:00:00Z"));
  }

  @Test
  void 매도_줄만_목록과_같은_거르기로_더한다() {
    List<TradeResponse> trades =
        List.of(
            trade(ACCOUNT_A, TradeType.SELL, "1000"),
            trade(ACCOUNT_A, TradeType.BUY, "999"),
            trade(ACCOUNT_B, TradeType.SELL, "-300"),
            trade(ACCOUNT_A, TradeType.SELL, null));

    assertThat(StockTradeHtmxController.sellRealizedTotal(trades, null, null))
        .isEqualByComparingTo("700");
    assertThat(StockTradeHtmxController.sellRealizedTotal(trades, List.of(ACCOUNT_A), null))
        .isEqualByComparingTo("1000");
    assertThat(StockTradeHtmxController.sellRealizedTotal(trades, null, List.of(UUID.randomUUID())))
        .isEqualByComparingTo("0");
    assertThat(StockTradeHtmxController.sellRealizedTotal(null, null, null))
        .isEqualByComparingTo("0");
  }

  private static String render(BigDecimal previous) {
    Map<String, Object> params = new HashMap<>();
    params.put("allTradeList", List.of());
    params.put("totalItems", 0);
    params.put("totalAllBuyAmount", BigDecimal.ZERO);
    params.put("totalAllSellAmount", BigDecimal.ZERO);
    params.put("totalAllFee", BigDecimal.ZERO);
    params.put("totalAllTax", BigDecimal.ZERO);
    params.put("totalAllRealizedProfit", new BigDecimal("5000"));
    params.put("previousRealizedProfit", previous);
    params.put("previousStartDate", previous == null ? null : LocalDate.parse("2025-01-01"));
    params.put("previousEndDate", previous == null ? null : LocalDate.parse("2025-12-31"));
    params.put("decimalFormat", StockFormatUtil.wonFormat());
    params.put("countMessage", (java.util.function.Function<Object, String>) String::valueOf);
    params.put("buyLabel", "buy");
    params.put("sellLabel", "sell");
    params.put("realizedProfitLabel", "realized");
    params.put("feeLabel", "fee");
    params.put("tradingTaxLabel", "tax");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/trade/tradeSummaryCards.jte", params, output);
    return output.toString();
  }

  @Test
  void 전기_손실은_손실_색과_부호로() {
    String html = render(new BigDecimal("-1234567"));

    assertThat(html)
        .contains("data-trade-previous-realized")
        .contains("2025-01-01")
        .contains("2025-12-31")
        .contains("text-loss\">-&#8361;1,234,567</span>");
    assertThat(render(null)).doesNotContain("data-trade-previous-realized");
  }
}
