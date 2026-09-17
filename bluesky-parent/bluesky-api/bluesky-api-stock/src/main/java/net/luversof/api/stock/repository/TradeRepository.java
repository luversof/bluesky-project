package net.luversof.api.stock.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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

  /**
   * 종목·계좌로 좁힌 최초 매매일. 둘 다 널이면 사용자 전체와 같다.
   *
   * <p>상세 화면의 '가장 이른 기간으로'(«) 는 이 날짜를 알아야 목표 창을 정한다 - 사용자 전체의 최초일을 쓰면 그 종목이 아직 없던 창으로 뛴다(실측
   * 2026-09-13: 삼성전자 최초 매매는 2020-03-23 인데 사용자 전체는 2009-10-06).
   *
   * <p>널 파라미터는 PostgreSQL 이 타입을 못 정하므로 캐스팅해서 비교한다.
   */
  @Query(
      """
				SELECT MIN(t."tradeDate")
				FROM "Trade" t
				JOIN "Account" a ON t."account_id" = a."id"
				WHERE a."user_id" = :userId AND t."tradeDate" IS NOT NULL
					AND (CAST(:stockItemId AS uuid) IS NULL OR t."stockItem_id" = :stockItemId)
					AND (CAST(:accountId AS uuid) IS NULL OR t."account_id" = :accountId)
			""")
  Instant findFirstTradeDate(UUID userId, UUID stockItemId, UUID accountId);

  /**
   * 연도별 매매 비용·실현손익.
   *
   * <p>합계를 DB 에서 낸다 - 화면이 원장을 통째로 받아 더하면 응답이 원장 크기를 따라간다(실측 2026-09-01: 거래 251 행 80.7 KB).
   *
   * <p>해를 가르는 존을 인자로 받는다. 세금은 그 나라 기준이라 서버 존에 좌우되면 안 된다.
   */
  @Query(
      """
                SELECT EXTRACT(YEAR FROM (t."tradeDate" AT TIME ZONE :zone))::int AS year,
                       COALESCE(SUM(t."fee"), 0)            AS fee,
                       COALESCE(SUM(t."tax"), 0)            AS tax,
                       COALESCE(SUM(t."realizedProfit"), 0) AS realized_profit,
                       COUNT(*) FILTER (WHERE t."type" = 'SELL') AS sell_count
                FROM "Trade" t
                JOIN "Account" a ON t."account_id" = a."id"
                WHERE a."user_id" = :userId
                  AND t."tradeDate" IS NOT NULL
                  AND (CAST(:startDate AS timestamptz) IS NULL OR t."tradeDate" >= CAST(:startDate AS timestamptz))
                  AND (CAST(:endDate   AS timestamptz) IS NULL OR t."tradeDate" <  CAST(:endDate   AS timestamptz))
                  AND (CAST(:accountIds AS text) IS NULL OR t."account_id" = ANY(string_to_array(:accountIds, ',')::uuid[]))
                  AND (CAST(:stockItemIds AS text) IS NULL OR t."stockItem_id" = ANY(string_to_array(:stockItemIds, ',')::uuid[]))
                GROUP BY 1
                ORDER BY 1
            """)
  List<net.luversof.api.stock.domain.YearlyTradeCost> findYearlyCost(
      @Param("userId") UUID userId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      @Param("zone") String zone,
      @Param("accountIds") String accountIds,
      @Param("stockItemIds") String stockItemIds);

  /**
   * 기간 매매 집계 한 줄.
   *
   * <p>대시보드가 기간 수치를 보여 주려면 지금은 원장을 통째로 받아 더해야 한다(실측 2026-09-10: 올해 258 행). 연도별 집계와 같은 방식으로 DB 에서
   * 합계만 낸다. 매수/매도 금액은 단가 x 수량이다(수수료·세금은 따로 낸다).
   */
  @Query(
      """
                SELECT COALESCE(SUM(CASE WHEN t."type" = 'BUY'  THEN t."price" * t."quantity" ELSE 0 END), 0) AS buy_amount,
                       COALESCE(SUM(CASE WHEN t."type" = 'SELL' THEN t."price" * t."quantity" ELSE 0 END), 0) AS sell_amount,
                       COALESCE(SUM(t."fee"), 0)            AS fee,
                       COALESCE(SUM(t."tax"), 0)            AS tax,
                       COALESCE(SUM(t."realizedProfit"), 0) AS realized_profit,
                       COUNT(*) FILTER (WHERE t."type" = 'BUY')  AS buy_count,
                       COUNT(*) FILTER (WHERE t."type" = 'SELL') AS sell_count
                FROM "Trade" t
                JOIN "Account" a ON t."account_id" = a."id"
                WHERE a."user_id" = :userId
                  AND t."tradeDate" IS NOT NULL
                  AND (CAST(:startDate AS timestamptz) IS NULL OR t."tradeDate" >= CAST(:startDate AS timestamptz))
                  AND (CAST(:endDate   AS timestamptz) IS NULL OR t."tradeDate" <  CAST(:endDate   AS timestamptz))
            """)
  net.luversof.api.stock.domain.PeriodTradeSummary findPeriodSummary(
      @Param("userId") UUID userId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

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

  /**
   * 사용자의 종목별 최초 매수 시점.
   *
   * <p>자산 현황이 "이 종목을 얼마나 오래 들고 있나"를 적으려면 종목마다 처음 산 날이 필요하다. 원장 전체(실측 2026-09-14: 258 행)를 받아 화면에서
   * 종목마다 min() 하는 대신 DB 집계로 보유 종목 수만큼만 가져온다.
   *
   * <p>매도는 세지 않는다 &mdash; 보유 기간의 시작은 처음 산 날이다. 판 뒤 다시 산 종목도 최초 매수일을 쓴다(그 종목을 알게 된 시점이 기준).
   *
   * <p>계좌 목록은 쉼표로 이어 한 문자열로 받는다 &mdash; {@code IN (:list)} 는 빈 목록에서 {@code IN ()} 이 되어 문법 오류가 나므로 이
   * 저장소가 이미 쓰는 {@code string_to_array(...)::uuid[]} 방식을 따른다. 널 파라미터는 PostgreSQL 이 타입을 못 정하므로 캐스팅해서
   * 비교한다.
   */
  @Query(
      """
                SELECT t."stockItem_id" AS stock_item_id, MIN(t."tradeDate") AS first_buy_date
                FROM "Trade" t
                JOIN "Account" a ON t."account_id" = a."id"
                WHERE a."user_id" = :userId
                  AND t."tradeDate" IS NOT NULL
                  AND t."stockItem_id" IS NOT NULL
                  AND t."type" = 'BUY'
                  AND (CAST(:accountIds AS text) IS NULL OR t."account_id" = ANY(string_to_array(:accountIds, ',')::uuid[]))
                GROUP BY t."stockItem_id"
            """)
  List<net.luversof.api.stock.domain.StockItemFirstBuy> findFirstBuyDateByStockItem(
      @Param("userId") UUID userId, @Param("accountIds") String accountIds);

  /**
   * 종목별로 실제로 오간 돈(매수 · 매도 · 배당) &mdash; 자산 현황 · 종목 상세의 연평균 수익률(XIRR) 재료.
   *
   * <p>금액은 손익 계산과 같은 재료다. 매매 금액은 가격 x 수량(amount 열은 없다)이고, 매수는 수수료 · 세금을 더해 나가고 매도는 빼고 들어온다. 배당은 종목별
   * 배당 합계(/api/dividend/totalByStockItem)와 같은 세전 - 세금 - 수수료다. 실측 2026-09-17: 보유 9 종목 모두 이 흐름의 합에 오늘
   * 평가액을 더한 값이 평가손익(Net) + 실현손익(Net) + 배당 합계와 0 원 차이였다.
   *
   * <p>필터 문자열 규칙(쉼표로 이은 id, 널이면 좁히지 않음)은 {@link #findFirstBuyDateByStockItem} 과 같다.
   */
  @Query(
      """
                SELECT f.stock_item_id, f.flow_at, f.amount
                FROM (
                  SELECT t."stockItem_id" AS stock_item_id, t."tradeDate" AS flow_at,
                         CASE WHEN t."type" = 'BUY'
                              THEN -(COALESCE(t."price", 0) * t."quantity" + COALESCE(t."fee", 0) + COALESCE(t."tax", 0))
                              ELSE COALESCE(t."price", 0) * t."quantity" - COALESCE(t."fee", 0) - COALESCE(t."tax", 0)
                         END AS amount
                  FROM "Trade" t
                  JOIN "Account" a ON t."account_id" = a."id"
                  WHERE a."user_id" = :userId
                    AND t."tradeDate" IS NOT NULL
                    AND t."stockItem_id" IS NOT NULL
                    AND t."type" IN ('BUY', 'SELL')
                    AND (CAST(:accountIds AS text) IS NULL OR t."account_id" = ANY(string_to_array(:accountIds, ',')::uuid[]))
                    AND (CAST(:stockItemIds AS text) IS NULL OR t."stockItem_id" = ANY(string_to_array(:stockItemIds, ',')::uuid[]))
                  UNION ALL
                  SELECT d."stockItem_id" AS stock_item_id, d."payDate" AS flow_at,
                         COALESCE(d."grossAmount", 0) - COALESCE(d."tax", 0) - COALESCE(d."fee", 0) AS amount
                  FROM "Dividend" d
                  JOIN "Account" a ON d."account_id" = a."id"
                  WHERE a."user_id" = :userId
                    AND d."payDate" IS NOT NULL
                    AND d."stockItem_id" IS NOT NULL
                    AND (CAST(:accountIds AS text) IS NULL OR d."account_id" = ANY(string_to_array(:accountIds, ',')::uuid[]))
                    AND (CAST(:stockItemIds AS text) IS NULL OR d."stockItem_id" = ANY(string_to_array(:stockItemIds, ',')::uuid[]))
                ) f
                ORDER BY f.stock_item_id, f.flow_at
            """)
  List<net.luversof.api.stock.domain.StockItemCashFlow> findCashFlowsByStockItem(
      @Param("userId") UUID userId,
      @Param("accountIds") String accountIds,
      @Param("stockItemIds") String stockItemIds);

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
