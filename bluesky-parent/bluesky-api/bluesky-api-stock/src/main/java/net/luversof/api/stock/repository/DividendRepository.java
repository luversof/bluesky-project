package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItemDateRange;

public interface DividendRepository extends CrudRepository<Dividend, UUID> {

  /**
   * 연도별 배당 집계. <b>지급일</b> 기준으로 해에 넣는다 - 원천징수가 그 때 일어나므로 세금 관점에서는 지급일이 맞다.
   *
   * <p>합계를 DB 에서 낸다(실측 2026-09-01: 배당 194 행 78.4 KB 를 요약 몇 줄 만들자고 실어 보낼 이유가 없다).
   */
  @Query(
      """
                SELECT EXTRACT(YEAR FROM (d."payDate" AT TIME ZONE :zone))::int AS year,
                       COALESCE(SUM(d."grossAmount"), 0)   AS gross_amount,
                       COALESCE(SUM(d."taxableAmount"), 0) AS taxable_amount,
                       COALESCE(SUM(d."tax"), 0)           AS tax,
                       COALESCE(SUM(d."fee"), 0)           AS fee
                FROM "Dividend" d
                JOIN "Account" a ON d."account_id" = a."id"
                WHERE a."user_id" = :userId
                  AND d."payDate" IS NOT NULL
                  AND (CAST(:startDate AS timestamptz) IS NULL OR d."payDate" >= CAST(:startDate AS timestamptz))
                  AND (CAST(:endDate   AS timestamptz) IS NULL OR d."payDate" <  CAST(:endDate   AS timestamptz))
                GROUP BY 1
                ORDER BY 1
            """)
  java.util.List<net.luversof.api.stock.domain.YearlyDividendIncome> findYearlyIncome(
      @org.springframework.data.repository.query.Param("userId") java.util.UUID userId,
      @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
      @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate,
      @org.springframework.data.repository.query.Param("zone") String zone);

  /** 사용자의 최초 배당 지급일. 전체 배당을 내려받아 min() 하는 대신 DB 집계로 1행만 가져온다. */
  @Query(
      """
				SELECT MIN(d."payDate")
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId AND d."payDate" IS NOT NULL
			""")
  Instant findFirstDividendDateByUserId(UUID userId);

  /** 사용자의 마지막 배당 지급일. 데이터 최신 시점 표시용(집계 1건). */
  @Query(
      """
				SELECT MAX(d."payDate")
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId AND d."payDate" IS NOT NULL
			""")
  Instant findLastDividendDateByUserId(UUID userId);

  /** 기간 내 배당이 있는 계좌 id (필터 목록용). */
  @Query(
      """
				SELECT DISTINCT d."account_id"
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId
					AND (CAST(:startDate AS timestamptz) IS NULL OR COALESCE(d."payDate", d."recordDate") >= :startDate)
					AND (CAST(:endDate AS timestamptz) IS NULL OR COALESCE(d."payDate", d."recordDate") < :endDate)
			""")
  List<UUID> findDistinctAccountIds(UUID userId, Instant startDate, Instant endDate);

  /** 기간 내 배당이 있는 종목 id (필터 목록용). */
  @Query(
      """
				SELECT DISTINCT d."stockItem_id"
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId
					AND (CAST(:startDate AS timestamptz) IS NULL OR COALESCE(d."payDate", d."recordDate") >= :startDate)
					AND (CAST(:endDate AS timestamptz) IS NULL OR COALESCE(d."payDate", d."recordDate") < :endDate)
			""")
  List<UUID> findDistinctStockItemIds(UUID userId, Instant startDate, Instant endDate);

  /** 사용자의 배당 건수. */
  @Query(
      """
				SELECT COUNT(*)
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
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
				SELECT MAX(d."payDate") AS last_date, COUNT(*) AS total_count
				FROM "Dividend" d
				JOIN "Account" a ON d."account_id" = a."id"
				WHERE a."user_id" = :userId
			""")
  UserLedgerSummary findLedgerSummaryByUserId(UUID userId);

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
