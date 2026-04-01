package net.luversof.api.stock.service.strategy;

import java.util.List;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.StockPriceService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

public interface ProfitCalculator {
    TradeProfit calculate(
            List<Trade> trades, TradeProfitRequest request, StockPriceService stockPriceService);
}
