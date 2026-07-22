package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItemDateRange;

public interface DividendRepository extends CrudRepository<Dividend, UUID> {

  /** 사용자의 최초 배당 지급일. 전체 배당을 내려받아 min() 하는 대신 DB 집계로 1행만 가져온다. */
  @Query(
      """
				SELECT MIN(d."payDate")
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId AND d."payDate" IS NOT NULL
			""")
  Instant findFirstDividendDateByUserId(UUID userId);

  /** 사용자의 최초 배당 기준일(지급일·기준일 중 이른 쪽). 게이트 배당 목록의 날짜 하한 계산과 동일한 규칙. */
  @Query(
      """
				SELECT MIN(LEAST(COALESCE(d."recordDate", d."payDate"), COALESCE(d."payDate", d."recordDate")))
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId AND (d."recordDate" IS NOT NULL OR d."payDate" IS NOT NULL)
			""")
  Instant findFirstDividendBasisDateByUserId(UUID userId);

  /** 사용자가 배당을 받은 적 있는 종목 ID 목록. 전체 배당 이력 대신 DISTINCT 만 가져온다. */
  @Query(
      """
				SELECT DISTINCT d."stockItem_id"
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId AND d."stockItem_id" IS NOT NULL
			""")
  List<UUID> findDistinctStockItemIdsByUserId(UUID userId);

  @Query(
      """
				SELECT "stockItem_id" AS stock_item_id,
				MIN(LEAST(COALESCE("recordDate", "payDate"), COALESCE("payDate", "recordDate"))) AS min_date,
				MAX(GREATEST(COALESCE("recordDate", "payDate"), COALESCE("payDate", "recordDate"))) AS max_date
				FROM "Dividend"
				WHERE "stockItem_id" IS NOT NULL AND ("recordDate" IS NOT NULL OR "payDate" IS NOT NULL)
				GROUP BY "stockItem_id"
			""")
  List<StockItemDateRange> findDividendDateRanges();

  long deleteByAccountId(UUID accountId);
}
