package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.response.HoldingsSnapshotItem;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

@RestController
@RequestMapping("/api/tradeProfit")
public class TradeProfitController {

  @Autowired private TradeProfitService stockProfitService;

  public void setStockProfitService(TradeProfitService stockProfitService) {
    this.stockProfitService = stockProfitService;
  }

  @GetMapping("/calculateProfit")
  public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
    return stockProfitService.calculateProfit(request);
  }

  @GetMapping("/timeSeries")
  public List<TradeProfitTimeSeriesPoint> timeSeries(
      TradeProfitRequest request, String granularity) {
    // Delegate to service-level efficient aggregation
    return stockProfitService.aggregateTimeSeries(request, granularity);
  }

  @GetMapping("/holdingsSnapshot")
  public List<HoldingsSnapshotItem> holdingsSnapshot(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) UUID accountId) {
    return stockProfitService.getHoldingsSnapshot(userId, date, accountId);
  }
}
