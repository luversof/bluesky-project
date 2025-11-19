package net.luversof.web.gate.stock.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/stock", produces = MediaType.TEXT_HTML_VALUE)
public class StockViewController {

	@GetMapping
	public String index() {
		return "stock/index";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "stock/dashboard";
	}

	@GetMapping("/dividend")
	public String dividendPage() {
		return "stock/dividend";
	}
}
