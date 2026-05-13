package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.request.DividendRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeSearchRequest;
import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockAssetGrowthHtmxController extends StockBaseHtmxController {
  private static final Logger logger =
      LoggerFactory.getLogger(StockAssetGrowthHtmxController.class);

  public StockAssetGrowthHtmxController(
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
  @GetMapping("/asset-growth/view")
  public String assetGrowthView(
      TradeProfitRequest request, @RequestParam(required = false) String rangeMode, Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    request.setUserId(userId);
    String effectiveRangeMode = rangeMode;

    // If no date range provided, default to this year (ytd)
    if (request.getStartDate() == null && request.getEndDate() == null) {
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

    var params = request.toParams();
    params.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> timeSeries = tradeProfitClient.timeSeries(params);

    // compute overall dataFirstDate (earliest available timestamp) so client can
    // enable Prev when earlier data exists outside the current timeSeries
    var allReq = new TradeProfitRequest();
    allReq.setUserId(userId);
    var allParams = allReq.toParams();
    allParams.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> allSeries = tradeProfitClient.timeSeries(allParams);
    java.time.LocalDate dataFirstDate = null;
    if (allSeries != null && !allSeries.isEmpty()) {
      var zone =
          (request.getTimeZone() != null && !request.getTimeZone().isEmpty())
              ? java.time.ZoneId.of(request.getTimeZone())
              : java.time.ZoneId.systemDefault();
      dataFirstDate =
          allSeries.stream()
              .filter(pt -> pt.timestamp() != null)
              .map(pt -> pt.timestamp().atZone(zone).toLocalDate())
              .min(java.util.Comparator.naturalOrder())
              .orElse(null);
    }

    model.addAttribute("timeSeries", timeSeries);
    model.addAttribute("dataFirstDate", dataFirstDate != null ? dataFirstDate.toString() : "");
    model.addAttribute("rangeMode", effectiveRangeMode);
    model.addAttribute("startDate", request.getStartDate());
    model.addAttribute("endDate", request.getEndDate());
    model.addAttribute("timeZone", request.getTimeZone());
    return "stock/htmx/asset-growth";
  }

  @GetMapping(value = "/asset-growth/data", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Map<String, Object> assetGrowthData(
      TradeProfitRequest request,
      @RequestParam(name = "from", required = false) String from,
      @RequestParam(name = "to", required = false) String to,
      @RequestParam(name = "gran", required = false) String gran) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return Map.of("labels", List.of());
    }
    request.setUserId(userId);
    if (from != null) {
      request.setStartDate(
          LocalDate.parse(from).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }
    if (to != null) {
      // preserve original behavior: treat 'to' as exclusive end (start of next day)
      request.setEndDate(
          LocalDate.parse(to)
              .plusDays(1)
              .atStartOfDay(java.time.ZoneId.systemDefault())
              .toInstant());
    }
    var params = request.toParams();
    params.add("granularity", gran != null ? gran : "AUTO");
    var timeSeries = tradeProfitClient.timeSeries(params);
    try {
      logger.debug(
          "assetGrowthData called: from={} to={} gran={} requestStart={} requestEnd={}",
          from,
          to,
          gran,
          request.getStartDate(),
          request.getEndDate());
    } catch (Exception _e) {
      /* ignore logging errors */ }
    var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    var labelsList = new ArrayList<String>();
    var tvList = new ArrayList<Long>();
    var tcList = new ArrayList<Long>();
    var crpList = new ArrayList<Long>();
    var cdList = new ArrayList<Long>();
    var tradeCountList = new ArrayList<Long>();
    var buyCountList = new ArrayList<Long>();
    var dailyRealizedList = new ArrayList<Long>();
    for (var pt : timeSeries) {
      labelsList.add(fmt.format(pt.timestamp()));
      tvList.add(
          pt.totalHoldingsValue() != null
              ? pt.totalHoldingsValue().setScale(0, RoundingMode.HALF_UP).longValue()
              : 0L);
      tcList.add(
          pt.totalHoldingsCost() != null
              ? pt.totalHoldingsCost().setScale(0, RoundingMode.HALF_UP).longValue()
              : 0L);
      crpList.add(
          pt.cumulativeRealizedProfit() != null
              ? pt.cumulativeRealizedProfit().setScale(0, RoundingMode.HALF_UP).longValue()
              : 0L);
      cdList.add(
          pt.cumulativeDividend() != null
              ? pt.cumulativeDividend().setScale(0, RoundingMode.HALF_UP).longValue()
              : 0L);
      tradeCountList.add(pt.tradeCount());
      buyCountList.add(pt.buyCount());
      dailyRealizedList.add(
          pt.dailyRealizedProfit() != null
              ? pt.dailyRealizedProfit().setScale(0, RoundingMode.HALF_UP).longValue()
              : 0L);
    }
    try {
      long sumTv = 0L;
      long sumTc = 0L;
      long sumCrp = 0L;
      for (Long v : tvList) sumTv += v == null ? 0L : v;
      for (Long v : tcList) sumTc += v == null ? 0L : v;
      for (Long v : crpList) sumCrp += v == null ? 0L : v;
      String first = labelsList.isEmpty() ? "" : labelsList.get(0);
      String last = labelsList.isEmpty() ? "" : labelsList.get(labelsList.size() - 1);
      logger.debug(
          "assetGrowthData summary: count={} first={} last={} sumTv={} sumTc={} sumCrp={}",
          labelsList.size(),
          first,
          last,
          sumTv,
          sumTc,
          sumCrp);
    } catch (Exception _e) {
      /* ignore */ }
    return Map.of(
        "labels", labelsList,
        "totalValueData", tvList,
        "totalCostData", tcList,
        "cumulativeRealizedProfitData", crpList,
        "cumulativeDividendData", cdList,
        "tradeCountData", tradeCountList,
        "buyCountData", buyCountList,
        "dailyRealizedProfitData", dailyRealizedList);
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
    return "stock/htmx/fragments/assetGrowthPeriodReturnSummary";
  }

  @GetMapping("/holdings-snapshot")
  public String holdingsSnapshot(@RequestParam(required = false) String date, Model model) {
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
    List<HoldingsSnapshotItem> holdings = tradeProfitClient.holdingsSnapshot(params);
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
    var allFromApi = tradeClient.findTrades(tradeReq.toParams());

    List<StockItem> stockItems = stockItemClient.getStockItems();
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

    model.addAttribute("trades", pagedTrades);
    model.addAttribute("totalItems", totalItems);
    model.addAttribute("currentPage", currentPage);
    model.addAttribute("totalPages", totalPages);
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
      return new AssetGrowthPeriodReturnSummary(from, to, null, false);
    }

    LocalDate fromDate;
    LocalDate toDate;
    try {
      fromDate = LocalDate.parse(from);
      toDate = LocalDate.parse(to);
    } catch (Exception ex) {
      logger.warn("Failed to parse asset growth period range: from={} to={}", from, to, ex);
      return new AssetGrowthPeriodReturnSummary(from, to, null, false);
    }

    if (toDate.isBefore(fromDate)) {
      return new AssetGrowthPeriodReturnSummary(
          fromDate.toString(), toDate.toString(), null, false);
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
          fromDate.toString(), toDate.toString(), periodReturnRatePct, periodReturnRatePct != null);
    } catch (Exception ex) {
      logger.warn(
          "Failed to build asset growth period return summary: from={} to={} timeZone={}",
          from,
          to,
          timeZone,
          ex);
      return new AssetGrowthPeriodReturnSummary(
          fromDate.toString(), toDate.toString(), null, false);
    }
  }

  private HoldingsValueWindow loadHoldingsValueWindow(
      UUID userId, LocalDate fromDate, LocalDate toDate, String timeZone, ZoneId zone) {
    TradeProfitRequest request = new TradeProfitRequest();
    request.setUserId(userId);
    request.setStartDate(fromDate.atStartOfDay(zone).toInstant());
    request.setEndDate(toDate.atStartOfDay(zone).toInstant());
    request.setTimeZone(timeZone);

    var params = request.toParams();
    params.add("granularity", "DAILY");

    List<TradeProfitTimeSeriesPoint> series = tradeProfitClient.timeSeries(params);
    if (series == null || series.isEmpty()) {
      return new HoldingsValueWindow(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    BigDecimal openingValue = BigDecimal.ZERO;
    BigDecimal closingValue = BigDecimal.ZERO;
    LocalDate openingDate = fromDate;

    for (TradeProfitTimeSeriesPoint point : series) {
      if (point == null || point.timestamp() == null) {
        continue;
      }
      LocalDate pointDate = point.timestamp().atZone(zone).toLocalDate();
      BigDecimal holdingsValue =
          point.totalHoldingsValue() != null ? point.totalHoldingsValue() : BigDecimal.ZERO;
      if (pointDate.equals(openingDate)) {
        openingValue = holdingsValue;
      }
      if (!pointDate.isAfter(toDate)) {
        closingValue = holdingsValue;
      }
    }

    return new HoldingsValueWindow(openingValue, closingValue);
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

  private List<PeriodCashFlow> buildPeriodCashFlows(
      UUID userId, Instant periodStart, Instant periodEndExclusive) {
    List<PeriodCashFlow> cashFlows = new ArrayList<>();

    TradeSearchRequest tradeRequest =
        new TradeSearchRequest(userId, null, null, periodStart, periodEndExclusive);
    List<TradeResponse> trades = tradeClient.findTrades(tradeRequest.toParams());
    for (TradeResponse trade : trades) {
      BigDecimal amount = resolveTradeAmount(trade);
      BigDecimal fee = trade.fee() != null ? trade.fee() : BigDecimal.ZERO;
      BigDecimal tax = trade.tax() != null ? trade.tax() : BigDecimal.ZERO;
      Instant occurredAt = clampInstant(trade.tradeDate(), periodStart, periodEndExclusive);

      if (trade.type() == TradeType.BUY) {
        BigDecimal contribution = amount.add(fee);
        if (contribution.compareTo(BigDecimal.ZERO) != 0) {
          cashFlows.add(new PeriodCashFlow(occurredAt, contribution));
        }
      } else if (trade.type() == TradeType.SELL) {
        BigDecimal withdrawal = amount.subtract(fee).subtract(tax);
        if (withdrawal.compareTo(BigDecimal.ZERO) != 0) {
          cashFlows.add(new PeriodCashFlow(occurredAt, withdrawal.negate()));
        }
      }
    }

    DividendRequest dividendRequest = new DividendRequest();
    dividendRequest.setUserId(userId);
    dividendRequest.setStartDate(periodStart);
    dividendRequest.setEndDate(periodEndExclusive);
    List<DividendResponse> dividends = dividendClient.findDividends(dividendRequest.toParams());
    for (DividendResponse dividend : dividends) {
      BigDecimal netAmount = resolveDividendNetAmount(dividend);
      if (netAmount.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      cashFlows.add(
          new PeriodCashFlow(
              clampInstant(dividend.payDate(), periodStart, periodEndExclusive),
              netAmount.negate()));
    }

    cashFlows.sort(Comparator.comparing(PeriodCashFlow::occurredAt));
    return cashFlows;
  }

  private Double calculateModifiedDietzReturnRate(
      BigDecimal openingValue,
      BigDecimal closingValue,
      List<PeriodCashFlow> cashFlows,
      Instant periodStart,
      Instant periodEndExclusive) {
    if (openingValue == null || closingValue == null) {
      return null;
    }

    BigDecimal netCashFlow = BigDecimal.ZERO;
    BigDecimal weightedCashFlow = BigDecimal.ZERO;
    double totalMillis =
        Math.max(1d, periodEndExclusive.toEpochMilli() - periodStart.toEpochMilli());

    for (PeriodCashFlow cashFlow : cashFlows) {
      BigDecimal amount = cashFlow.amount() != null ? cashFlow.amount() : BigDecimal.ZERO;
      netCashFlow = netCashFlow.add(amount);

      double elapsedMillis = cashFlow.occurredAt().toEpochMilli() - periodStart.toEpochMilli();
      double weight = 1d - Math.max(0d, Math.min(1d, elapsedMillis / totalMillis));
      weightedCashFlow = weightedCashFlow.add(amount.multiply(BigDecimal.valueOf(weight)));
    }

    BigDecimal denominator = openingValue.add(weightedCashFlow);
    if (denominator.abs().compareTo(new BigDecimal("0.000001")) <= 0) {
      return null;
    }

    BigDecimal numerator = closingValue.subtract(openingValue).subtract(netCashFlow);
    return numerator.divide(denominator, 8, RoundingMode.HALF_UP).doubleValue();
  }

  private BigDecimal resolveTradeAmount(TradeResponse trade) {
    if (trade.amount() != null) {
      return trade.amount();
    }
    if (trade.price() != null) {
      return trade.price().multiply(BigDecimal.valueOf(trade.quantity()));
    }
    return BigDecimal.ZERO;
  }

  private BigDecimal resolveDividendNetAmount(DividendResponse dividend) {
    if (dividend.netAmount() != null) {
      return dividend.netAmount();
    }
    BigDecimal grossAmount =
        dividend.grossAmount() != null ? dividend.grossAmount() : BigDecimal.ZERO;
    BigDecimal fee = dividend.fee() != null ? dividend.fee() : BigDecimal.ZERO;
    BigDecimal tax = dividend.tax() != null ? dividend.tax() : BigDecimal.ZERO;
    return grossAmount.subtract(fee).subtract(tax);
  }

  private Instant clampInstant(Instant instant, Instant min, Instant max) {
    if (instant == null || instant.isBefore(min)) {
      return min;
    }
    if (instant.isAfter(max)) {
      return max;
    }
    return instant;
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

  private record HoldingsValueWindow(BigDecimal openingValue, BigDecimal closingValue) {}

  private record PeriodCashFlow(Instant occurredAt, BigDecimal amount) {}

  private record AssetGrowthPeriodReturnSummary(
      String fromDate, String toDate, Double periodReturnRatePct, boolean returnCalculable) {}
}
