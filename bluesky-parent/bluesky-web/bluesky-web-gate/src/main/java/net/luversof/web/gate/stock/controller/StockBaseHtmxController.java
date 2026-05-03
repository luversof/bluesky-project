package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

/** 주식 HTMX 컨트롤러들의 공통 베이스 클래스. 공통 상수, 의존성, 헬퍼 메서드를 제공한다. */
public abstract class StockBaseHtmxController {

  protected static final String ERROR_ATTRIBUTE = "error";
  protected static final String ERROR_VIEW = "stock/htmx/error";
  protected static final int DIVIDEND_CHART_START_YEAR = 2015;

  protected final TradeProfitClient tradeProfitClient;
  protected final TradeClient tradeClient;
  protected final AccountClient accountClient;
  protected final StockItemClient stockItemClient;
  protected final DividendClient dividendClient;
  protected final MessageSource messageSource;

  protected StockBaseHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      MessageSource messageSource) {
    this.tradeProfitClient = tradeProfitClient;
    this.tradeClient = tradeClient;
    this.accountClient = accountClient;
    this.stockItemClient = stockItemClient;
    this.dividendClient = dividendClient;
    this.messageSource = messageSource;
  }

  /** 현재 로케일에 맞는 메시지를 반환한다. */
  protected String msg(String code, Object... args) {
    return messageSource.getMessage(code, args.length > 0 ? args : null, code, LocaleContextHolder.getLocale());
  }

  protected BigDecimal calculateDividendTax(DividendResponse d, boolean isDeferred) {
    if (isDeferred) {
      return BigDecimal.ZERO;
    }
    return d.tax() != null ? d.tax() : BigDecimal.ZERO;
  }

  protected BigDecimal calculateDividendTaxable(DividendResponse d, boolean isDeferred) {
    if (isDeferred) {
      return BigDecimal.ZERO;
    }
    if (d.taxableAmount() != null) {
      return d.taxableAmount();
    }
    if (d.taxPerShare() != null && d.quantity() != null) {
      return d.taxPerShare().multiply(BigDecimal.valueOf(d.quantity()));
    }
    return BigDecimal.ZERO;
  }

  // Helper to get enriched data
  protected List<TradeProfit> getEnrichedTradeProfits(TradeProfitRequest request) {
    List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request.toParams());
    String unknownLabel = msg("stock.label.unknown");

    Map<UUID, String> accountNames = tradeProfitList.stream()
        .map(TradeProfit::accountId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(
            Collectors.toMap(
                id -> id,
                id -> accountClient.getAccountById(id).map(Account::name).orElse(unknownLabel),
                (a, b) -> a));

    Map<UUID, String> stockItemNames = stockItemClient.getStockItems().stream()
        .collect(Collectors.toMap(StockItem::id, StockItem::name, (a, b) -> a));

    return tradeProfitList.stream()
        .map(
            profit -> TradeProfit.withNames(
                profit,
                stockItemNames.getOrDefault(profit.stockItemId(), unknownLabel),
                profit.accountId() != null ? accountNames.get(profit.accountId()) : null))
        .toList();
  }

  public record AnalyticsRow(
      String key,
      String subKey,
      BigDecimal value1,
      BigDecimal value2,
      BigDecimal value3,
      BigDecimal value4,
      BigDecimal value5,
      BigDecimal value6,
      BigDecimal value7) {
  }

  public record ChartDataset(
      String label,
      List<BigDecimal> data,
      String backgroundColor,
      String borderColor,
      Integer borderWidth,
      List<Integer> borderDash) {
  }
}
