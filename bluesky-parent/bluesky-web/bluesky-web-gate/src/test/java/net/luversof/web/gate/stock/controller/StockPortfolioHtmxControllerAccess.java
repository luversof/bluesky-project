package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.AssetStatusStockAccountView;

/** 뷰 검사가 컨트롤러의 패키지 전용 조립 메서드를 부르게 해 주는 창구(테스트 전용). */
public final class StockPortfolioHtmxControllerAccess {

  private StockPortfolioHtmxControllerAccess() {}

  public static Map<UUID, List<AssetStatusStockAccountView>> buildStockHoldingAccountViews(
      List<TradeProfit> holdings, BigDecimal totalEvaluationAmount) {
    return new StockPortfolioHtmxController(null, null, null, null, null, null, null, null)
        .buildStockHoldingAccountViews(holdings, totalEvaluationAmount);
  }
}
