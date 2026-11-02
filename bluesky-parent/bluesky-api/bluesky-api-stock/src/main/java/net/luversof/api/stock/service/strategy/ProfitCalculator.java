package net.luversof.api.stock.service.strategy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

public interface ProfitCalculator {

  /**
   * 현재가를 미리 일괄 조회해 넘기는 형태. 종목마다 1건씩 조회하면 종목 수만큼 DB 왕복이 생긴다. latestPrices 가 null 이면 종목마다 개별 조회로
   * 동작한다. (3 인자 판은 부르는 곳이 없어 2026-10-21 에 지웠다.)
   */
  TradeProfit calculate(
      List<Trade> trades,
      TradeProfitRequest request,
      StockPriceService stockPriceService,
      Map<UUID, StockDailyClosePrice> latestPrices);
}
