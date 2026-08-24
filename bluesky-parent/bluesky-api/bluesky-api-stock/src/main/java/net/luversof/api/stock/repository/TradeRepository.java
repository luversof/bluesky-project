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

  /** 사용자의 마지막 거래일. 데이터 최신 시점 표시용(집계 1건). */
  @Query(
      """
				SELECT MAX(t."tradeDate")
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId AND t."tradeDate" IS NOT NULL
			""")
  Instant findLastTradeDateByUserId(UUID userId);

  /** 기간 내 거래가 있는 계좌 id (필터 목록용). 전체 거래를 내려받지 않도록 DISTINCT 로 뽑는다. */
  @Query(
      """
				SELECT DISTINCT t."account_id"
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId
					AND (CAST(:startDate AS timestamptz) IS NULL OR t."tradeDate" >= :startDate)
					AND (CAST(:endDate AS timestamptz) IS NULL OR t."tradeDate" < :endDate)
			""")
  List<UUID> findDistinctAccountIds(UUID userId, Instant startDate, Instant endDate);

  /** 기간 내 거래가 있는 종목 id (필터 목록용). */
  @Query(
      """
				SELECT DISTINCT t."stockItem_id"
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId
					AND (CAST(:startDate AS timestamptz) IS NULL OR t."tradeDate" >= :startDate)
					AND (CAST(:endDate AS timestamptz) IS NULL OR t."tradeDate" < :endDate)
			""")
  List<UUID> findDistinctStockItemIds(UUID userId, Instant startDate, Instant endDate);

  /** 사용자의 거래 건수. */
  @Query(
      """
				SELECT COUNT(*)
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId
			""")
  long countByUserId(UUID userId);

  /**
   * 마지막 일자와 건수를 한 번에 읽는다. 따로 물으면 같은 조인을 두 번 훑는다.
   *
   * <p>{@code MAX} 는 NULL 을 무시하므로 예전 {@code IS NOT NULL} 조건과 결과가 같다.
   */
  @Query(
      """
				SELECT MAX(t."tradeDate") AS last_date, COUNT(*) AS total_count
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId
			""")
  UserLedgerSummary findLedgerSummaryByUserId(UUID userId);

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
