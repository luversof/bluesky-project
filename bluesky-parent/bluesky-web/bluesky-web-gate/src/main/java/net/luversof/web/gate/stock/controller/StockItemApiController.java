package net.luversof.web.gate.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

@RestController
@RequestMapping("/api/stock/stockItem")
public class StockItemApiController {

	@Setter(onMethod_ = @Autowired)
	private StockItemClient stockItemClient;
	
	@PostMapping
	public StockItem createStockItem(@RequestBody StockItem stockItem) {
		return stockItemClient.createStockItem(stockItem);
	}
	
	@GetMapping("/search/findByName/{name}")
	public StockItem findByName(@PathVariable String name) {
		return stockItemClient.findByName(name);
	}

}
