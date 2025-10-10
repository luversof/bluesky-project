package net.luversof.api.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.stock.service.TradeProfitService;

@RestController
@RequestMapping("/api/tradeProfit")
public class TradeProfitController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitService stockProfitService;
	
}
