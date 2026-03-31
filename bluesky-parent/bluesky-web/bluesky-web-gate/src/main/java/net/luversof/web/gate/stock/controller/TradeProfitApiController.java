package net.luversof.web.gate.stock.controller;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import java.util.List;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/tradeProfit")
public class TradeProfitApiController {

    private TradeProfitClient tradeProfitClient;

    @Autowired
    public void setTradeProfitClient(TradeProfitClient tradeProfitClient) {
        this.tradeProfitClient = tradeProfitClient;
    }

    @BlueskyPreAuthorize
    @GetMapping("/calculateProfit")
    public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
        return tradeProfitClient.calculateProfit(request.toParams());
    }
}
