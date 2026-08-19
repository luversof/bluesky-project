package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * 여러 날짜의 보유 스냅샷을 한 번에 조회한다.
   *
   * <p>호출자가 날짜마다 /holdingsSnapshot 을 순차 호출하면 날짜 수만큼 왕복이 발생한다(배당 분석은 기준일이 수십 개라 수 초가 걸렸다). 날짜별 결과를
   * ISO 날짜 문자열을 키로 하는 맵으로 한 번에 돌려준다.
   */
  @GetMapping("/holdingsSnapshotBatch")
  public Map<String, List<HoldingsSnapshotItem>> holdingsSnapshotBatch(
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> dates,
      @RequestParam(required = false) UUID accountId) {
    Map<String, List<HoldingsSnapshotItem>> result = new LinkedHashMap<>();
    if (dates == null) {
      return result;
    }
    // 날짜마다 조회하면 날짜 수만큼 시뮬레이션이 돈다. 한 번의 시뮬레이션에서 모두 캡처한다.
    stockProfitService
        .getHoldingsSnapshotBatch(userId, dates, accountId)
        .forEach((date, items) -> result.put(date.toString(), items));
    return result;
  }
}
