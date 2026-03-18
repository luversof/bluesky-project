package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemDateRange;
import net.luversof.api.stock.domain.Trade;

public interface TradeRepository extends CrudRepository<Trade, UUID> {

	@Query("""
			SELECT "stockItem_id" AS stock_item_id, MIN("tradeDate") AS min_date, MAX("tradeDate") AS max_date
				WHERE "stockItem_id" IS NOT NULL AND "tradeDate" IS NOT NULL
				GROUP BY "stockItem_id"
			""")
	List<StockItemDateRange> findTradeDateRanges();

	List<Trade> findByAccountId(UUID accountId);

	List<Trade> findByAccountIdIn(List<UUID> accountIdList);

	List<Trade> findByAccountIdInAndTradeDateBetween(List<UUID> accountIdList, Instant startDate, Instant endDate);

	List<Trade> findByAccountIdInAndStockItemIdIn(List<UUID> accountIdList, List<UUID> stockItemIdList);

	List<Trade> findByAccountIdInAndStockItemIdInAndTradeDateBetween(List<UUID> accountIdList,
			List<UUID> stockItemIdList, Instant startDate, Instant endDate);

	long deleteByAccountId(UUID accountId);

}
