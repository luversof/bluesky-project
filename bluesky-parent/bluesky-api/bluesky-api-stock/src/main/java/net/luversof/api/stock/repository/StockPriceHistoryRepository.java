package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockPriceHistory;

public interface StockPriceHistoryRepository extends CrudRepository<StockPriceHistory, UUID> {

    Optional<StockPriceHistory> findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
            UUID stockItemId, LocalDate tradeDate);

    List<StockPriceHistory> findByStockItemIdAndTradeDateBetween(
            UUID stockItemId, LocalDate start, LocalDate end);

    List<StockPriceHistory> findByStockItemIdInAndTradeDateBetween(
            Collection<UUID> stockItemId, LocalDate start, LocalDate end);

    Optional<StockPriceHistory> findByStockItemIdAndTradeDate(
            UUID stockItemId, LocalDate tradeDate);

    Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateDesc(UUID stockItemId);

    Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateAsc(UUID stockItemId);
}
