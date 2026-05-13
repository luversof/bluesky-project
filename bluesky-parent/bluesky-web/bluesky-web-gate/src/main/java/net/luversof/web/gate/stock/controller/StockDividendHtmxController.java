package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.common.menu.domain.Pagination;
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.DividendView;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockDividendHtmxController extends StockBaseHtmxController {

  public StockDividendHtmxController(
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
  @GetMapping("/dividend/list")
  public String dividendList(
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList,
      @RequestParam(required = false) List<String> stockTagList,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String timeZone,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    UUID userId = UserUtil.getUserId();
    if (userId == null) {
      model.addAttribute(ERROR_ATTRIBUTE, msg("stock.label.login.required"));
      return ERROR_VIEW;
    }

    Instant startInstant = startDate;
    Instant endInstant = endDate;
    // Default to this year when client didn't provide range
    if (startInstant == null && endInstant == null && (rangeMode == null || rangeMode.isBlank())) {
      ZoneId zone =
          (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
      LocalDate now = LocalDate.now(zone);
      startInstant = LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant();
      endInstant = now.plusDays(1).atStartOfDay(zone).toInstant();
      rangeMode = "ytd";
    }

    var request = new DividendRequest();
    request.setUserId(userId);
    request.setStartDate(startInstant);
    request.setEndDate(endInstant);

    List<DividendResponse> dividends = dividendClient.findDividends(request.toParams());

    // Always fetch the global/all dividend set so we can offer "전체 기간" filtering
    var globalReq = new DividendRequest();
    globalReq.setUserId(userId);
    List<DividendResponse> globalDividends = dividendClient.findDividends(globalReq.toParams());
    ZoneId zone =
        (timeZone != null && !timeZone.isEmpty()) ? ZoneId.of(timeZone) : ZoneId.systemDefault();
    LocalDate dataFirstDate =
        globalDividends.stream()
            .map(
                d -> {
                  Instant payDate = d.payDate();
                  Instant recordDate = d.recordDate();
                  if (payDate == null && recordDate == null) return null;
                  if (payDate == null) return recordDate;
                  if (recordDate == null) return payDate;
                  return payDate.isBefore(recordDate) ? payDate : recordDate;
                })
            .filter(inst -> inst != null)
            .map(inst -> inst.atZone(zone).toLocalDate())
            .min(Comparator.naturalOrder())
            .orElse(null);

    var dividendAccountIds =
        dividends.stream().map(DividendResponse::accountId).collect(Collectors.toSet());
    var dividendStockIds =
        dividends.stream().map(DividendResponse::stockItemId).collect(Collectors.toSet());
    var globalDividendStockIds =
        globalDividends.stream().map(DividendResponse::stockItemId).collect(Collectors.toSet());

    List<Account> accounts = accountClient.getAccountsByUserId(userId);
    List<Account> filteredAccountList =
        accounts.stream().filter(a -> dividendAccountIds.contains(a.id())).toList();

    Map<UUID, String> accountNames =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id, Account::name, (left, right) -> left, LinkedHashMap::new));
    Map<UUID, Boolean> taxDeferredMap =
        accounts.stream()
            .collect(
                Collectors.toMap(
                    Account::id,
                    a ->
                        a.jsonConfig() != null
                            && Boolean.TRUE.equals(a.jsonConfig().get("isTaxDeferred")),
                    (l, r) -> l));

    List<StockItem> stockItemList = stockItemClient.getStockItems();
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, stockItemIdList, stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    List<StockItem> filteredStockItemList =
        stockItemList.stream().filter(s -> dividendStockIds.contains(s.id())).toList();
    // Stocks that have any dividends in the global timeframe (used for 전체/no-range)
    List<StockItem> filteredStockItemListAll =
        stockItemList.stream().filter(s -> globalDividendStockIds.contains(s.id())).toList();
    Map<UUID, String> stockItemNames =
        stockItemList.stream().collect(Collectors.toMap(StockItem::id, StockItem::name));

    // Validate requested filters against full lists (so we can preserve previous
    // selections) and build final lists that show date-available items but keep
    // any previously selected items visible.
    Set<UUID> availableAccountIds = accounts.stream().map(Account::id).collect(Collectors.toSet());
    List<UUID> requestedAccountIds = accountIdList;
    List<UUID> effectiveAccountIdList =
        (requestedAccountIds != null
                && !requestedAccountIds.isEmpty()
                && availableAccountIds.containsAll(requestedAccountIds))
            ? requestedAccountIds
            : null;

    Set<UUID> availableStockIds =
        stockItemList.stream().map(StockItem::id).collect(Collectors.toSet());
    List<UUID> requestedStockItemIds = stockTagSelection.requestedStockItemIds();
    List<UUID> effectiveStockItemIdList =
        requestedStockItemIds != null && availableStockIds.containsAll(requestedStockItemIds)
            ? requestedStockItemIds
            : stockTagSelection.hasFilter() ? List.of() : null;

    List<Account> finalAccountList;
    if (startInstant != null || endInstant != null) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null) continue;
          if (!finalAccountList.stream().anyMatch(a -> a.id().equals(sel))) {
            accounts.stream()
                .filter(a -> a.id().equals(sel))
                .findFirst()
                .ifPresent(a -> finalAccountList.add(0, a));
          }
        }
      }
    } else {
      finalAccountList = accounts;
    }

    List<StockItem> finalStockItemList;
    if (startInstant != null || endInstant != null) {
      // Date-specific search -> show only stocks that had dividends in the requested
      // period
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(s -> finalStockItemList.add(0, s));
          }
        }
      }
    } else {
      // No explicit date range (or rangeMode='all') -> show stocks that have any
      // dividend history
      finalStockItemList = new ArrayList<>(filteredStockItemListAll);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null) continue;
          if (!finalStockItemList.stream().anyMatch(s -> s.id().equals(sel))) {
            stockItemList.stream()
                .filter(s -> s.id().equals(sel))
                .findFirst()
                .ifPresent(s -> finalStockItemList.add(0, s));
          }
        }
      }
    }

    List<DividendView> viewList =
        dividends.stream()
            .filter(
                d ->
                    (effectiveAccountIdList == null
                        || effectiveAccountIdList.isEmpty()
                        || effectiveAccountIdList.contains(d.accountId())))
            .filter(
                d ->
                    (effectiveStockItemIdList == null
                        || effectiveStockItemIdList.contains(d.stockItemId())))
            .map(
                dividend -> {
                  String accountName =
                      accountNames.getOrDefault(dividend.accountId(), msg("stock.label.unknown"));
                  String stockItemName =
                      Optional.ofNullable(dividend.stockItemName())
                          .orElse(
                              Optional.ofNullable(dividend.stockItemId())
                                  .map(
                                      id ->
                                          stockItemNames.getOrDefault(
                                              id, msg("stock.label.unknown")))
                                  .orElse(msg("stock.label.unknown")));

                  boolean isDeferred = taxDeferredMap.getOrDefault(dividend.accountId(), false);

                  BigDecimal grossAmount =
                      Optional.ofNullable(dividend.grossAmount()).orElse(BigDecimal.ZERO);
                  BigDecimal tax =
                      isDeferred
                          ? BigDecimal.ZERO
                          : Optional.ofNullable(dividend.tax()).orElse(BigDecimal.ZERO);
                  BigDecimal netAmount =
                      isDeferred
                          ? grossAmount
                          : Optional.ofNullable(dividend.netAmount())
                              .orElse(grossAmount.subtract(tax));

                  BigDecimal taxableAmount = BigDecimal.ZERO;
                  if (!isDeferred) {
                    if (dividend.taxableAmount() != null) {
                      taxableAmount = dividend.taxableAmount();
                    } else if (dividend.taxPerShare() != null && dividend.quantity() != null) {
                      taxableAmount =
                          dividend.taxPerShare().multiply(BigDecimal.valueOf(dividend.quantity()));
                    }
                  }

                  return new DividendView(
                      dividend.id(),
                      dividend.accountId(),
                      accountName,
                      dividend.stockItemId(),
                      stockItemName,
                      dividend.quantity(),
                      dividend.amountPerShare(),
                      grossAmount,
                      tax,
                      taxableAmount,
                      netAmount,
                      dividend.recordDate(),
                      dividend.payDate(),
                      null,
                      null,
                      null,
                      null,
                      null,
                      null);
                })
            .collect(Collectors.toCollection(ArrayList::new));

    DividendAnalyticsResult analyticsResult =
        buildDividendAnalytics(
        userId,
        viewList,
        effectiveAccountIdList,
        effectiveStockItemIdList,
        startInstant,
        endInstant,
        zone);
    viewList = analyticsResult.dividendViews();

    if (sort != null && !sort.isEmpty()) {
      String[] parts = sort.split(",");
      String field = parts[0];
      String direction = parts.length > 1 ? parts[1] : "asc";

      Comparator<DividendView> comparator =
          switch (field) {
            case "payDate" ->
                Comparator.comparing(
                    DividendView::payDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "accountName" ->
                Comparator.comparing(
                    DividendView::accountName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "stockItemName" ->
                Comparator.comparing(
                    DividendView::stockItemName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "grossAmount" ->
                Comparator.comparing(
                    DividendView::grossAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "netAmount" ->
                Comparator.comparing(
                    DividendView::netAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            case "tax" ->
                Comparator.comparing(
                    DividendView::tax, Comparator.nullsLast(Comparator.naturalOrder()));
            case "taxableAmount" ->
                Comparator.comparing(
                    DividendView::taxableAmount, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
          };

      if (comparator != null) {
        if ("desc".equalsIgnoreCase(direction)) {
          comparator = comparator.reversed();
        }
        viewList.sort(comparator);
      }
    } else {
      viewList.sort(
          Comparator.comparing(
              DividendView::payDate, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    if (size <= 0) size = 15;

    boolean isSearch =
        (effectiveAccountIdList != null && !effectiveAccountIdList.isEmpty())
            || (effectiveStockItemIdList != null && !effectiveStockItemIdList.isEmpty())
            || startDate != null
            || endDate != null;

    if (isSearch) {
      size = Math.max(viewList.size(), 1);
    }

    int totalItems = viewList.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int currentPage = Math.max(1, Math.min(page, totalPages));
    if (totalPages == 0) currentPage = 1;

    int fromIndex = (currentPage - 1) * size;
    int toIndex = Math.min(fromIndex + size, totalItems);
    List<DividendView> pagedList =
        (fromIndex < totalItems) ? viewList.subList(fromIndex, toIndex) : Collections.emptyList();

    BigDecimal totalGrossAmount =
        pagedList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalNetAmount =
        pagedList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTax =
        pagedList.stream().map(DividendView::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTaxableAmount =
        pagedList.stream()
            .map(DividendView::taxableAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAllGrossAmount =
        viewList.stream().map(DividendView::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllNetAmount =
        viewList.stream().map(DividendView::netAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAllTaxableAmount =
        viewList.stream().map(DividendView::taxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal prevPeriodNetAmount = null;
    LocalDate prevStartDate = null;
    LocalDate prevEndDate = null;
    if (startDate != null && endDate != null) {
      // convert Instants to LocalDate in the request's timezone (reuse earlier
      // `zone`)
      LocalDate startLocal = startDate.atZone(zone).toLocalDate();
      LocalDate endLocal = endDate.atZone(zone).toLocalDate();

      long durationDays = ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
      prevStartDate = startLocal.minusDays(durationDays);
      prevEndDate = startLocal.minusDays(1);

      Instant prevStartInstant = prevStartDate.atStartOfDay(zone).toInstant();
      Instant prevEndInstant = prevEndDate.plusDays(1).atStartOfDay(zone).toInstant();

      var prevRequest = new DividendRequest();
      prevRequest.setUserId(userId);
      prevRequest.setStartDate(prevStartInstant);
      prevRequest.setEndDate(prevEndInstant);

      final List<UUID> finalAccountIdList = effectiveAccountIdList;
      final List<UUID> finalStockItemIdList = effectiveStockItemIdList;
      List<DividendResponse> prevDividends = dividendClient.findDividends(prevRequest.toParams());
      prevPeriodNetAmount =
          prevDividends.stream()
              .filter(
                  d ->
                      (finalAccountIdList == null
                          || finalAccountIdList.isEmpty()
                          || finalAccountIdList.contains(d.accountId())))
              .filter(
                  d ->
                      (finalStockItemIdList == null
                          || finalStockItemIdList.isEmpty()
                          || finalStockItemIdList.contains(d.stockItemId())))
              .map(
                  d -> {
                    boolean isDeferred = taxDeferredMap.getOrDefault(d.accountId(), false);
                    BigDecimal gross = Optional.ofNullable(d.grossAmount()).orElse(BigDecimal.ZERO);
                    if (isDeferred) return gross;
                    BigDecimal tax2 = Optional.ofNullable(d.tax()).orElse(BigDecimal.ZERO);
                    return Optional.ofNullable(d.netAmount()).orElse(gross.subtract(tax2));
                  })
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    var pageImpl = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, size), totalItems);
    var pagination = new Pagination(pageImpl);

    model.addAttribute("dividendList", pagedList);
    model.addAttribute("allDividendList", viewList);
    model.addAttribute("pagination", pagination);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("size", size);
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
    // reflect actual instants used (may have been defaulted to YTD above)
    model.addAttribute("startDate", startInstant);
    model.addAttribute("endDate", endInstant);
    model.addAttribute("timeZone", timeZone);
    model.addAttribute("sort", sort);
    model.addAttribute("totalGrossAmount", totalGrossAmount);
    model.addAttribute("totalNetAmount", totalNetAmount);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("totalTaxableAmount", totalTaxableAmount);
    model.addAttribute("totalAllGrossAmount", totalAllGrossAmount);
    model.addAttribute("totalAllNetAmount", totalAllNetAmount);
    model.addAttribute("totalAllTaxableAmount", totalAllTaxableAmount);
    model.addAttribute("prevPeriodNetAmount", prevPeriodNetAmount);
    model.addAttribute("prevStartDate", prevStartDate);
    model.addAttribute("prevEndDate", prevEndDate);
    model.addAttribute(
        "portfolioYieldOnCostPct",
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnCostPct()
            : null);
    model.addAttribute(
      "portfolioYieldOnDailyAverageCostPct",
      analyticsResult.portfolioYield() != null
        ? analyticsResult.portfolioYield().yieldOnDailyAverageCostPct()
        : null);
    model.addAttribute(
        "portfolioYieldOnMarketPct",
        analyticsResult.portfolioYield() != null
            ? analyticsResult.portfolioYield().yieldOnMarketPct()
            : null);
    model.addAttribute(
        "bestYieldStock",
        analyticsResult.stockYieldRows().isEmpty()
            ? null
            : analyticsResult.stockYieldRows().get(0));
    model.addAttribute(
        "bestYieldAccount",
        analyticsResult.accountYieldRows().isEmpty()
            ? null
            : analyticsResult.accountYieldRows().get(0));
    model.addAttribute("stockYieldRows", analyticsResult.stockYieldRows());
    model.addAttribute("accountYieldRows", analyticsResult.accountYieldRows());
    model.addAttribute("yearlyYieldRows", analyticsResult.yearlyYieldRows());
    model.addAttribute("rangeMode", rangeMode);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    // reflect rangeMode back into model (may have been defaulted above)
    model.addAttribute("rangeMode", rangeMode);

    return "stock/htmx/fragments/tabsDividendHistory";
  }

  private DividendAnalyticsResult buildDividendAnalytics(
      UUID userId,
      List<DividendView> dividendViews,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList,
      Instant startInstant,
      Instant endInstant,
      ZoneId zone) {
    if (dividendViews == null || dividendViews.isEmpty()) {
      return DividendAnalyticsResult.empty(
          dividendViews != null ? dividendViews : new ArrayList<>());
    }

    Set<LocalDate> basisDates =
        dividendViews.stream()
            .map(dividend -> resolveBasisDate(dividend, zone))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));

    Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> snapshotByDate =
        loadSnapshotsByDate(userId, basisDates);
    LocalDate maxBasisDate = basisDates.stream().max(Comparator.naturalOrder()).orElse(null);
    LocalDate periodStartDate =
      startInstant != null ? startInstant.atZone(zone).toLocalDate() : maxBasisDate;
    LocalDate periodEndDate = resolvePeriodEndDate(endInstant, maxBasisDate, zone);
    Map<Integer, Long> periodDayCountsByYear = buildPeriodDayCountsByYear(periodStartDate, periodEndDate);
    long totalPeriodDayCount = periodDayCountsByYear.values().stream().mapToLong(Long::longValue).sum();
    LocalDate tradeCoverageEndDate =
      Stream.of(maxBasisDate, periodEndDate)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
    Instant tradeEndDate =
      tradeCoverageEndDate != null
        ? tradeCoverageEndDate.plusDays(1).atStartOfDay(zone).toInstant()
        : null;

    List<TradeResponse> trades =
        tradeClient.findTrades(
            new TradeSearchRequest(userId, accountIdList, stockItemIdList, null, tradeEndDate)
                .toParams());

    Map<PositionKey, List<TradeResponse>> tradesByKey =
        trades.stream()
            .filter(
                trade ->
                    trade.accountId() != null
                        && trade.stockItemId() != null
                        && trade.tradeDate() != null)
            .collect(
                Collectors.groupingBy(
                    trade -> new PositionKey(trade.accountId(), trade.stockItemId()),
                    Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        list -> {
                          list.sort(
                              Comparator.comparing(
                                      (TradeResponse trade) ->
                                          trade.tradeDate().atZone(zone).toLocalDate())
                                  .thenComparing(
                                      trade ->
                                          trade.type()
                                                  == net.luversof.web.gate.stock.constant.TradeType
                                                      .BUY
                                              ? 0
                                              : 1)
                                  .thenComparing(TradeResponse::tradeDate));
                          return list;
                        })));

    Map<PositionKey, List<DividendView>> dividendsByKey =
        dividendViews.stream()
            .filter(dividend -> dividend.accountId() != null && dividend.stockItemId() != null)
            .collect(
                Collectors.groupingBy(
                    dividend -> new PositionKey(dividend.accountId(), dividend.stockItemId()),
                    Collectors.collectingAndThen(
                        Collectors.toCollection(ArrayList::new),
                        list -> {
                          list.sort(
                              Comparator.comparing(
                                      (DividendView dividend) -> resolveBasisDate(dividend, zone),
                                      Comparator.nullsLast(Comparator.naturalOrder()))
                                  .thenComparing(
                                      dividend ->
                                          Optional.ofNullable(dividend.payDate())
                                              .orElse(dividend.recordDate()),
                                      Comparator.nullsLast(Comparator.naturalOrder())));
                          return list;
                        })));

    Map<UUID, DividendView> enrichedById = new HashMap<>();
    Map<UUID, YieldAccumulator> stockAccumulators = new LinkedHashMap<>();
    Map<UUID, YieldAccumulator> accountAccumulators = new LinkedHashMap<>();
    Map<Integer, YieldAccumulator> yearlyAccumulators = new LinkedHashMap<>();
    YieldAccumulator portfolioAccumulator = new YieldAccumulator("portfolio", totalPeriodDayCount);

    for (Map.Entry<PositionKey, List<DividendView>> entry : dividendsByKey.entrySet()) {
      PositionKey key = entry.getKey();
      List<TradeResponse> tradeList = tradesByKey.getOrDefault(key, List.of());
      CostBasisState costBasisState = new CostBasisState();
      int tradeIndex = 0;

      for (DividendView dividend : entry.getValue()) {
        LocalDate basisDate = resolveBasisDate(dividend, zone);
        if (basisDate == null) {
          enrichedById.put(dividend.id(), dividend);
          continue;
        }

        while (tradeIndex < tradeList.size()) {
          TradeResponse trade = tradeList.get(tradeIndex);
          LocalDate tradeDate = trade.tradeDate().atZone(zone).toLocalDate();
          if (tradeDate.isAfter(basisDate)) {
            break;
          }
          costBasisState.apply(trade);
          tradeIndex++;
        }

        Integer quantity =
            dividend.quantity() != null && dividend.quantity() > 0
                ? dividend.quantity()
                : (costBasisState.rawQuantity() > 0 ? (int) costBasisState.rawQuantity() : null);
        BigDecimal averageCostBasis =
            quantity != null && quantity > 0 ? costBasisState.averageCost() : null;
        BigDecimal principalCost = multiplyQuantity(averageCostBasis, quantity);

        HoldingsSnapshotItem snapshotItem =
            snapshotByDate.getOrDefault(basisDate, Map.of()).get(dividend.stockItemId());
        BigDecimal referencePrice = snapshotItem != null ? snapshotItem.priceAtDate() : null;
        BigDecimal principalMarketValue = multiplyQuantity(referencePrice, quantity);

        BigDecimal yieldOnCostPct = percentage(dividend.netAmount(), principalCost);
        BigDecimal yieldOnMarketPct = percentage(dividend.netAmount(), principalMarketValue);

        DividendView enrichedDividend =
            new DividendView(
                dividend.id(),
                dividend.accountId(),
                dividend.accountName(),
                dividend.stockItemId(),
                dividend.stockItemName(),
                quantity,
                dividend.amountPerShare(),
                dividend.grossAmount(),
                dividend.tax(),
                dividend.taxableAmount(),
                dividend.netAmount(),
                dividend.recordDate(),
                dividend.payDate(),
                referencePrice,
                averageCostBasis,
                principalCost,
                principalMarketValue,
                yieldOnCostPct,
                yieldOnMarketPct);
        enrichedById.put(enrichedDividend.id(), enrichedDividend);

        portfolioAccumulator.accept(enrichedDividend);
        stockAccumulators
            .computeIfAbsent(
            dividend.stockItemId(),
            ignored -> new YieldAccumulator(dividend.stockItemName(), totalPeriodDayCount))
            .accept(enrichedDividend);
        accountAccumulators
            .computeIfAbsent(
            dividend.accountId(),
            ignored -> new YieldAccumulator(dividend.accountName(), totalPeriodDayCount))
            .accept(enrichedDividend);
        yearlyAccumulators
            .computeIfAbsent(
            basisDate.getYear(),
            year ->
              new YieldAccumulator(
                String.valueOf(year), periodDayCountsByYear.getOrDefault(year, 0L)))
            .accept(enrichedDividend);
      }

        PeriodPrincipalSummary periodPrincipalSummary =
          summarizePeriodPrincipalCosts(tradeList, periodStartDate, periodEndDate, zone);

        stockAccumulators
          .computeIfAbsent(
            key.stockItemId(),
            ignored ->
              new YieldAccumulator(
                entry.getValue().get(0).stockItemName(), totalPeriodDayCount))
          .acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
        accountAccumulators
          .computeIfAbsent(
            key.accountId(),
            ignored ->
              new YieldAccumulator(
                entry.getValue().get(0).accountName(), totalPeriodDayCount))
          .acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
        portfolioAccumulator.acceptDailyPrincipalCostSum(periodPrincipalSummary.principalCostSum());
        periodPrincipalSummary
          .principalCostSumByYear()
          .forEach(
            (year, principalCostSum) ->
              yearlyAccumulators
                .computeIfAbsent(
                  year,
                  ignored ->
                    new YieldAccumulator(
                      String.valueOf(year),
                      periodDayCountsByYear.getOrDefault(year, 0L)))
                .acceptDailyPrincipalCostSum(principalCostSum));
    }

    List<DividendView> enrichedDividends =
        dividendViews.stream()
            .map(dividend -> enrichedById.getOrDefault(dividend.id(), dividend))
            .collect(Collectors.toCollection(ArrayList::new));

    List<DividendYieldGroupView> stockYieldRows = sortYieldRows(stockAccumulators.values());
    List<DividendYieldGroupView> accountYieldRows = sortYieldRows(accountAccumulators.values());
    List<DividendYieldGroupView> yearlyYieldRows =
        yearlyAccumulators.entrySet().stream()
            .sorted(Map.Entry.<Integer, YieldAccumulator>comparingByKey().reversed())
            .map(entry -> entry.getValue().toView())
            .collect(Collectors.toCollection(ArrayList::new));

    return new DividendAnalyticsResult(
        enrichedDividends,
        portfolioAccumulator.hasData() ? portfolioAccumulator.toView() : null,
        yearlyYieldRows,
        stockYieldRows,
        accountYieldRows);
  }

  private Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> loadSnapshotsByDate(
      UUID userId, Set<LocalDate> basisDates) {
    Map<LocalDate, Map<UUID, HoldingsSnapshotItem>> result = new LinkedHashMap<>();
    for (LocalDate basisDate : basisDates) {
      var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
      params.add("userId", userId.toString());
      params.add("date", basisDate.toString());
      List<HoldingsSnapshotItem> snapshotItems = tradeProfitClient.holdingsSnapshot(params);
      Map<UUID, HoldingsSnapshotItem> itemsByStockId =
          snapshotItems.stream()
              .filter(item -> item.stockItemId() != null)
              .collect(
                  Collectors.toMap(
                      HoldingsSnapshotItem::stockItemId,
                      item -> item,
                      (left, right) -> left,
                      LinkedHashMap::new));
      result.put(basisDate, itemsByStockId);
    }
    return result;
  }

  private List<DividendYieldGroupView> sortYieldRows(Iterable<YieldAccumulator> accumulators) {
    List<DividendYieldGroupView> rows = new ArrayList<>();
    for (YieldAccumulator accumulator : accumulators) {
      if (accumulator.hasData()) {
        rows.add(accumulator.toView());
      }
    }
    rows.sort(
        Comparator.comparing(
                DividendYieldGroupView::yieldOnDailyAverageCostPct,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                DividendYieldGroupView::yieldOnCostPct,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                DividendYieldGroupView::totalNetAmount,
                Comparator.nullsLast(Comparator.reverseOrder())));
    return rows;
  }

  private static LocalDate resolvePeriodEndDate(Instant endInstant, LocalDate fallback, ZoneId zone) {
    if (endInstant != null) {
      return endInstant.minusNanos(1).atZone(zone).toLocalDate();
    }
    return fallback;
  }

  private static Map<Integer, Long> buildPeriodDayCountsByYear(
      LocalDate periodStartDate, LocalDate periodEndDate) {
    Map<Integer, Long> dayCountsByYear = new LinkedHashMap<>();
    if (periodStartDate == null || periodEndDate == null || periodEndDate.isBefore(periodStartDate)) {
      return dayCountsByYear;
    }

    LocalDate currentDate = periodStartDate;
    while (!currentDate.isAfter(periodEndDate)) {
      dayCountsByYear.merge(currentDate.getYear(), 1L, Long::sum);
      currentDate = currentDate.plusDays(1);
    }
    return dayCountsByYear;
  }

  private static PeriodPrincipalSummary summarizePeriodPrincipalCosts(
      List<TradeResponse> tradeList, LocalDate periodStartDate, LocalDate periodEndDate, ZoneId zone) {
    if (periodStartDate == null || periodEndDate == null || periodEndDate.isBefore(periodStartDate)) {
      return PeriodPrincipalSummary.empty();
    }

    CostBasisState costBasisState = new CostBasisState();
    int tradeIndex = 0;
    while (tradeIndex < tradeList.size()) {
      TradeResponse trade = tradeList.get(tradeIndex);
      LocalDate tradeDate = trade.tradeDate().atZone(zone).toLocalDate();
      if (!tradeDate.isBefore(periodStartDate)) {
        break;
      }
      costBasisState.apply(trade);
      tradeIndex++;
    }

    BigDecimal principalCostSum = BigDecimal.ZERO;
    Map<Integer, BigDecimal> principalCostSumByYear = new LinkedHashMap<>();
    LocalDate currentDate = periodStartDate;
    while (!currentDate.isAfter(periodEndDate)) {
      while (tradeIndex < tradeList.size()) {
        TradeResponse trade = tradeList.get(tradeIndex);
        LocalDate tradeDate = trade.tradeDate().atZone(zone).toLocalDate();
        if (tradeDate.isAfter(currentDate)) {
          break;
        }
        costBasisState.apply(trade);
        tradeIndex++;
      }

      BigDecimal principalCost = principalCostForState(costBasisState);
      if (principalCost != null && principalCost.compareTo(BigDecimal.ZERO) > 0) {
        principalCostSum = principalCostSum.add(principalCost);
        principalCostSumByYear.merge(currentDate.getYear(), principalCost, BigDecimal::add);
      }

      currentDate = currentDate.plusDays(1);
    }

    return new PeriodPrincipalSummary(principalCostSum, principalCostSumByYear);
  }

  private static LocalDate resolveBasisDate(DividendView dividend, ZoneId zone) {
    Instant basisInstant =
        dividend.recordDate() != null ? dividend.recordDate() : dividend.payDate();
    return basisInstant != null ? basisInstant.atZone(zone).toLocalDate() : null;
  }

  private static BigDecimal multiplyQuantity(BigDecimal price, Integer quantity) {
    if (price == null || quantity == null || quantity <= 0) {
      return null;
    }
    return price.multiply(BigDecimal.valueOf(quantity));
  }

  private static BigDecimal principalCostForState(CostBasisState costBasisState) {
    BigDecimal averageCost = costBasisState.averageCost();
    if (averageCost == null || costBasisState.rawQuantity() <= 0) {
      return null;
    }
    return averageCost.multiply(BigDecimal.valueOf(costBasisState.rawQuantity()));
  }

  private static BigDecimal percentage(BigDecimal amount, BigDecimal principal) {
    if (amount == null || principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    return amount.multiply(BigDecimal.valueOf(100)).divide(principal, 4, RoundingMode.HALF_UP);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private record PositionKey(UUID accountId, UUID stockItemId) {}

  private record PeriodPrincipalSummary(
      BigDecimal principalCostSum, Map<Integer, BigDecimal> principalCostSumByYear) {

    private static PeriodPrincipalSummary empty() {
      return new PeriodPrincipalSummary(BigDecimal.ZERO, Map.of());
    }
  }

  private record DividendAnalyticsResult(
      List<DividendView> dividendViews,
      DividendYieldGroupView portfolioYield,
      List<DividendYieldGroupView> yearlyYieldRows,
      List<DividendYieldGroupView> stockYieldRows,
      List<DividendYieldGroupView> accountYieldRows) {

    private static DividendAnalyticsResult empty(List<DividendView> dividendViews) {
      return new DividendAnalyticsResult(dividendViews, null, List.of(), List.of(), List.of());
    }
  }

  private static final class CostBasisState {
    private long rawQuantity;
    private BigDecimal totalCost = BigDecimal.ZERO;

    private void apply(TradeResponse trade) {
      int quantity = trade.quantity();
      if (quantity <= 0) {
        return;
      }

      BigDecimal amount = nz(trade.price()).multiply(BigDecimal.valueOf(quantity));
      if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.BUY) {
        rawQuantity += quantity;
        totalCost = totalCost.add(amount);
        return;
      }

      if (trade.type() == net.luversof.web.gate.stock.constant.TradeType.SELL && rawQuantity > 0) {
        BigDecimal sellProceeds = amount.subtract(nz(trade.fee())).subtract(nz(trade.tax()));
        BigDecimal cogs = sellProceeds.subtract(nz(trade.realizedProfit()));
        totalCost = totalCost.subtract(cogs);
        if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
          totalCost = BigDecimal.ZERO;
        }
        rawQuantity -= quantity;
        if (rawQuantity <= 0) {
          rawQuantity = 0;
          totalCost = BigDecimal.ZERO;
        }
      }
    }

    private long rawQuantity() {
      return rawQuantity;
    }

    private BigDecimal averageCost() {
      if (rawQuantity <= 0 || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
        return null;
      }
      return totalCost.divide(BigDecimal.valueOf(rawQuantity), 2, RoundingMode.HALF_UP);
    }
  }

  private static final class YieldAccumulator {
    private final String label;
    private final long periodDayCount;
    private BigDecimal totalGrossAmount = BigDecimal.ZERO;
    private BigDecimal totalNetAmount = BigDecimal.ZERO;
    private BigDecimal dailyPrincipalCostSum = BigDecimal.ZERO;
    private final Map<PositionKey, PrincipalAccumulator> principalByPosition =
        new LinkedHashMap<>();
    private long dividendCount;
    private Instant lastDividendDate;

    private YieldAccumulator(String label, long periodDayCount) {
      this.label = label;
      this.periodDayCount = periodDayCount;
    }

    private void accept(DividendView dividend) {
      totalGrossAmount = totalGrossAmount.add(nz(dividend.grossAmount()));
      totalNetAmount = totalNetAmount.add(nz(dividend.netAmount()));
      dividendCount++;

      if (dividend.accountId() != null && dividend.stockItemId() != null) {
        principalByPosition
            .computeIfAbsent(
                new PositionKey(dividend.accountId(), dividend.stockItemId()),
                ignored -> new PrincipalAccumulator())
            .accept(dividend);
      }

      Instant displayDate = dividend.payDate() != null ? dividend.payDate() : dividend.recordDate();
      if (displayDate != null
          && (lastDividendDate == null || displayDate.isAfter(lastDividendDate))) {
        lastDividendDate = displayDate;
      }
    }

    private void acceptDailyPrincipalCostSum(BigDecimal principalCostSum) {
      if (principalCostSum != null && principalCostSum.compareTo(BigDecimal.ZERO) > 0) {
        dailyPrincipalCostSum = dailyPrincipalCostSum.add(principalCostSum);
      }
    }

    private boolean hasData() {
      return dividendCount > 0;
    }

    private DividendYieldGroupView toView() {
      BigDecimal averageDailyPrincipalCost =
        periodDayCount > 0 && dailyPrincipalCostSum.compareTo(BigDecimal.ZERO) > 0
          ? dailyPrincipalCostSum.divide(
            BigDecimal.valueOf(periodDayCount), 2, RoundingMode.HALF_UP)
          : null;
      BigDecimal averagePrincipalCost = null;
      BigDecimal averagePrincipalMarketValue = null;

      for (PrincipalAccumulator principalAccumulator : principalByPosition.values()) {
        BigDecimal positionAveragePrincipalCost = principalAccumulator.averagePrincipalCost();
        if (positionAveragePrincipalCost != null) {
          averagePrincipalCost =
              averagePrincipalCost == null
                  ? positionAveragePrincipalCost
                  : averagePrincipalCost.add(positionAveragePrincipalCost);
        }

        BigDecimal positionAveragePrincipalMarketValue =
            principalAccumulator.averagePrincipalMarketValue();
        if (positionAveragePrincipalMarketValue != null) {
          averagePrincipalMarketValue =
              averagePrincipalMarketValue == null
                  ? positionAveragePrincipalMarketValue
                  : averagePrincipalMarketValue.add(positionAveragePrincipalMarketValue);
        }
      }

    BigDecimal yieldOnDailyAverageCostPct =
      averageDailyPrincipalCost != null
        ? percentage(totalNetAmount, averageDailyPrincipalCost)
        : null;
      BigDecimal yieldOnCostPct =
          averagePrincipalCost != null ? percentage(totalNetAmount, averagePrincipalCost) : null;
      BigDecimal yieldOnMarketPct =
          averagePrincipalMarketValue != null
              ? percentage(totalNetAmount, averagePrincipalMarketValue)
              : null;
      return new DividendYieldGroupView(
          label,
          totalGrossAmount,
          totalNetAmount,
      averageDailyPrincipalCost,
          averagePrincipalCost,
          averagePrincipalMarketValue,
      yieldOnDailyAverageCostPct,
          yieldOnCostPct,
          yieldOnMarketPct,
          dividendCount,
          lastDividendDate);
    }
  }

  private static final class PrincipalAccumulator {
    private BigDecimal principalCostSum = BigDecimal.ZERO;
    private long principalCostCount;
    private BigDecimal principalMarketSum = BigDecimal.ZERO;
    private long principalMarketCount;

    private void accept(DividendView dividend) {
      if (dividend.principalCost() != null
          && dividend.principalCost().compareTo(BigDecimal.ZERO) > 0) {
        principalCostSum = principalCostSum.add(dividend.principalCost());
        principalCostCount++;
      }
      if (dividend.principalMarketValue() != null
          && dividend.principalMarketValue().compareTo(BigDecimal.ZERO) > 0) {
        principalMarketSum = principalMarketSum.add(dividend.principalMarketValue());
        principalMarketCount++;
      }
    }

    private BigDecimal averagePrincipalCost() {
      if (principalCostCount <= 0) {
        return null;
      }
      return principalCostSum.divide(
          BigDecimal.valueOf(principalCostCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averagePrincipalMarketValue() {
      if (principalMarketCount <= 0) {
        return null;
      }
      return principalMarketSum.divide(
          BigDecimal.valueOf(principalMarketCount), 2, RoundingMode.HALF_UP);
    }
  }
}
