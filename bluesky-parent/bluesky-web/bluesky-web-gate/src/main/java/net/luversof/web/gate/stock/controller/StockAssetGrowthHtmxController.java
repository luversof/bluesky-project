package net.luversof.web.gate.stock.controller;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockAssetGrowthHtmxController extends StockBaseHtmxController {
  public StockAssetGrowthHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient) {
    super(tradeProfitClient, tradeClient, accountClient, stockItemClient, dividendClient);
  }

  @BlueskyPreAuthorize
  @GetMapping("/asset-growth/view")
  public String assetGrowthView(TradeProfitRequest request, Model model) {
    var userId = UserUtil.getUserId();
    if (userId == null) {
      return ERROR_VIEW;
    }

    request.setUserId(userId);

    var params = request.toParams();
    params.add("granularity", "AUTO");
    List<TradeProfitTimeSeriesPoint> timeSeries = tradeProfitClient.timeSeries(params);

    model.addAttribute("timeSeries", timeSeries);
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
      request.setStartDate(LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
    if (to != null) {
      request.setEndDate(LocalDate.parse(to).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
    var params = request.toParams();
    params.add("granularity", gran != null ? gran : "AUTO");
    var timeSeries = tradeProfitClient.timeSeries(params);
    var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    var labelsList = new ArrayList<String>();
    var tvList = new ArrayList<Long>();
    var tcList = new ArrayList<Long>();
    var crpList = new ArrayList<Long>();
    var cdList = new ArrayList<Long>();
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
    }
    return Map.of(
        "labels", labelsList,
        "totalValueData", tvList,
        "totalCostData", tcList,
        "cumulativeRealizedProfitData", crpList,
        "cumulativeDividendData", cdList);
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
}
