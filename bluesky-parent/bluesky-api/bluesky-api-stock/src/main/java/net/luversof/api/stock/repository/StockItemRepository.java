package net.luversof.api.stock.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItem;

public interface StockItemRepository extends CrudRepository<StockItem, UUID> {

  StockItem findByName(String name);

  StockItem findBySymbol(String symbol);
}
