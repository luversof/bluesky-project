package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DataFirstDateClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockAssetGrowthHtmxController extends StockBaseHtmxController {
  private static final Logger logger =
      LoggerFactory.getLogger(StockAssetGrowthHtmxController.class);

  private final DataFirstDateClient dataFirstDateClient;

  public StockAssetGrowthHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      DataFirstDateClient dataFirstDateClient,
      MessageSource messageSource) {
    super(
        tradeProfitClient,
        tradeClient,
        accountClient,
        stockItemClient,
        dividendClient,
        messageSource);
    this.dataFirstDateClient = dataFirstDateClient;
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/view")
  public String assetGrowthView(
      TradeProfitRequest request,
      @RequestParam(required = false) java.util.List<String> stockTagList,
      @RequestParam(required = false) String rangeMode,
      Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    request.setUserId(userId);
    String effectiveRangeMode = rangeMode;

    // Load filter source lists (full account/stock lists for the detail filter form).
    List<StockItem> stockItemList = emptyIfNull(stockItemClient.getStockItems());
    List<net.luversof.web.gate.stock.domain.Account> accountList =
        emptyIfNull(accountClient.getAccountsByUserId(userId));

    // Resolve tag selection -> stock item ids (tags drive the stock selection).
    StockTagSelection stockTagSelection =
        resolveStockTagSelection(stockItemList, request.getStockItemIdList(), stockTagList);
    List<String> selectedStockTags = stockTagSelection.selectedStockTags();
    request.setStockItemIdList(stockTagSelection.requestedStockItemIds());

    List<UUID> requestedAccountIds = request.getAccountIdList();
    List<UUID> requestedStockItemIds = request.getStockItemIdList();

    // Remember whether the client actually provided a date range (before YTD default),
    // so the dropdowns only narrow to in-range accounts/stocks when a range was chosen.
    boolean clientProvidedRange =
        !((request.getStartDate() == null || request.getStartDate().toEpochMilli() == 0)
            && (request.getEndDate() == null || request.getEndDate().toEpochMilli() == 0)
            && (rangeMode == null || rangeMode.isBlank()));

    // If no date range provided, default to this year (ytd).
    // 단, '전체(all)'는 빈 기간으로 전체 데이터를 의미하므로 YTD 기본값을 적용하지 않는다.
    if (request.getStartDate() == null
        && request.getEndDate() == null
        && !"all".equalsIgnoreCase(rangeMode)) {
      ZoneId zone =
          (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
              ? ZoneId.of(request.getTimeZone())
              : ZoneId.systemDefault();
      LocalDate now = LocalDate.now(zone);
      request.setStartDate(LocalDate.of(now.getYear(), 1, 1).atStartOfDay(zone).toInstant());
      request.setEndDate(now.plusDays(1).atStartOfDay(zone).toInstant());
      if (effectiveRangeMode == null || effectiveRangeMode.isBlank()) {
        effectiveRangeMode = "ytd";
      }
    }

    // Chart data for the SELECTED range. The upstream aggregateTimeSeries simulates from
    // the first trade (carrying prior holdings) and only outputs from the requested start,
    // so a range query already reflects carried-over holdings at the right granularity
    // (AUTO picks DAILY for short windows). No client-side x-axis windowing needed.
    // 시리즈와 기간 요약을 한 번에 받는다. 예전에는 요약을 별도 프래그먼트가 다시 호출해
    // 같은 시뮬레이션(전체 거래 이력)이 두 번 돌았다.
    var seriesParams = request.toParams();
    seriesParams.add("granularity", "AUTO");
    var timeSeriesResult = tradeProfitClient.timeSeriesWithSummary(seriesParams);
    List<TradeProfitTimeSeriesPoint> timeSeries =
        timeSeriesResult != null ? timeSeriesResult.series() : null;
    addPeriodSummaryAttributes(model, timeSeriesResult != null ? timeSeriesResult.summary() : null);
    model.addAttribute(
        "yearlySummaries",
        timeSeriesResult != null && timeSeriesResult.yearly() != null
            ? timeSeriesResult.yearly()
            : List.of());

    // 날짜 네비게이션의 "이전" 가드용 최초 데이터 일자.
    // 전 기간 시계열을 다시 집계하면(기간 제거 timeSeries) 이 화면에서만 초 단위가 걸렸다.
    // 집계 엔드포인트 1회로 대체한다.
    var zone =
        (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
            ? java.time.ZoneId.of(request.getTimeZone())
            : java.time.ZoneId.systemDefault();
    var firstDateResponse = dataFirstDateClient.findDataFirstDate(userId);
    java.time.Instant firstInstant = firstDateResponse.tradeFirstDate();
    if (firstDateResponse.dividendFirstDate() != null
        && (firstInstant == null || firstDateResponse.dividendFirstDate().isBefore(firstInstant))) {
      firstInstant = firstDateResponse.dividendFirstDate();
    }
    java.time.LocalDate dataFirstDate =
        firstInstant != null ? firstInstant.atZone(zone).toLocalDate() : null;

    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    model.addAttribute("rangeMode", effectiveRangeMode);
    model.addAttribute("startDate", request.getStartDate());
    model.addAttribute("endDate", request.getEndDate());
    model.addAttribute("timeZone", request.getTimeZone());

    // Detail filter form model. Like realized-profit: when a date range is chosen,
    // narrow the account/stock dropdowns to items that have trades in that range,
    // but keep previously-selected items visible even if absent from the range.
    Set<UUID> tradeAccountIds;
    Set<UUID> tradeStockIds;
    List<net.luversof.web.gate.stock.domain.Account> filteredAccountList;
    List<StockItem> filteredStockItemList;
    if (clientProvidedRange) {
      TradeProfitRequest dateOnlyReq = new TradeProfitRequest();
      dateOnlyReq.setUserId(userId);
      dateOnlyReq.setStartDate(request.getStartDate());
      dateOnlyReq.setEndDate(request.getEndDate());
      dateOnlyReq.setTimeZone(request.getTimeZone());
      var dateRangeEnriched = new ArrayList<>(getEnrichedTradeProfits(dateOnlyReq));
      tradeAccountIds =
          dateRangeEnriched.stream()
              .map(tp -> tp.accountId())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      tradeStockIds =
          dateRangeEnriched.stream()
              .map(tp -> tp.stockItemId())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      filteredAccountList =
          accountList.stream().filter(a -> tradeAccountIds.contains(a.id())).toList();
      filteredStockItemList =
          stockItemList.stream().filter(s -> tradeStockIds.contains(s.id())).toList();
    } else {
      tradeAccountIds = Collections.emptySet();
      tradeStockIds = Collections.emptySet();
      filteredAccountList = accountList;
      filteredStockItemList = stockItemList;
    }

    Set<UUID> availableAccountIds =
        accountList.stream().map(a -> a.id()).collect(Collectors.toSet());
    List<UUID> effectiveAccountIds =
        (requestedAccountIds != null
                && !requestedAccountIds.isEmpty()
                && availableAccountIds.containsAll(requestedAccountIds))
            ? requestedAccountIds
            : List.of();

    Set<UUID> availableStockIds =
        stockItemList.stream().map(s -> s.id()).collect(Collectors.toSet());
    List<UUID> effectiveStockItemIds =
        (requestedStockItemIds != null && availableStockIds.containsAll(requestedStockItemIds))
            ? requestedStockItemIds
            : (stockTagSelection.hasFilter() ? List.of() : requestedStockItemIds);
    if (effectiveStockItemIds == null) {
      effectiveStockItemIds = List.of();
    }

    // Final dropdown lists: narrowed list + previously-selected items not in range.
    List<net.luversof.web.gate.stock.domain.Account> finalAccountList;
    if (clientProvidedRange) {
      finalAccountList = new ArrayList<>(filteredAccountList);
      if (requestedAccountIds != null) {
        for (UUID sel : requestedAccountIds) {
          if (sel == null || tradeAccountIds.contains(sel)) {
            continue;
          }
          accountList.stream()
              .filter(a -> a.id().equals(sel))
              .findFirst()
              .ifPresent(
                  a -> {
                    if (finalAccountList.stream().noneMatch(x -> x.id().equals(a.id()))) {
                      finalAccountList.add(0, a);
                    }
                  });
        }
      }
    } else {
      finalAccountList = new ArrayList<>(filteredAccountList);
    }

    List<StockItem> finalStockItemList;
    if (clientProvidedRange) {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
      if (requestedStockItemIds != null) {
        for (UUID sel : requestedStockItemIds) {
          if (sel == null || tradeStockIds.contains(sel)) {
            continue;
          }
          stockItemList.stream()
              .filter(s -> s.id().equals(sel))
              .findFirst()
              .ifPresent(
                  s -> {
                    if (finalStockItemList.stream().noneMatch(x -> x.id().equals(s.id()))) {
                      finalStockItemList.add(0, s);
                    }
                  });
        }
      }
    } else {
      finalStockItemList = new ArrayList<>(filteredStockItemList);
    }

    model.addAttribute("accountList", finalAccountList);
    model.addAttribute("stockItemList", finalStockItemList);
    model.addAttribute("stockTagList", getAvailableStockTags(stockItemList));
    model.addAttribute("selectedAccountIds", effectiveAccountIds);
    model.addAttribute("selectedStockItemIds", effectiveStockItemIds);
    model.addAttribute("selectedStockTags", selectedStockTags);
    return "stock/htmx/asset-growth";
  }

  /** 기간 요약 값을 화면 모델에 실는다. 요약이 없으면 '계산 불가'로 렌더된다. */
  private void addPeriodSummaryAttributes(Model model, TradeProfitTimeSeriesSummary summary) {
    model.addAttribute("periodReturnRatePct", summary != null ? summary.growthRatePct() : null);
    model.addAttribute("returnCalculable", summary != null && summary.growthRatePct() != null);
    model.addAttribute(
        "timeWeightedReturnPct", summary != null ? summary.timeWeightedReturnPct() : null);
    model.addAttribute("periodProfit", summary != null ? summary.periodProfit() : null);
    model.addAttribute("principalDelta", summary != null ? summary.principalDelta() : null);
    model.addAttribute("unrealizedStart", summary != null ? summary.unrealizedStart() : null);
    model.addAttribute("unrealizedEnd", summary != null ? summary.unrealizedEnd() : null);
    model.addAttribute("unrealizedEndPct", summary != null ? summary.unrealizedEndPct() : null);
    model.addAttribute("recoveredAmount", summary != null ? summary.recoveredAmount() : null);
    model.addAttribute("netNewProfit", summary != null ? summary.netNewProfit() : null);
    model.addAttribute("maxDrawdownPct", summary != null ? summary.maxDrawdownPct() : null);
    model.addAttribute(
        "maxDrawdownPeakDate", summary != null ? summary.maxDrawdownPeakDate() : null);
    model.addAttribute(
        "maxDrawdownTroughDate", summary != null ? summary.maxDrawdownTroughDate() : null);
    model.addAttribute("currentDrawdownPct", summary != null ? summary.currentDrawdownPct() : null);
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/period-return")
  public String assetGrowthPeriodReturn(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String timeZone,
      Model model) {
    var userId = UserUtil.getUserId();
    model.addAttribute("fromDate", from);
    model.addAttribute("toDate", to);
    addPeriodSummaryAttributes(model, loadPeriodSummary(userId, from, to, timeZone));
    return "stock/htmx/fragments/assetGrowthPeriodReturnSummary";
  }

  @GetMapping("/holdings-snapshot")
  public String holdingsSnapshot(
      @RequestParam(required = false) String date,
      @RequestParam(required = false) String accountId,
      Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }
    if (date == null || date.isBlank()) {
      model.addAttribute("holdings", List.of());
      model.addAttribute("date", "");
      return "stock/htmx/holdings-snapshot";
    }
    var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
    params.add("userId", userId.toString());
    params.add("date", date);
    if (accountId != null && !accountId.isBlank()) {
      params.add("accountId", accountId);
    }
    List<HoldingsSnapshotItem> holdings = emptyIfNull(tradeProfitClient.holdingsSnapshot(params));
    model.addAttribute("holdings", holdings);
    model.addAttribute("date", date);
    return "stock/htmx/holdings-snapshot";
  }

  @GetMapping("/trade-history")
  public String tradeHistory(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size,
      Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      model.addAttribute("trades", List.of());
      model.addAttribute("totalItems", 0);
      model.addAttribute("currentPage", 1);
      model.addAttribute("totalPages", 0);
      model.addAttribute("pageSize", size);
      model.addAttribute("from", from);
      model.addAttribute("to", to);
      model.addAttribute("totalBuy", BigDecimal.ZERO);
      model.addAttribute("totalSell", BigDecimal.ZERO);
      model.addAttribute("totalRealizedProfit", BigDecimal.ZERO);
      model.addAttribute("tradePeriod", "");
      return "stock/htmx/trade-history";
    }

    Instant tradeStart =
        (from != null && !from.isBlank())
            ? LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant()
            : null;
    Instant tradeEnd =
        (to != null && !to.isBlank())
            ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            : null;

    var tradeReq = new TradeSearchRequest(userId, null, null, tradeStart, tradeEnd);
    var allFromApi = emptyIfNull(tradeClient.findTrades(tradeReq.toParams()));

    List<StockItem> stockItems = emptyIfNull(stockItemClient.getStockItems());
    Map<UUID, String> stockItemNames =
        stockItems.stream().collect(Collectors.toMap(StockItem::id, StockItem::name, (l, r) -> l));

    var allTrades =
        allFromApi.stream()
            .map(
                t ->
                    new TradeResponse(
                        t.id(),
                        t.accountId(),
                        t.stockItemId(),
                        stockItemNames.getOrDefault(t.stockItemId(), msg("stock.label.unknown")),
                        t.type(),
                        t.quantity(),
                        t.price(),
                        t.fee(),
                        t.tax(),
                        t.amount(),
                        t.realizedProfit(),
                        t.tradeDate()))
            .sorted(
                Comparator.comparing(
                    TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toCollection(ArrayList::new));

    BigDecimal totalBuy =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.BUY)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSell =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalRealizedProfit =
        allTrades.stream()
            .filter(t -> t.type() == TradeType.SELL)
            .map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalFee =
        allTrades.stream()
            .map(t -> t.fee() != null ? t.fee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalTax =
        allTrades.stream()
            .map(t -> t.tax() != null ? t.tax() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    int totalItems = allTrades.size();
    if (size <= 0) size = 20;
    int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / size) : 0;
    int currentPage = Math.max(1, Math.min(page, Math.max(1, totalPages)));
    int fromIdx = (currentPage - 1) * size;
    int toIdx = Math.min(fromIdx + size, totalItems);
    List<TradeResponse> pagedTrades =
        fromIdx < totalItems ? allTrades.subList(fromIdx, toIdx) : Collections.emptyList();

    String periodFrom = (from != null && !from.isBlank()) ? from : "";
    String periodTo = (to != null && !to.isBlank()) ? to : "";
    String tradePeriod =
        periodFrom.isEmpty() && periodTo.isEmpty()
            ? msg("stock.label.period.all")
            : periodFrom + (periodTo.isEmpty() ? "" : " ~ " + periodTo);

    // 화면에는 오름차순(오래된 것 위)으로 표시 — 조회/페이징은 그대로(1페이지=최신 묶음).
    List<TradeResponse> displayTrades = new java.util.ArrayList<>(pagedTrades);
    java.util.Collections.reverse(displayTrades);
    model.addAttribute("trades", displayTrades);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute(
        "pagination",
        new Pagination(
            new PageImpl<>(pagedTrades, PageRequest.of(currentPage - 1, size), totalItems)));
    model.addAttribute("pageSize", size);
    model.addAttribute("from", periodFrom);
    model.addAttribute("to", periodTo);
    model.addAttribute("totalBuy", totalBuy);
    model.addAttribute("totalSell", totalSell);
    model.addAttribute("totalRealizedProfit", totalRealizedProfit);
    model.addAttribute("totalFee", totalFee);
    model.addAttribute("totalTax", totalTax);
    model.addAttribute("tradePeriod", tradePeriod);
    return "stock/htmx/trade-history";
  }

  private ZoneId resolveZoneId(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return ZoneId.systemDefault();
    }
    try {
      return ZoneId.of(timeZone);
    } catch (Exception ex) {
      logger.debug("Unknown time zone '{}', falling back to system default", timeZone, ex);
      return ZoneId.systemDefault();
    }
  }

  /** 요약만 필요한 호출용. 시리즈와 요약을 함께 주는 엔드포인트를 그대로 재사용한다. */
  private TradeProfitTimeSeriesSummary loadPeriodSummary(
      UUID userId, String from, String to, String timeZone) {
    if (userId == null || from == null || from.isBlank() || to == null || to.isBlank()) {
      return null;
    }
    try {
      LocalDate fromDate = LocalDate.parse(from);
      LocalDate toDate = LocalDate.parse(to);
      if (toDate.isBefore(fromDate)) {
        return null;
      }
      ZoneId zone = resolveZoneId(timeZone);
      TradeProfitRequest request = new TradeProfitRequest();
      request.setUserId(userId);
      request.setStartDate(fromDate.atStartOfDay(zone).toInstant());
      request.setEndDate(toDate.plusDays(1).atStartOfDay(zone).toInstant());
      request.setTimeZone(timeZone);
      var params = request.toParams();
      params.add("granularity", "DAILY");
      var result = tradeProfitClient.timeSeriesWithSummary(params);
      return result != null ? result.summary() : null;
    } catch (Exception ex) {
      logger.warn("Failed to load period summary: from={} to={} tz={}", from, to, timeZone, ex);
      return null;
    }
  }
}
