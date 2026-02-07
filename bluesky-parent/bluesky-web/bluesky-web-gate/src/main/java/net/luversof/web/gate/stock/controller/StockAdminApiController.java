package net.luversof.web.gate.stock.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.httpexchange.StockAdminClient;

@RestController
@RequestMapping("/api/stock/admin")
public class StockAdminApiController {

	@Autowired
	private StockAdminClient stockAdminClient;

	@PostMapping("/stock-items")
	public int stockItemBulkInsert() {
		return stockAdminClient.stockItemBulkInsert(UserUtil.getUserId());
	}

	@PostMapping("/trades")
	public void tradeBulkInsert() {
		stockAdminClient.tradeBulkInsert(UserUtil.getUserId());
	}

	@PostMapping("/dividends")
	public void dividendBulkInsert() {
		stockAdminClient.dividendBulkInsert(UserUtil.getUserId());
	}
}
