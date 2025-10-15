package net.luversof.web.gate.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.openfeign.TradeProfitClient;

@RestController
@RequestMapping("/api/stock/tradeProfit")
public class TradeProfitGateController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitClient tradeProfitClient;
	
	@GetMapping("/calculateProfit")
	public List<TradeProfit> calculateProfit(TradeProfitRequest request) {
		return tradeProfitClient.calculateProfit(request);
	}
	
}