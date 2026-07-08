package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup;
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
  protected static final List<String> ACCOUNT_PRINCIPAL_CONFIG_KEYS =
      List.of("manualPrincipalAmount", "manualPrincipal", "principalAmount", "principal");

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
    return messageSource.getMessage(
        code, args.length > 0 ? args : null, code, LocaleContextHolder.getLocale());
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

  protected BigDecimal resolveAccountManualPrincipal(Account account) {
    if (account == null || account.jsonConfig() == null || account.jsonConfig().isEmpty()) {
      return null;
    }

    for (String key : ACCOUNT_PRINCIPAL_CONFIG_KEYS) {
      BigDecimal parsed = parseJsonBigDecimal(account.jsonConfig().get(key));
      if (parsed != null && parsed.compareTo(BigDecimal.ZERO) >= 0) {
        return parsed;
      }
    }

    return null;
  }

  private BigDecimal parseJsonBigDecimal(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof BigDecimal decimalValue) {
      return decimalValue;
    }

    if (value instanceof Number numberValue) {
      try {
        return new BigDecimal(numberValue.toString());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    if (value instanceof String stringValue) {
      String normalized = stringValue.replace(",", "").trim();
      if (normalized.isEmpty()) {
        return null;
      }

      try {
        return new BigDecimal(normalized);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    return null;
  }

  // Helper to get enriched data
  protected List<TradeProfit> getEnrichedTradeProfits(TradeProfitRequest request) {
    List<TradeProfit> tradeProfitList =
        emptyIfNull(tradeProfitClient.calculateProfit(request.toParams()));
    String unknownLabel = msg("stock.label.unknown");

    Map<UUID, String> accountNames =
        tradeProfitList.stream()
            .map(TradeProfit::accountId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(
                Collectors.toMap(
                    id -> id,
                    id -> accountClient.getAccountById(id).map(Account::name).orElse(unknownLabel),
                    (a, b) -> a));

    Map<UUID, String> stockItemNames =
        emptyIfNull(stockItemClient.getStockItems()).stream()
            .collect(Collectors.toMap(StockItem::id, StockItem::name, (a, b) -> a));

    return tradeProfitList.stream()
        .map(
            profit ->
                TradeProfit.withNames(
                    profit,
                    stockItemNames.getOrDefault(profit.stockItemId(), unknownLabel),
                    profit.accountId() != null ? accountNames.get(profit.accountId()) : null))
        .toList();
  }

  protected List<TradeProfit> getEnrichedTradeProfits(
      TradeProfitRequest request, TradeProfitRequestGroup groupBy) {
    if (groupBy == null || groupBy == request.getGroupBy()) {
      return getEnrichedTradeProfits(request);
    }

    TradeProfitRequest requestCopy = copyTradeProfitRequest(request);
    requestCopy.setGroupBy(groupBy);
    return getEnrichedTradeProfits(requestCopy);
  }

  protected TradeProfitRequest copyTradeProfitRequest(TradeProfitRequest request) {
    TradeProfitRequest requestCopy = new TradeProfitRequest();
    requestCopy.setUserId(request.getUserId());
    requestCopy.setAccountIdList(request.getAccountIdList());
    requestCopy.setStockItemIdList(request.getStockItemIdList());
    requestCopy.setStartDate(request.getStartDate());
    requestCopy.setEndDate(request.getEndDate());
    requestCopy.setTimeZone(request.getTimeZone());
    requestCopy.setGroupBy(request.getGroupBy());
    return requestCopy;
  }

  /** 종목 단위(계좌 무시)로 그룹핑된 손익 목록을 반환한다. */
  protected List<TradeProfit> getStockGroupedTradeProfits(
      TradeProfitRequest request, boolean includeZeroHoldings) {
    List<TradeProfit> stockGroupedTradeProfits =
        new ArrayList<>(getEnrichedTradeProfits(request, TradeProfitRequestGroup.STOCKITEM));
    if (!includeZeroHoldings) {
      stockGroupedTradeProfits.removeIf(tp -> tp.holdingQuantity() == 0);
    }
    return stockGroupedTradeProfits;
  }

  /** 종목별 실현손익 행으로 변환한다 (수수료/세금 반영 + 매수/매도 금액 포함). */
  protected TradeProfit toStockRealized(TradeProfit profit) {
    return TradeProfit.ofStockRealized(
        profit.stockItemId(),
        profit.stockItemName(),
        profit.holdingQuantity(),
        profit.totalSellQuantity(),
        profit.totalBuyAmount(),
        profit.totalSellAmount(),
        profit.evaluationAmount(),
        profit.evaluationProfit(),
        profit.realizedProfitNet(),
        profit.totalBuyCost(),
        profit.totalSellProceeds(),
        profit.totalBuyFee(),
        profit.totalSellFee(),
        profit.totalSellTax());
  }

  protected List<String> getAvailableStockTags(List<StockItem> stockItemList) {
    if (stockItemList == null || stockItemList.isEmpty()) {
      return List.of();
    }

    return stockItemList.stream()
        .filter(Objects::nonNull)
        .flatMap(stockItem -> stockItem.tags() != null ? stockItem.tags().stream() : Stream.empty())
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .sorted(String::compareToIgnoreCase)
        .toList();
  }

  protected static <T> List<T> emptyIfNull(List<T> values) {
    return values != null ? values : List.of();
  }

  protected StockTagSelection resolveStockTagSelection(
      List<StockItem> stockItemList, List<UUID> stockItemIdList, List<String> stockTagList) {
    List<String> selectedStockTags = normalizeStockTags(stockTagList);
    boolean hasFilter =
        (stockItemIdList != null && !stockItemIdList.isEmpty()) || !selectedStockTags.isEmpty();

    if (!hasFilter) {
      return new StockTagSelection(selectedStockTags, null, false);
    }

    var requestedStockItemIds = new LinkedHashSet<UUID>();
    if (stockItemIdList != null) {
      stockItemIdList.stream().filter(Objects::nonNull).forEach(requestedStockItemIds::add);
    }

    if (!selectedStockTags.isEmpty() && stockItemList != null) {
      stockItemList.stream()
          .filter(Objects::nonNull)
          .filter(stockItem -> stockItem.id() != null)
          .filter(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty())
          .filter(
              stockItem ->
                  stockItem.tags().stream()
                      .filter(StringUtils::hasText)
                      .map(String::trim)
                      .anyMatch(selectedStockTags::contains))
          .map(StockItem::id)
          .forEach(requestedStockItemIds::add);
    }

    return new StockTagSelection(selectedStockTags, new ArrayList<>(requestedStockItemIds), true);
  }

  private List<String> normalizeStockTags(List<String> stockTagList) {
    if (stockTagList == null || stockTagList.isEmpty()) {
      return List.of();
    }

    var normalizedStockTags = new LinkedHashSet<String>();
    stockTagList.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .forEach(normalizedStockTags::add);
    return new ArrayList<>(normalizedStockTags);
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
      BigDecimal value7) {}

  public record ChartDataset(
      String label,
      List<BigDecimal> data,
      String backgroundColor,
      String borderColor,
      Integer borderWidth,
      List<Integer> borderDash) {}

  protected record StockTagSelection(
      List<String> selectedStockTags, List<UUID> requestedStockItemIds, boolean hasFilter) {}
}
