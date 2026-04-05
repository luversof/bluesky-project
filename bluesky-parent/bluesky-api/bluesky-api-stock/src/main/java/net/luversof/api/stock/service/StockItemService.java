package net.luversof.api.stock.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.StockItemRepository;

@Service
public class StockItemService {

  @Autowired private StockItemRepository stockItemRepository;

  public void setStockItemRepository(StockItemRepository stockItemRepository) {
    this.stockItemRepository = stockItemRepository;
  }

  @CacheEvict(value = "stockItems", allEntries = true)
  public StockItem createStockItem(StockItem stockItem) {
    return stockItemRepository.save(stockItem);
  }

  @Cacheable(value = "stockItems", key = "#id")
  public Optional<StockItem> findById(UUID id) {
    return stockItemRepository.findById(id);
  }

  @Cacheable(value = "stockItems", key = "#name")
  public StockItem findByName(String name) {
    return stockItemRepository.findByName(name);
  }

  public Iterable<StockItem> findAllById(Iterable<UUID> ids) {
    return stockItemRepository.findAllById(ids);
  }

  public java.util.List<StockItem> findAll() {
    java.util.List<StockItem> list = new java.util.ArrayList<>();
    stockItemRepository.findAll().forEach(list::add);
    return list;
  }
}
