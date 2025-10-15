package net.luversof.web.gate.stock.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.luversof.web.gate.stock.domain.StockItem;

@FeignClient(name = "bluesky-api-stock", contextId="api-stock-stockitem", path = "/api/stockItem", url = "${gate.feign-client.url.stock:}")
public interface StockItemClient {

	@PostMapping
	StockItem createStockItem(@RequestBody StockItem stockItem);
	
	@GetMapping("/search/findByName/{name}")
	StockItem findByName(@PathVariable String name);

}
