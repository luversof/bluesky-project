package net.luversof.api.stock.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItemDateRange;

public interface DividendRepository extends CrudRepository<Dividend, UUID> {

	@Query("""
				SELECT "stockItem_id" AS "stockItemId",
				MIN(LEAST(COALESCE("recordDate", "payDate"), COALESCE("payDate", "recordDate"))) AS "minDate",
				MAX(GREATEST(COALESCE("recordDate", "payDate"), COALESCE("payDate", "recordDate"))) AS "maxDate"
				FROM "Dividend"
				WHERE "stockItem_id" IS NOT NULL AND ("recordDate" IS NOT NULL OR "payDate" IS NOT NULL)
				GROUP BY "stockItem_id"
			""")
	List<StockItemDateRange> findDividendDateRanges();

	long deleteByAccountId(UUID accountId);

}
