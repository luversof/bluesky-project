package net.luversof.api.stock.web.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.StockItemService;

@RestController
@RequestMapping("/api/stockItem")
public class StockItemController {

	@Autowired
	private StockItemService stockItemService;

	public void setStockItemService(StockItemService stockItemService) {
		this.stockItemService = stockItemService;
	}

	@PostMapping
	public StockItem createStockItem(@RequestBody StockItem stockItem) {
		return stockItemService.createStockItem(stockItem);
	}

	@GetMapping("/{id}")
	public Optional<StockItem> getStockItemById(@PathVariable UUID id) {
		return stockItemService.findById(id);
	}

	@GetMapping("/search/findByName/{name}")
	public StockItem findByName(@PathVariable String name) {
		return stockItemService.findByName(name);
	}

	@GetMapping("/search/findAll")
	public java.util.List<net.luversof.api.stock.domain.StockItem> findAll() {
		return stockItemService.findAll();
	}

}
