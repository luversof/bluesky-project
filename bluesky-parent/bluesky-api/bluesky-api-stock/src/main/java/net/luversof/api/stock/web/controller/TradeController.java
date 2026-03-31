package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeSearchRequest;
import net.luversof.api.stock.web.dto.response.TradeResponse;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired private TradeProfitService tradeProfitService;

    @GetMapping
    public List<TradeResponse> findTrades(TradeSearchRequest request) {
        return tradeProfitService.getTradeHistory(request);
    }
}
