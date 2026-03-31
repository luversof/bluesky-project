package net.luversof.api.stock.web.controller;

import java.util.List;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;
import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
