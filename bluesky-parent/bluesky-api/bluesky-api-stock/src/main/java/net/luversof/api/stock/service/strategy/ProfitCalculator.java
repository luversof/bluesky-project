package net.luversof.api.stock.service.strategy;

import java.util.List;

import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

public interface ProfitCalculator {
  TradeProfit calculate(
      List<Trade> trades, TradeProfitRequest request, StockPriceService stockPriceService);

  /**
   * 현재가를 미리 일괄 조회해 넘기는 형태. 종목마다 1건씩 조회하면 종목 수만큼 DB 왕복이 생긴다. latestPrices 가 null 이면 기존처럼 개별 조회로
   * 동작한다.
   */
  default TradeProfit calculate(
      List<Trade> trades,
      TradeProfitRequest request,
      StockPriceService stockPriceService,
      java.util.Map<java.util.UUID, net.luversof.api.stock.domain.StockDailyClosePrice>
          latestPrices) {
    return calculate(trades, request, stockPriceService);
  }
}
