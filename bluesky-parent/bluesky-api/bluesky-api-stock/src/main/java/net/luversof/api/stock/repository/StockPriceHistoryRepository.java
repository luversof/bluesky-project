package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockPriceHistory;

public interface StockPriceHistoryRepository extends CrudRepository<StockPriceHistory, UUID> {

    Optional<StockPriceHistory> findTopByStockItemIdAndPriceDateLessThanEqualOrderByPriceDateDesc(UUID stockItemId, Instant priceDate);

}
