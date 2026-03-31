package net.luversof.api.stock.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockPrice;

public interface StockPriceRepository extends CrudRepository<StockPrice, UUID> {

    Optional<StockPrice> findByStockItemId(UUID stockItemId);
}
