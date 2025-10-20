package net.luversof.web.gate.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.Setter;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.openfeign.TradeProfitClient;

@Controller
@RequestMapping(value = "/stock/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class StockHtmxController {

	@Setter(onMethod_ = @Autowired)
	private TradeProfitClient tradeProfitClient;

	@PostMapping("/calculateProfit")
	public String calculateProfit(@RequestBody TradeProfitRequest request, Model model) {

		List<TradeProfit> tradeProfitList = tradeProfitClient.calculateProfit(request);
		model.addAttribute("tradeProfitList", tradeProfitList);
		return "stock/htmx/calculateProfit";
	}

}
