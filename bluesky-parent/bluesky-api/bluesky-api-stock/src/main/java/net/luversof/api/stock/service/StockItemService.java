package net.luversof.api.stock.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.StockItemRepository;

@Service
public class StockItemService {

	@Autowired
	private StockItemRepository stockItemRepository;

	public void setStockItemRepository(StockItemRepository stockItemRepository) {
		this.stockItemRepository = stockItemRepository;
	}

	public StockItem createStockItem(StockItem stockItem) {
		return stockItemRepository.save(stockItem);
	}

	public Optional<StockItem> findById(UUID id) {
		return stockItemRepository.findById(id);
	}

	public StockItem findByName(String name) {
		return stockItemRepository.findByName(name);
	}

	public java.util.List<StockItem> findAll() {
		java.util.List<StockItem> list = new java.util.ArrayList<>();
		stockItemRepository.findAll().forEach(list::add);
		return list;
	}

}
