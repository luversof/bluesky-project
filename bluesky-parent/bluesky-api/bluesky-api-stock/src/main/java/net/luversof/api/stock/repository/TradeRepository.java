package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemDateRange;
import net.luversof.api.stock.domain.Trade;

public interface TradeRepository extends CrudRepository<Trade, UUID> {

  /** 사용자의 최초 거래일. 날짜 선택기의 하한(minDate) 계산용으로, 전체 거래를 내려받아 min() 하는 대신 DB 집계로 1행만 가져온다. */
  @Query(
      """
				SELECT MIN(t."tradeDate")
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId AND t."tradeDate" IS NOT NULL
			""")
  Instant findFirstTradeDateByUserId(UUID userId);

  @Query(
      """
				SELECT "stockItem_id" AS stock_item_id, MIN("tradeDate") AS min_date, MAX("tradeDate") AS max_date
				FROM "Trade"
				WHERE "stockItem_id" IS NOT NULL AND "tradeDate" IS NOT NULL
				GROUP BY "stockItem_id"
			""")
  List<StockItemDateRange> findTradeDateRanges();

  @Query(
      """
                                SELECT "stockItem_id"
                                FROM "Trade"
                                WHERE "stockItem_id" IS NOT NULL
                                GROUP BY "stockItem_id"
                                HAVING SUM(CASE WHEN "type" = 'BUY' THEN "quantity" ELSE -"quantity" END) > 0
                        """)
  List<UUID> findCurrentlyHeldStockItemIds();

  List<Trade> findByAccountId(UUID accountId);

  List<Trade> findByAccountIdIn(List<UUID> accountIdList);

  List<Trade> findByAccountIdInAndTradeDateBetween(
      List<UUID> accountIdList, Instant startDate, Instant endDate);

  List<Trade> findByAccountIdInAndStockItemIdIn(
      List<UUID> accountIdList, List<UUID> stockItemIdList);

  List<Trade> findByAccountIdInAndStockItemIdInAndTradeDateBetween(
      List<UUID> accountIdList, List<UUID> stockItemIdList, Instant startDate, Instant endDate);

  long deleteByAccountId(UUID accountId);
}
