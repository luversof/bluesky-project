package net.luversof.api.stock.web.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.StockAdminService;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminController {

	@Autowired
	private StockAdminService stockAdminService;

	@PostMapping("/stock-items")
	public int stockItemBulkInsert(@RequestParam UUID userId) {
		return stockAdminService.stockItemBulkInsert(userId);
	}

	@PostMapping("/trades")
	public void tradeBulkInsert(@RequestParam UUID userId) {
		stockAdminService.tradeBulkInsert(userId);
	}

	@PostMapping("/dividends")
	public void dividendBulkInsert(@RequestParam UUID userId) {
		stockAdminService.dividendBulkInsert(userId);
	}
}
