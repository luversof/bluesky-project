package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.stock.domain.TradeProfit;
import net.luversof.api.stock.service.TradeProfitService;
import net.luversof.api.stock.web.dto.request.TradeProfitRequest;

@RestController
@RequestMapping("/api/tradeProfit")
public class TradeProfitController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitService stockProfitService;
	
	@PostMapping("/calculateProfit")
	public List<TradeProfit> calculateProfit(@RequestBody TradeProfitRequest request) {
		return stockProfitService.calculateProfit(request);
	}
	
}
