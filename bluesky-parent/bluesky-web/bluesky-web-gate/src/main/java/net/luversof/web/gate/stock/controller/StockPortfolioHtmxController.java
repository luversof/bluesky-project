package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.domain.TradeProfitAggregator;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockPortfolioHtmxController extends StockBaseHtmxController {

  public StockPortfolioHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      MessageSource messageSource) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
  }

  @BlueskyPreAuthorize
  @GetMapping("/portfolio")
  public String portfolio(
      TradeProfitRequest request,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "ACCOUNT") String viewGroupBy,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      model.addAttribute(ERROR_ATTRIBUTE, msg("stock.label.login.required"));
      return ERROR_VIEW;
    }
    request.setUserId(userId);
    // Compute earliest trade date across all trades so the date-range nav
    // can correctly enable/disable the Previous button.
    List<TradeResponse> allTrades =
        tradeClient.findTrades(new TradeSearchRequest(userId, null, null, null, null).toParams());
    ZoneId dataZone =
        (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
            ? ZoneId.of(request.getTimeZone())
            : ZoneId.systemDefault();
    LocalDate dataFirstDate =
        allTrades.stream()
            .filter(t -> t.tradeDate() != null)
            .map(t -> t.tradeDate().atZone(dataZone).toLocalDate())
            .min(Comparator.naturalOrder())
            .orElse(null);

    List<TradeProfit> enrichedList = new ArrayList<>(getEnrichedTradeProfits(request));

    enrichedList.removeIf(tp -> tp.holdingQuantity() == 0);

    if ("STOCK".equals(viewGroupBy)) {
      Map<UUID, List<TradeProfit>> byStock =
          enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::stockItemId));

      List<TradeProfit> aggregatedList = new ArrayList<>();
      byStock.forEach(
          (stockId, list) -> {
            if (list.isEmpty()) return;

            TradeProfit first = list.get(0);
            String stockName = first.stockItemName();
            BigDecimal currentPrice = first.currentPrice();

            var s = TradeProfitAggregator.aggregate(list);
            aggregatedList.add(
                TradeProfit.ofPortfolioStock(
                    stockId,
                    stockName,
                    s.totalBuyAmount(),
                    s.avgBuyPrice(),
                    s.totalSellQuantity(),
                    s.avgSellPrice(),
                    s.totalSellAmount(),
                    s.realizedProfit(),
                    s.holdingQuantity(),
                    currentPrice,
                    s.evaluationAmount(),
                    s.evaluationProfit(),
                    s.totalProfit(),
                    s.totalBuyFee(),
                    s.totalSellFee(),
                    s.totalSellTax(),
                    s.totalBuyCost(),
                    s.totalSellProceeds(),
                    s.avgBuyPriceNet(),
                    s.avgSellPriceNet(),
                    s.realizedProfitNet(),
                    s.evaluationProfitNet(),
                    s.totalProfitNet()));
          });
      enrichedList = aggregatedList;
    }

    Comparator<TradeProfit> comparator = null;

    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";

      comparator =
          switch (field) {
            case "accountName" ->
                Comparator.comparing(
                    TradeProfit::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    TradeProfit::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "holdingQuantity" ->
                Comparator.comparing(
                    TradeProfit::holdingQuantity, Comparator.nullsLast(Comparator.naturalOrder()));
            case "averageBuyPrice" ->
                Comparator.comparing(
                    TradeProfit::averageBuyPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "currentPrice" ->
                Comparator.comparing(
                    TradeProfit::currentPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case "evaluationAmount" ->
                Comparator.comparing(
                    TradeProfit::evaluationAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "evaluationProfit" ->
                Comparator.comparing(
                    TradeProfit::evaluationProfit, Comparator.nullsLast(Comparator.naturalOrder()));
            case "realizedProfit" ->
                Comparator.comparing(
                    TradeProfit::realizedProfit, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };

      if (comparator != null && "desc".equalsIgnoreCase(direction)) {
        comparator = comparator.reversed();
      }
    }

    Map<String, TradeProfit> accountTotalMap = new HashMap<>();
    if ("ACCOUNT".equals(viewGroupBy)) {
      Comparator<TradeProfit> accountComparator =
          Comparator.comparing(
              TradeProfit::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
      comparator =
          (comparator == null) ? accountComparator : accountComparator.thenComparing(comparator);

      Map<String, List<TradeProfit>> byAccount =
          enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::accountName));

      byAccount.forEach(
          (accountName, list) -> {
            if (list.isEmpty()) return;

            var s = TradeProfitAggregator.aggregate(list);
            accountTotalMap.put(
                accountName,
                TradeProfit.ofPortfolioAccount(
                    accountName,
                    s.totalBuyAmount(),
                    s.totalSellQuantity(),
                    s.totalSellAmount(),
                    s.realizedProfit(),
                    s.holdingQuantity(),
                    s.evaluationAmount(),
                    s.evaluationProfit(),
                    s.totalProfit(),
                    s.totalBuyFee(),
                    s.totalSellFee(),
                    s.totalSellTax(),
                    s.totalBuyCost(),
                    s.totalSellProceeds(),
                    s.realizedProfitNet(),
                    s.evaluationProfitNet(),
                    s.totalProfitNet()));
          });
    }

    if (comparator != null) {
      enrichedList.sort(comparator);
    }

    BigDecimal totalEvaluationAmount =
        enrichedList.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalEvaluationProfit =
        enrichedList.stream()
            .map(TradeProfit::evaluationProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalRealizedProfit =
        enrichedList.stream()
            .map(TradeProfit::realizedProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("tradeProfitList", enrichedList);
    model.addAttribute("sort", sort);
    model.addAttribute("viewGroupBy", viewGroupBy);
    model.addAttribute("totalEvaluationAmount", totalEvaluationAmount);
    model.addAttribute("totalEvaluationProfit", totalEvaluationProfit);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);
    model.addAttribute("accountTotalMap", accountTotalMap);
    return "stock/htmx/fragments/tabsPortfolio";
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-status")
  public String assetStatus(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<TradeProfit> enrichedList = new ArrayList<>(getEnrichedTradeProfits(request));
    enrichedList.removeIf(tp -> tp.holdingQuantity() == 0);

    Map<UUID, BigDecimal> accountPrincipalOverrideMap =
        accountClient.getAccountsByUserId(userId).stream()
            .filter(account -> account.id() != null)
            .flatMap(
                account -> {
                  BigDecimal principal = resolveAccountManualPrincipal(account);
                  return principal != null
                      ? java.util.stream.Stream.of(Map.entry(account.id(), principal))
                      : java.util.stream.Stream.empty();
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

    // 계좌별 집계
    Map<String, TradeProfit> accountTotalMap = new LinkedHashMap<>();
    enrichedList.stream()
        .collect(Collectors.groupingBy(TradeProfit::accountId))
        .entrySet()
        .stream()
        .sorted(
            Comparator.comparing(
                entry -> {
                  List<TradeProfit> list = entry.getValue();
                  if (list == null || list.isEmpty()) {
                    return null;
                  }
                  return list.get(0).accountName();
                },
                Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(
            entry -> {
              UUID accountId = entry.getKey();
              List<TradeProfit> list = entry.getValue();
              TradeProfit first = list.get(0);
              String accountName = first.accountName();
              var s = TradeProfitAggregator.aggregate(list);
              BigDecimal evaluationAmount =
                  s.evaluationAmount() != null ? s.evaluationAmount() : BigDecimal.ZERO;
              BigDecimal defaultEvaluationProfit =
                  s.evaluationProfit() != null ? s.evaluationProfit() : BigDecimal.ZERO;
              BigDecimal defaultPrincipal =
                  s.evaluationAmount() != null && s.evaluationProfit() != null
                      ? s.evaluationAmount().subtract(s.evaluationProfit())
                      : (s.totalBuyCost() != null ? s.totalBuyCost() : BigDecimal.ZERO);
              BigDecimal manualPrincipal =
                  accountId != null ? accountPrincipalOverrideMap.get(accountId) : null;
              BigDecimal effectivePrincipal =
                  manualPrincipal != null ? manualPrincipal : defaultPrincipal;
              BigDecimal effectiveEvaluationProfit =
                  manualPrincipal != null
                      ? evaluationAmount.subtract(manualPrincipal)
                      : defaultEvaluationProfit;
              accountTotalMap.put(
                  accountName,
                  TradeProfit.ofAccountStatus(
                      accountName,
                      evaluationAmount,
                      effectiveEvaluationProfit,
                      s.realizedProfit(),
                      effectivePrincipal));
            });

    // 종목별 집계 (계좌 통합)
    Map<UUID, List<TradeProfit>> byStock =
        enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::stockItemId));
    List<TradeProfit> stockAggregated =
        byStock.entrySet().stream()
            .map(
                entry -> {
                  List<TradeProfit> list = entry.getValue();
                  TradeProfit first = list.get(0);
                  var s = TradeProfitAggregator.aggregate(list);
                  return TradeProfit.ofStockStatus(
                      entry.getKey(),
                      first.stockItemName(),
                      s.avgBuyPriceNet(),
                      s.holdingQuantity(),
                      first.currentPrice(),
                      s.evaluationAmount(),
                      s.evaluationProfit(),
                      s.realizedProfit(),
                      s.totalBuyCost());
                })
            .sorted(
                Comparator.comparing(
                        TradeProfit::evaluationAmount,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        TradeProfit::stockItemName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    BigDecimal totalEvaluationAmount =
        stockAggregated.stream()
            .map(TradeProfit::evaluationAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalEvaluationProfit =
        stockAggregated.stream()
            .map(TradeProfit::evaluationProfit)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("accountTotalMap", accountTotalMap);
    model.addAttribute("stockAggregated", stockAggregated);
    model.addAttribute("totalEvaluationAmount", totalEvaluationAmount);
    model.addAttribute("totalEvaluationProfit", totalEvaluationProfit);
    return "stock/htmx/fragments/assetStatus";
  }

  @BlueskyPreAuthorize
  @GetMapping("/realized-profit")
  public String realizedProfit(
      TradeProfitRequest request,
      @RequestParam(required = false) List<String> stockTagList,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<net.luversof.web.gate.stock.domain.StockItem> stockItemList =
        stockItemClient.getStockItems();
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, request.getStockItemIdList(), stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    request.setStockItemIdList(stockTagSelection.requestedStockItemIds());

    // Remember whether the client actually provided a date range (before we
    // possibly default to YTD). This lets the UI treat server-default ranges
    // differently from user-selected ranges.
    boolean clientProvidedRange =
        !((request.getStartDate() == null || request.getStartDate().toEpochMilli() == 0)
            && (request.getEndDate() == null || request.getEndDate().toEpochMilli() == 0)
            && (rangeMode == null || rangeMode.isBlank()));

    // If client did not provide date range, default to this year (ytd)
    if (!clientProvidedRange
        && (request.getStartDate() == null || request.getStartDate().toEpochMilli() == 0)
        && (request.getEndDate() == null || request.getEndDate().toEpochMilli() == 0)
        && (rangeMode == null || rangeMode.isBlank())) {
      ZoneId zone =
          (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
              ? ZoneId.of(request.getTimeZone())
              : ZoneId.systemDefault();
      LocalDate now = LocalDate.now(zone);
      request.setStartDate(LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant());
      request.setEndDate(now.plusDays(1).atStartOfDay(zone).toInstant());
      rangeMode = "ytd";
    }

    // 보유량 0인 것도 포함 (전량 매도한 종목의 실현손익도 표시)
    List<TradeProfit> enrichedList =
        stockTagSelection.hasFilter()
                && request.getStockItemIdList() != null
                && request.getStockItemIdList().isEmpty()
            ? new ArrayList<>()
            : new ArrayList<>(getEnrichedTradeProfits(request));

    // 계좌별 집계
    Map<String, TradeProfit> accountRealizedMap = new LinkedHashMap<>();
    enrichedList.stream()
        .collect(Collectors.groupingBy(TradeProfit::accountName))
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
        .forEach(
            entry -> {
              String accountName = entry.getKey();
              List<TradeProfit> list = entry.getValue();
              var s = TradeProfitAggregator.aggregate(list);
              accountRealizedMap.put(
                  accountName,
                  TradeProfit.ofAccountStatus(
                      accountName,
                      s.evaluationAmount(),
                      s.evaluationProfit(),
                      s.realizedProfitNet(),
                      s.totalBuyCost()));
            });

    // 종목별 집계 (계좌 통합, 보유량 0 포함) - ofStockRealized 사용으로 상세 필드 포함
    Map<UUID, List<TradeProfit>> byStock =
        enrichedList.stream().collect(Collectors.groupingBy(TradeProfit::stockItemId));
    List<TradeProfit> stockRealizedList =
        byStock.entrySet().stream()
            .map(
                entry -> {
                  List<TradeProfit> list = entry.getValue();
                  TradeProfit first = list.get(0);
                  var s = TradeProfitAggregator.aggregate(list);
                  return TradeProfit.ofStockRealized(
                      entry.getKey(),
                      first.stockItemName(),
                      s.holdingQuantity(),
                      s.totalSellQuantity(),
                      s.evaluationAmount(),
                      s.evaluationProfit(),
                      s.realizedProfitNet(),
                      s.totalBuyCost(),
                      s.totalSellProceeds(),
                      s.totalBuyFee(),
                      s.totalSellFee(),
                      s.totalSellTax());
                })
            .filter(
                tp ->
                    tp.realizedProfitNet() != null
                        && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) != 0)
            .sorted(
                Comparator.comparing(
                        TradeProfit::realizedProfitNet,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        TradeProfit::stockItemName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    // 전체 요약 통계
    BigDecimal totalRealizedProfit =
        enrichedList.stream()
            .map(TradeProfit::realizedProfitNet)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalBuyCost =
        enrichedList.stream()
            .map(TradeProfit::totalBuyCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSellProceeds =
        enrichedList.stream()
            .map(TradeProfit::totalSellProceeds)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalFees =
        enrichedList.stream()
            .map(
                tp -> {
                  BigDecimal fee = BigDecimal.ZERO;
                  if (tp.totalBuyFee() != null) fee = fee.add(tp.totalBuyFee());
                  if (tp.totalSellFee() != null) fee = fee.add(tp.totalSellFee());
                  if (tp.totalSellTax() != null) fee = fee.add(tp.totalSellTax());
                  return fee;
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long winCount =
        stockRealizedList.stream()
            .filter(
                tp ->
                    tp.realizedProfitNet() != null
                        && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) > 0)
            .count();
    double winRate =
        stockRealizedList.isEmpty() ? 0.0 : (double) winCount / stockRealizedList.size() * 100;

    // 최고/최대 손실 종목 (stockRealizedList는 이미 realizedProfitNet 내림차순 정렬됨)
    TradeProfit bestStock = stockRealizedList.isEmpty() ? null : stockRealizedList.get(0);
    TradeProfit worstStock =
        stockRealizedList.isEmpty()
            ? null
            : stockRealizedList.stream()
                .filter(
                    tp ->
                        tp.realizedProfitNet() != null
                            && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) < 0)
                .min(
                    Comparator.comparing(
                        TradeProfit::realizedProfitNet,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

    // 차트 데이터: 수익 Top5 / 손실 Worst5 분리
    List<TradeProfit> profitStocks =
        stockRealizedList.stream()
            .filter(
                tp ->
                    tp.realizedProfitNet() != null
                        && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) > 0)
            .limit(5)
            .toList(); // 이미 내림차순 정렬
    List<TradeProfit> lossStocks =
        stockRealizedList.stream()
            .filter(
                tp ->
                    tp.realizedProfitNet() != null
                        && tp.realizedProfitNet().compareTo(BigDecimal.ZERO) < 0)
            .sorted(
                Comparator.comparing(
                    TradeProfit::realizedProfitNet,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(5)
            .toList(); // 손실 큰 순
    List<String> chartProfitLabels =
        profitStocks.stream()
            .map(tp -> tp.stockItemName() != null ? tp.stockItemName() : "-")
            .toList();
    List<Long> chartProfitValues =
        profitStocks.stream().map(tp -> tp.realizedProfitNet().longValue()).toList();
    List<String> chartLossLabels =
        lossStocks.stream()
            .map(tp -> tp.stockItemName() != null ? tp.stockItemName() : "-")
            .toList();
    List<Long> chartLossValues =
        lossStocks.stream().map(tp -> tp.realizedProfitNet().longValue()).toList();

    // Compute earliest trade date across all trades so the date-range nav can
    // correctly
    // enable/disable Previous
    List<TradeResponse> allTrades =
        tradeClient.findTrades(new TradeSearchRequest(userId, null, null, null, null).toParams());
    ZoneId dataZone =
        (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
            ? ZoneId.of(request.getTimeZone())
            : ZoneId.systemDefault();
    LocalDate dataFirstDate =
        allTrades.stream()
            .filter(t -> t.tradeDate() != null)
            .map(t -> t.tradeDate().atZone(dataZone).toLocalDate())
            .min(Comparator.naturalOrder())
            .orElse(null);
    // Prepare account/stock lists for filter controls (limit to items present
    // in the current result set to avoid showing filters that yield no data)
    List<net.luversof.web.gate.stock.domain.Account> accountList =
        accountClient.getAccountsByUserId(userId);

    // Derive accounts/stocks that exist within the selected date range. IMPORTANT:
    // - Use a request that DOES NOT include the user's current account/stock
    // filters so we compute availability by date-range only.
    List<net.luversof.web.gate.stock.domain.Account> filteredAccountList;
    List<net.luversof.web.gate.stock.domain.StockItem> filteredStockItemList;
    java.util.Set<UUID> tradeAccountIds;
    java.util.Set<UUID> tradeStockIds;
    if (clientProvidedRange) {
      TradeProfitRequest dateOnlyReq = new TradeProfitRequest();
      dateOnlyReq.setUserId(userId);
      dateOnlyReq.setStartDate(request.getStartDate());
      dateOnlyReq.setEndDate(request.getEndDate());
      dateOnlyReq.setTimeZone(request.getTimeZone());
      List<TradeProfit> dateRangeEnriched = new ArrayList<>(getEnrichedTradeProfits(dateOnlyReq));
      tradeAccountIds =
          dateRangeEnriched.stream()
              .map(TradeProfit::accountId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      tradeStockIds =
          dateRangeEnriched.stream()
              .map(TradeProfit::stockItemId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      filteredAccountList =
          accountList.stream().filter(a -> tradeAccountIds.contains(a.id())).toList();
      filteredStockItemList =
          stockItemList.stream().filter(s -> tradeStockIds.contains(s.id())).toList();
    } else {
      // No user-specified range -> show full lists
      tradeAccountIds = java.util.Collections.emptySet();
      tradeStockIds = java.util.Collections.emptySet();
      filteredAccountList = accountList;
      filteredStockItemList = stockItemList;
    }

    Set<UUID> availableAccountIds =
        accountList.stream()
            .map(net.luversof.web.gate.stock.domain.Account::id)
            .collect(Collectors.toSet());
    List<UUID> requestedAccountIds = request.getAccountIdList();
    List<UUID> effectiveAccountIdList =
        (requestedAccountIds != null
                && !requestedAccountIds.isEmpty()
                && availableAccountIds.containsAll(requestedAccountIds))
            ? requestedAccountIds
            : null;

    Set<UUID> availableStockIds =
        stockItemList.stream()
            .map(net.luversof.web.gate.stock.domain.StockItem::id)
            .collect(Collectors.toSet());
    List<UUID> requestedStockItemIds = request.getStockItemIdList();
    List<UUID> effectiveStockItemIdList =
        requestedStockItemIds != null && availableStockIds.containsAll(requestedStockItemIds)
            ? requestedStockItemIds
            : stockTagSelection.hasFilter() ? List.of() : null;

    // Determine what to show in the dropdowns:
    // - If the client actually selected a date range, show only accounts/stocks
    // that have data in that range (filtered lists).
    // - However, if the user had previously selected an account/stock that is
    // NOT present in the filtered list, keep that selected item visible so
    // the selection doesn't disappear. If no client range, show full lists.
    List<net.luversof.web.gate.stock.domain.Account> finalAccountList;
    if (clientProvidedRange) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null) continue;
          if (!tradeAccountIds.contains(sel)) {
            accountList.stream()
                .filter(a -> a.id().equals(sel))
                .findFirst()
                .ifPresent(
                    a -> {
                      if (finalAccountList.stream().noneMatch(x -> x.id().equals(a.id())))
                        finalAccountList.add(0, a);
                    });
          }
        }
      }
    } else {
      finalAccountList = accountList;
    }

    List<net.luversof.web.gate.stock.domain.StockItem> finalStockItemList;
    if (clientProvidedRange) {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!tradeStockIds.contains(sel)) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(
                    s -> {
                      if (finalStockItemList.stream().noneMatch(x -> x.id().equals(s.id())))
                        finalStockItemList.add(0, s);
                    });
          }
        }
      }
    } else {
      finalStockItemList = stockItemList;
    }

    // Debug info: expose computed trade account IDs and final list sizes for
    // troubleshooting
    model.addAttribute(
        "debugTradeAccountIds",
        tradeAccountIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
    model.addAttribute(
        "debugFinalAccountListSize", finalAccountList != null ? finalAccountList.size() : 0);
    model.addAttribute(
        "debugFinalStockItemListSize", finalStockItemList != null ? finalStockItemList.size() : 0);

    model.addAttribute("accountRealizedMap", accountRealizedMap);
    model.addAttribute("stockRealizedList", stockRealizedList);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);
    model.addAttribute("totalSellProceeds", totalSellProceeds);
    model.addAttribute("totalFees", totalFees);
    model.addAttribute("winRate", winRate);
    model.addAttribute("bestStock", bestStock);
    model.addAttribute("worstStock", worstStock);
    model.addAttribute("chartProfitLabels", chartProfitLabels);
    model.addAttribute("chartProfitValues", chartProfitValues);
    model.addAttribute("chartLossLabels", chartLossLabels);
    model.addAttribute("chartLossValues", chartLossValues);
    // filter lists for UI selects (respect client-provided range and preserve
    // previously selected items that may not exist in the current range)
    model.addAttribute("accountList", finalAccountList);
    model.addAttribute("stockItemList", finalStockItemList);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute(
        "selectedAccountIds", effectiveAccountIdList != null ? effectiveAccountIdList : List.of());
    model.addAttribute(
        "selectedStockItemIds",
        effectiveStockItemIdList != null ? effectiveStockItemIdList : List.of());
    model.addAttribute("selectedStockTags", selectedStockTags);
    model.addAttribute(
        "selectedAccountId",
        (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
            ? effectiveAccountIdList.get(0)
            : null);
    model.addAttribute(
        "selectedStockItemId",
        (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
            ? effectiveStockItemIdList.get(0)
            : null);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    // Pass date range and UI state back to the fragment so buttons/inputs reflect
    // the selection
    model.addAttribute("startDate", request.getStartDate());
    model.addAttribute("endDate", request.getEndDate());
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("timeZone", request.getTimeZone());
    return "stock/htmx/fragments/realizedProfit";
  }

  @BlueskyPreAuthorize
  @GetMapping("/charts/allocation")
  public String allocationChart(TradeProfitRequest request, Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;
    request.setUserId(userId);

    List<TradeProfit> profitList = getEnrichedTradeProfits(request);

    Map<String, BigDecimal> allocation =
        profitList.stream()
            .filter(p -> p.evaluationAmount() != null)
            .collect(
                Collectors.toMap(
                    p -> p.stockItemName() != null ? p.stockItemName() : msg("stock.label.unknown"),
                    TradeProfit::evaluationAmount,
                    BigDecimal::add));

    model.addAttribute("allocation", allocation);
    return "stock/htmx/fragments/chartsAllocation";
  }

  @BlueskyPreAuthorize
  @GetMapping("/charts/dividend")
  public String dividendChart(Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) return ERROR_VIEW;

    DividendRequest request = new DividendRequest();
    request.setUserId(userId);
    List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

    Map<String, BigDecimal> monthly =
        dividends.stream()
            .filter(d -> d.payDate() != null)
            .collect(
                Collectors.groupingBy(
                    d ->
                        d.payDate()
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        d -> d.grossAmount() != null ? d.grossAmount() : BigDecimal.ZERO,
                        BigDecimal::add)));

    Map<String, BigDecimal> sortedMonthly = new java.util.TreeMap<>(monthly);
    model.addAttribute("monthlyDividends", sortedMonthly);
    return "stock/htmx/fragments/chartsDividend";
  }
}
