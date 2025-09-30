package net.luversof.api.stock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.service.StockItemService;

@Slf4j
class StockItemTest implements GeneralTest {
	
	@Autowired
	StockItemService stockItemService;
	
	@Test
	void createStockItem() {
		var stockItem = new StockItem();
		stockItem.setTicker("161510");
		stockItem.setName("PLUS 고배당주");
		stockItem.setMarket("KOSDAQ");
		
		var result = stockItemService.createStockItem(stockItem);
		log.debug("result : {}", result);
	}

}
