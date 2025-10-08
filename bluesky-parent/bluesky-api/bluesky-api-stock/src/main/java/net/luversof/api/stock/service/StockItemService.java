package net.luversof.api.stock.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.StockItemRepository;

@Service
public class StockItemService {

	@Setter(onMethod_ = @Autowired)
	private StockItemRepository stockItemRepository;
	
	public StockItem createStockItem(StockItem stockItem) {
		return stockItemRepository.save(stockItem);
	}
	
	public StockItem findByName(String name) {
		return stockItemRepository.findByName(name);
	}

}
