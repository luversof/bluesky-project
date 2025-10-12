package net.luversof.api.stock.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.StockItemService;

@RestController
@RequestMapping("/api/stockItem")
public class StockItemController {

	@Setter(onMethod_ = @Autowired)
	private StockItemService stockItemService;
	
	@PostMapping
	public StockItem createStockItem(@RequestBody StockItem stockItem) {
		return stockItemService.createStockItem(stockItem);
	}
	
	@GetMapping("/search/findByName/{name}")
	public StockItem findByName(@PathVariable String name) {
		return stockItemService.findByName(name);
	}

}
