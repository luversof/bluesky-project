package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    var seriesParams = request.toParams();
    seriesParams.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> timeSeries = tradeProfitClient.timeSeries(seriesParams);

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

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/period-return")
  public String assetGrowthPeriodReturn(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String timeZone,
      Model model) {
    var userId = UserUtil.getUserId();
    var summary = buildAssetGrowthPeriodReturnSummary(userId, from, to, timeZone);
    model.addAttribute("fromDate", summary.fromDate());
    model.addAttribute("toDate", summary.toDate());
    model.addAttribute("periodReturnRatePct", summary.periodReturnRatePct());
    model.addAttribute("returnCalculable", summary.returnCalculable());
    model.addAttribute("timeWeightedReturnPct", summary.timeWeightedReturnPct());
    model.addAttribute("periodProfit", summary.periodProfit());
    model.addAttribute("principalDelta", summary.principalDelta());
    model.addAttribute("unrealizedStart", summary.unrealizedStart());
    model.addAttribute("unrealizedEnd", summary.unrealizedEnd());
    model.addAttribute("unrealizedEndPct", summary.unrealizedEndPct());
    model.addAttribute("recoveredAmount", summary.recoveredAmount());
    model.addAttribute("netNewProfit", summary.netNewProfit());
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

  private AssetGrowthPeriodReturnSummary buildAssetGrowthPeriodReturnSummary(
      UUID userId, String from, String to, String timeZone) {
    if (userId == null || from == null || from.isBlank() || to == null || to.isBlank()) {
      return new AssetGrowthPeriodReturnSummary(
          from, to, null, false, null, null, null, null, null, null, null, null);
    }

    LocalDate fromDate;
    LocalDate toDate;
    try {
      fromDate = LocalDate.parse(from);
      toDate = LocalDate.parse(to);
    } catch (Exception ex) {
      logger.warn("Failed to parse asset growth period range: from={} to={}", from, to, ex);
      return new AssetGrowthPeriodReturnSummary(
          from, to, null, false, null, null, null, null, null, null, null, null);
    }

    if (toDate.isBefore(fromDate)) {
      return new AssetGrowthPeriodReturnSummary(
          fromDate.toString(),
          toDate.toString(),
          null,
          false,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    try {
      ZoneId zone = resolveZoneId(timeZone);
      HoldingsValueWindow holdingsValueWindow =
          loadHoldingsValueWindow(userId, fromDate, toDate, timeZone, zone);

      Double periodReturnRate =
          calculatePeriodGrowthRate(
              holdingsValueWindow.openingValue(), holdingsValueWindow.closingValue());
      Double periodReturnRatePct =
          periodReturnRate != null && Double.isFinite(periodReturnRate)
              ? periodReturnRate * 100.0d
              : null;

      return new AssetGrowthPeriodReturnSummary(
          fromDate.toString(),
          toDate.toString(),
          periodReturnRatePct,
          periodReturnRatePct != null,
          holdingsValueWindow.timeWeightedReturnPct(),
          holdingsValueWindow.periodProfit(),
          holdingsValueWindow.principalDelta(),
          holdingsValueWindow.unrealizedStart(),
          holdingsValueWindow.unrealizedEnd(),
          holdingsValueWindow.unrealizedEndPct(),
          holdingsValueWindow.recoveredAmount(),
          holdingsValueWindow.netNewProfit());
    } catch (Exception ex) {
      logger.warn(
          "Failed to build asset growth period return summary: from={} to={} timeZone={}",
          from,
          to,
          timeZone,
          ex);
      return new AssetGrowthPeriodReturnSummary(
          fromDate.toString(),
          toDate.toString(),
          null,
          false,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }
  }

  private HoldingsValueWindow loadHoldingsValueWindow(
      UUID userId, LocalDate fromDate, LocalDate toDate, String timeZone, ZoneId zone) {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);
    request.setStartDate(fromDate.atStartOfDay(zone).toInstant());
    request.setEndDate(toDate.plusDays(1).atStartOfDay(zone).toInstant());
    request.setTimeZone(timeZone);

    var params = request.toParams();
    params.add("granularity", "DAILY");

    List<TradeProfitTimeSeriesPoint> series = tradeProfitClient.timeSeries(params);
    if (series == null || series.isEmpty()) {
      return HoldingsValueWindow.empty();
    }

    BigDecimal openingValue = null;
    BigDecimal closingValue = null;
    TradeProfitTimeSeriesPoint firstPoint = null;
    TradeProfitTimeSeriesPoint lastPoint = null;
    // TWR(시간가중수익률): 일별로 입출금(원금 변동)을 제거한 수익률을 곱해 누적한다.
    // 평가액 성장률은 입금까지 성과로 잡히므로, 순수 운용 성과는 이 값으로 본다.
    double timeWeightedFactor = 1.0d;
    TradeProfitTimeSeriesPoint previousPoint = null;

    for (TradeProfitTimeSeriesPoint point : series) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      LocalDate pointDate = point.timestamp().atZone(zone).toLocalDate();
      if (pointDate.isBefore(fromDate) || pointDate.isAfter(toDate)) {
        continue;
      }
      BigDecimal holdingsValue = nz(point.totalHoldingsValue());
      if (openingValue == null) {
        openingValue = holdingsValue;
        firstPoint = point;
      }
      closingValue = holdingsValue;
      lastPoint = point;

      if (previousPoint != null) {
        BigDecimal previousValue = nz(previousPoint.totalHoldingsValue());
        if (previousValue.compareTo(BigDecimal.ZERO) > 0) {
          // 당일 순수 손익 = (평가액 증가 - 원금 유입) + 실현손익 증가 + 배당 증가
          BigDecimal cashFlow =
              nz(point.totalHoldingsCost()).subtract(nz(previousPoint.totalHoldingsCost()));
          BigDecimal realizedGain =
              nz(point.cumulativeRealizedProfit())
                  .subtract(nz(previousPoint.cumulativeRealizedProfit()));
          BigDecimal dividendGain =
              nz(point.cumulativeDividend()).subtract(nz(previousPoint.cumulativeDividend()));
          BigDecimal dailyGain =
              holdingsValue
                  .subtract(previousValue)
                  .subtract(cashFlow)
                  .add(realizedGain)
                  .add(dividendGain);
          double dailyReturn =
              dailyGain.divide(previousValue, 10, RoundingMode.HALF_UP).doubleValue();
          timeWeightedFactor *= (1.0d + dailyReturn);
        }
      }
      previousPoint = point;
    }

    if (firstPoint == null || lastPoint == null) {
      return HoldingsValueWindow.empty();
    }

    // 기간 총 손익 = 누적손익(미실현 + 실현 + 배당)의 기말 - 기초
    BigDecimal periodProfit = accumulatedProfit(lastPoint).subtract(accumulatedProfit(firstPoint));
    BigDecimal principalDelta =
        nz(lastPoint.totalHoldingsCost()).subtract(nz(firstPoint.totalHoldingsCost()));
    // 기간 손익이 "손실 회복분"인지 "순수 이익"인지 구분되도록 평가손익 기초/기말을 함께 준다.
    // (예: -8,877만 -> +1,271만 이면 1억 손익 대부분이 회복분이고 누적은 +2.37%에 불과)
    BigDecimal unrealizedStart =
        nz(firstPoint.totalHoldingsValue()).subtract(nz(firstPoint.totalHoldingsCost()));
    BigDecimal unrealizedEnd =
        nz(lastPoint.totalHoldingsValue()).subtract(nz(lastPoint.totalHoldingsCost()));
    BigDecimal endCost = nz(lastPoint.totalHoldingsCost());
    Double unrealizedEndPct =
        endCost.compareTo(BigDecimal.ZERO) > 0
            ? unrealizedEnd
                .multiply(BigDecimal.valueOf(100))
                .divide(endCost, 4, RoundingMode.HALF_UP)
                .doubleValue()
            : null;

    // 기간 손익을 '손실 회복분'과 '순증분'으로 분해한다.
    // 회복분 = 마이너스였던 평가손익이 0 쪽으로 메워진 금액, 순증분 = 그 위로 새로 번 금액.
    // (회복 + 순증 = 기간 손익 이 항상 성립하도록 순증분은 잔차로 구한다.)
    BigDecimal lossGapStart = unrealizedStart.min(BigDecimal.ZERO).negate();
    BigDecimal lossGapEnd = unrealizedEnd.min(BigDecimal.ZERO).negate();
    BigDecimal recoveredAmount = lossGapStart.subtract(lossGapEnd);
    BigDecimal netNewProfit = periodProfit.subtract(recoveredAmount);
    Double timeWeightedReturnPct =
        Double.isFinite(timeWeightedFactor) ? (timeWeightedFactor - 1.0d) * 100.0d : null;

    return new HoldingsValueWindow(
        openingValue != null ? openingValue : BigDecimal.ZERO,
        closingValue != null ? closingValue : BigDecimal.ZERO,
        timeWeightedReturnPct,
        periodProfit,
        principalDelta,
        unrealizedStart,
        unrealizedEnd,
        unrealizedEndPct,
        recoveredAmount,
        netNewProfit);
  }

  /** 시점까지 쌓인 총 손익 = 미실현(평가액 - 원금) + 누적 실현손익 + 누적 배당. */
  private static BigDecimal accumulatedProfit(TradeProfitTimeSeriesPoint point) {
    return nz(point.totalHoldingsValue())
        .subtract(nz(point.totalHoldingsCost()))
        .add(nz(point.cumulativeRealizedProfit()))
        .add(nz(point.cumulativeDividend()));
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private Double calculatePeriodGrowthRate(BigDecimal openingValue, BigDecimal closingValue) {
    if (openingValue == null || closingValue == null) {
      return null;
    }
    if (openingValue.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }

    return closingValue
        .subtract(openingValue)
        .divide(openingValue, 8, RoundingMode.HALF_UP)
        .doubleValue();
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

  private record HoldingsValueWindow(
      BigDecimal openingValue,
      BigDecimal closingValue,
      Double timeWeightedReturnPct,
      BigDecimal periodProfit,
      BigDecimal principalDelta,
      BigDecimal unrealizedStart,
      BigDecimal unrealizedEnd,
      Double unrealizedEndPct,
      BigDecimal recoveredAmount,
      BigDecimal netNewProfit) {

    private static HoldingsValueWindow empty() {
      return new HoldingsValueWindow(
          BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null, null, null);
    }
  }

  private record AssetGrowthPeriodReturnSummary(
      String fromDate,
      String toDate,
      Double periodReturnRatePct,
      boolean returnCalculable,
      Double timeWeightedReturnPct,
      BigDecimal periodProfit,
      BigDecimal principalDelta,
      BigDecimal unrealizedStart,
      BigDecimal unrealizedEnd,
      Double unrealizedEndPct,
      BigDecimal recoveredAmount,
      BigDecimal netNewProfit) {}
}
