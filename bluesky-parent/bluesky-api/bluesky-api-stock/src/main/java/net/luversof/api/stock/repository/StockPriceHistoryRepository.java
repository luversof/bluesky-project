package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemTradeDate;
import net.luversof.api.stock.domain.StockPriceHistory;

public interface StockPriceHistoryRepository extends CrudRepository<StockPriceHistory, UUID> {

  @Query(
      """
                    SELECT "stockItem_id" AS stock_item_id,
                                 "tradeDate" AS trade_date
                    FROM "StockPriceHistory"
                    WHERE "stockItem_id" IS NOT NULL
                        AND "tradeDate" IS NOT NULL
                        AND "updatedDate" IS NOT NULL
                        AND (
                            "tradeDate" = ("updatedDate" AT TIME ZONE 'Asia/Seoul')::date
                            OR COALESCE("volume", 0) = 0
                        )
                    ORDER BY "stockItem_id", "tradeDate"
                """)
  List<StockItemTradeDate> findRefreshTargetTradeDates();

  Optional<StockPriceHistory> findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
      UUID stockItemId, LocalDate tradeDate);

  List<StockPriceHistory> findByStockItemIdAndTradeDateBetween(
      UUID stockItemId, LocalDate start, LocalDate end);

  List<StockPriceHistory> findByStockItemIdInAndTradeDateBetween(
      Collection<UUID> stockItemId, LocalDate start, LocalDate end);

  Optional<StockPriceHistory> findByStockItemIdAndTradeDate(UUID stockItemId, LocalDate tradeDate);

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateDesc(UUID stockItemId);

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateAsc(UUID stockItemId);
}
