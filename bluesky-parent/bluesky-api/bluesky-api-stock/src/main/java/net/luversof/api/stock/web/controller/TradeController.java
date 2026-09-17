package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.StockHoldingPeriodService;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeResponse;
import net.luversof.api.stock.web.support.RequestZoneUtil;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

  @Autowired private TradeProfitService tradeProfitService;

  @Autowired private StockHoldingPeriodService stockHoldingPeriodService;

  public void setStockHoldingPeriodService(StockHoldingPeriodService stockHoldingPeriodService) {
    this.stockHoldingPeriodService = stockHoldingPeriodService;
  }

  @GetMapping
  public List<TradeResponse> findTrades(TradeSearchRequest request) {
    return tradeProfitService.getTradeHistory(request);
  }

  /**
   * 종목별 최초 매수일. 자산 현황의 보유 기간·연평균 수익률이 쓴다.
   *
   * <p>날짜 몇 개 때문에 원장 목록(실측 2026-09-14: 258 행)을 다시 받지 않는다. 존은 형제 엔드포인트와 같은 규칙이다 &mdash; 안 주면 한국 기준,
   * 줬는데 모르는 값이면 400.
   */
  @GetMapping("/firstBuyDateByStockItem")
  public Map<UUID, LocalDate> findFirstBuyDateByStockItem(
      @RequestParam UUID userId,
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) List<UUID> accountIdList) {
    ZoneId zoneId = RequestZoneUtil.parse(timeZone, StockHoldingPeriodService.DEFAULT_ZONE);
    return stockHoldingPeriodService.findFirstBuyDateByStockItem(userId, zoneId, accountIdList);
  }

  /**
   * 종목별 하루치 순현금흐름(매수 · 매도 · 배당). 자산 현황 · 종목 상세의 연평균 수익률(XIRR)이 쓴다.
   *
   * <p>원장 목록(매매 258 행 + 배당)을 다시 받지 않는다. 존 규칙은 최초 매수일과 같다 &mdash; 안 주면 한국 기준, 줬는데 모르는 값이면 400.
   */
  @GetMapping("/cashFlowsByStockItem")
  public Map<UUID, List<net.luversof.api.stock.web.dto.response.StockCashFlowResponse>>
      findCashFlowsByStockItem(
          @RequestParam UUID userId,
          @RequestParam(required = false) String timeZone,
          @RequestParam(required = false) List<UUID> accountIdList,
          @RequestParam(required = false) List<UUID> stockItemIdList) {
    ZoneId zoneId = RequestZoneUtil.parse(timeZone, StockHoldingPeriodService.DEFAULT_ZONE);
    return stockHoldingPeriodService.findCashFlowsByStockItem(
        userId, zoneId, accountIdList, stockItemIdList);
  }
}
