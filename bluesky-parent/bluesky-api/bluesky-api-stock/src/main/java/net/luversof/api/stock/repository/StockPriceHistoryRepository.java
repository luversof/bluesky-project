package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import net.luversof.api.stock.domain.ItemWithoutPriceHistory;
import net.luversof.api.stock.domain.PriceHistoryDuplicateSummary;
import net.luversof.api.stock.domain.PriceHistoryRowCounts;
import net.luversof.api.stock.domain.PriceLimitBreachRow;
import net.luversof.api.stock.domain.StockDailyClosePrice;
import net.luversof.api.stock.domain.StockItemTradeDate;
import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.domain.ZeroVolumeChangedClose;

public interface StockPriceHistoryRepository extends CrudRepository<StockPriceHistory, UUID> {

  // 장중에 저장되어 값이 아직 확정되지 않았을 수 있는 레코드(거래일 == 갱신일)만 재조회 대상으로 본다.
  // 거래량은 장중에도 0이 아닐 수 있어 "거래량 == 0"은 장중/미확정 여부의 올바른 신호가 아니므로 제외한다.
  // (재조회 후 API 값이 기존과 다르면 갱신, 같으면 갱신하지 않는다.)
  @Query(
      """
                    SELECT "stockItem_id" AS stock_item_id,
                                 "tradeDate" AS trade_date
                    FROM "StockPriceHistory"
                    WHERE "stockItem_id" IS NOT NULL
                        AND "tradeDate" IS NOT NULL
                        AND "updatedDate" IS NOT NULL
                        AND "tradeDate" = ("updatedDate" AT TIME ZONE 'Asia/Seoul')::date
                    ORDER BY "stockItem_id", "tradeDate"
                """)
  List<StockItemTradeDate> findRefreshTargetTradeDates();

  Optional<StockPriceHistory> findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
      UUID stockItemId, LocalDate tradeDate);

  List<StockPriceHistory> findByStockItemIdAndTradeDateBetween(
      UUID stockItemId, LocalDate start, LocalDate end);

  /**
   * 종목별 '가장 최근 거래일'의 종가를 한 번에 가져온다. 예전에는 그룹마다 findTopByStockItemIdOrderByTradeDateDesc 로 1건씩 조회해 종목
   * 수만큼 왕복이 발생했다.
   */
  @Query(
      """
                    SELECT i.id  AS stock_item_id,
                                 x."tradeDate"  AS trade_date,
                                 x."closePrice" AS close_price,
                                 x."updatedDate" AS updated_date
                    FROM unnest(string_to_array(:ids, ',')::uuid[]) AS i(id)
                    CROSS JOIN LATERAL (
                            -- 거래가 있던 날의 최근 행, 없으면 그냥 최근 행. 두 갈래 모두 인덱스를 거꾸로 훑다
                            -- 첫 행에서 멈춘다(예전 식 정렬은 종목 이력 전체를 정렬했다 - StockDailyClosePriceQuery 참고).
                            SELECT y."tradeDate", y."closePrice", y."updatedDate"
                            FROM (
                                    (SELECT h."tradeDate", h."closePrice", h."updatedDate", 0 AS pick
                                     FROM "StockPriceHistory" h
                                     WHERE h."stockItem_id" = i.id
                                       AND h."volume" > 0
                                     ORDER BY h."tradeDate" DESC
                                     LIMIT 1)
                                    UNION ALL
                                    (SELECT h."tradeDate", h."closePrice", h."updatedDate", 1 AS pick
                                     FROM "StockPriceHistory" h
                                     WHERE h."stockItem_id" = i.id
                                     ORDER BY h."tradeDate" DESC
                                     LIMIT 1)
                            ) AS y
                            ORDER BY y.pick
                            LIMIT 1
                    ) AS x
                """)
  List<StockDailyClosePrice> findLatestClosePrices(@Param("ids") String ids);

  /**
   * 한 종목의 일별 종가(차트용).
   *
   * <p>엔티티로 읽지 않고 세 값만 투영한다 &mdash; 시가·고가·저가·거래량까지 매핑하면 행 수가 큰 구간에서 그 변환이 응답 시간을 지배한다(같은 이유로 {@code
   * StockDailyClosePriceQuery} 도 위치 기반 RowMapper 를 쓴다).
   *
   * <p>거래량 0 행은 뺀다. 그 행의 종가 자리에는 직전 종가가 들어 있어(거래가 없던 날) 그대로 그리면 없던 날에 선이 이어진다. 앱의 다른 시세 조회와 같은
   * 규칙이다.
   */
  @Query(
      """
                    SELECT h."stockItem_id" AS stock_item_id,
                           h."tradeDate"    AS trade_date,
                           h."closePrice"   AS close_price
                    FROM "StockPriceHistory" h
                    WHERE h."stockItem_id" = :stockItemId
                      AND h."volume" > 0
                      AND (CAST(:startDate AS date) IS NULL OR h."tradeDate" >= CAST(:startDate AS date))
                      AND (CAST(:endDate   AS date) IS NULL OR h."tradeDate" <= CAST(:endDate   AS date))
                    ORDER BY h."tradeDate"
                """)
  List<StockDailyClosePrice> findDailyClosePrices(
      @Param("stockItemId") UUID stockItemId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 여러 종목의 첫 거래일(거래량 0 행 제외) - 종가 자리는 비운다. 카탈로그는 계산에 필요한 창만 읽으므로 "언제부터 이력이 있나" 는 따로
   * 묻는다(2026-09-23).
   */
  @Query(
      """
                    SELECT h."stockItem_id"        AS stock_item_id,
                           MIN(h."tradeDate")      AS trade_date,
                           CAST(NULL AS numeric)   AS close_price
                    FROM "StockPriceHistory" h
                    WHERE h."stockItem_id" = ANY(string_to_array(:ids, ',')::uuid[])
                      AND h."volume" > 0
                    GROUP BY h."stockItem_id"
                """)
  List<StockDailyClosePrice> findFirstTradeDatesForItems(@Param("ids") String ids);

  Optional<StockPriceHistory> findByStockItemIdAndTradeDate(UUID stockItemId, LocalDate tradeDate);

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateDesc(UUID stockItemId);

  /**
   * 실제로 거래가 있던 날의 마지막 종가.
   *
   * <p>거래량 0 행은 그 날 거래가 없었다는 뜻이고, 그때 KIS 는 종가 자리에 직전 종가를 넣는다. 그 행을 "그 날의 종가"로 쓰면 화면의 평가 기준 일자가 실제보다
   * 앞당겨진다. 값은 어차피 같으므로 달라지는 것은 날짜뿐이다(실측: 거래량 0 행 1,352 개 중 종가가 직전과 다른 것은 1 개뿐).
   *
   * <p>이 종목의 모든 행이 거래량 0 이면 비어 있는 결과가 나오므로, 호출자는 기존 조회로 폴백해야 한다.
   */
  Optional<StockPriceHistory> findTopByStockItemIdAndVolumeGreaterThanOrderByTradeDateDesc(
      UUID stockItemId, long volume);

  /** 기준일 이하에서 실제로 거래가 있던 날의 마지막 종가. 없으면 비어 있다(호출자가 폴백). */
  Optional<StockPriceHistory>
      findTopByStockItemIdAndTradeDateLessThanEqualAndVolumeGreaterThanOrderByTradeDateDesc(
          UUID stockItemId, LocalDate tradeDate, long volume);

  /** 가격 이력이 채워진 가장 최근 거래일(전 종목 기준). 데이터 최신 시점 표시용. */
  @Query(
      """
				SELECT MAX("tradeDate") FROM "StockPriceHistory"
			""")
  LocalDate findLastPriceDate();

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateAsc(UUID stockItemId);

  /**
   * 가장 최근 시세 일자가 직전 거래일의 복제인지 판정할 집계.
   *
   * <p>종목마다 마지막 일자의 행과 그 직전 일자의 행을 짝지어, 종가만 같은 수와 시가/고가/저가/거래량까지 전부 같은 수를 센다. 종목별 직전 거래일은 서로 다를 수
   * 있으므로(그 종목에 그 날 시세가 없을 수 있다) LATERAL 로 종목마다 따로 찾는다.
   *
   * <p>수집이 자동이 아니라서(스케줄러 없음) 사람이 누른 시점에 따라 같은 값이 다른 날짜로 들어갈 수 있다. 그 사실을 관리 화면이 알 수 있게 한다.
   */
  @Query(
      """
                    WITH d AS (SELECT MAX("tradeDate") AS day FROM "StockPriceHistory"),
                    pair AS (
                        SELECT h."closePrice" AS c, h."openPrice" AS o, h."highPrice" AS hi,
                               h."lowPrice" AS lo, h."volume" AS v,
                               x."tradeDate" AS ptd, x."closePrice" AS pc, x."openPrice" AS po,
                               x."highPrice" AS phi, x."lowPrice" AS plo, x."volume" AS pv
                        FROM "StockPriceHistory" h
                        CROSS JOIN d
                        CROSS JOIN LATERAL (
                            SELECT p."tradeDate", p."closePrice", p."openPrice",
                                   p."highPrice", p."lowPrice", p."volume"
                            FROM "StockPriceHistory" p
                            WHERE p."stockItem_id" = h."stockItem_id"
                                AND p."tradeDate" < d.day
                            ORDER BY p."tradeDate" DESC
                            LIMIT 1
                        ) AS x
                        WHERE h."tradeDate" = d.day
                    )
                    SELECT (SELECT day FROM d) AS trade_date,
                           MAX(ptd) AS previous_trade_date,
                           COUNT(*) AS item_count,
                           COUNT(*) FILTER (WHERE c = pc) AS same_close_count,
                           COUNT(*) FILTER (WHERE c = pc AND o = po AND hi = phi
                                              AND lo = plo AND v = pv) AS same_all_count,
                           COUNT(*) FILTER (WHERE v = 0) AS zero_volume_count
                    FROM pair
                """)
  PriceHistoryDuplicateSummary findLastDateDuplicateSummary();

  /**
   * 전체 시세 행 수와 그중 거래량이 0 인 행 수. 관리 화면의 데이터 품질 표시용이다.
   *
   * <p>거래가 없던 시점에 수집하면 KIS 가 직전 종가를 거래량 0 으로 실어 보낸다. 그런 행이 몇 개나 쌓여 있는지 알아야 "이 행들을 종가로 쓰지 않는다"는 판단이
   * 과거 평가액을 얼마나 흔드는지 가늠할 수 있다.
   *
   * <p>두 값을 따로 물으면 같은 57,586 행을 두 번 훑는다. FILTER 로 한 번에 센다.
   *
   * <p>종목 단위 신선도도 같은 왕복에 실어 보낸다. 마지막 일자 하나만 알려 주면 전 종목이 그 날까지 최신인 것으로 읽히는데, 실측 2026-09-12 로는 86 종목
   * 중 9 종목만 2026-09-09 이고 70 종목이 2026-04-01/03 에 멈춰 있었다(이력이 아예 없는 종목 4). 시세 갱신이 보유 종목 위주로 도는 결과라
   * 데이터가 잘못된 것은 아니지만, 관리 화면이 그렇게 말하지 않으면 알 수 없다.
   */
  @Query(
      """
                    SELECT COUNT(*) AS total_count,
                        COUNT(*) FILTER (WHERE "volume" = 0) AS zero_volume_count,
                        (SELECT COUNT(DISTINCT x."stockItem_id")
                             FROM "StockPriceHistory" x
                             WHERE x."tradeDate" = (SELECT MAX(y."tradeDate")
                                                        FROM "StockPriceHistory" y)
                        ) AS last_date_item_count,
                        (SELECT COUNT(*)
                             FROM "StockItem" s
                             WHERE NOT EXISTS (SELECT 1
                                                   FROM "StockPriceHistory" z
                                                   WHERE z."stockItem_id" = s."id")
                        ) AS no_history_item_count
                    FROM "StockPriceHistory"
                """)
  PriceHistoryRowCounts findRowCounts();

  /**
   * 위 개수에 해당하는 행 자체. 개수만으로는 그것이 수집 오류인지 액면분할 같은 정상 조정인지 알 수 없다.
   *
   * <p>관리 화면이 "1 건" 이라고만 알려 주던 것을 어느 종목의 어느 날인지까지 알려 주기 위한 조회다. 응답이 원장 크기를 따라가지 않도록 상한을 둔다.
   */
  @Query(
      """
                    SELECT h."stockItem_id" AS stock_item_id,
                        h."tradeDate" AS trade_date,
                        h."closePrice" AS close_price,
                        x."closePrice" AS previous_close_price,
                        COUNT(*) OVER () AS total_count
                    FROM "StockPriceHistory" h
                    CROSS JOIN LATERAL (
                        SELECT p."closePrice"
                        FROM "StockPriceHistory" p
                        WHERE p."stockItem_id" = h."stockItem_id"
                            AND p."tradeDate" < h."tradeDate"
                        ORDER BY p."tradeDate" DESC
                        LIMIT 1
                    ) AS x
                    WHERE h."volume" = 0 AND h."closePrice" <> x."closePrice"
                    ORDER BY h."tradeDate" DESC
                    LIMIT 5
                """)
  List<ZeroVolumeChangedClose> findZeroVolumeRowsWithChangedClose();

  /**
   * 시세 이력이 한 행도 없는 종목. 관리 화면이 개수만 말하던 것을 이름까지 적기 위한 목록이다.
   *
   * <p>개수를 세는 {@code findRowCounts} 의 하위 질의와 <b>같은 조건</b>이어야 한다 - 화면에 "4 개" 라고 적고 세 줄만 보여 주면 그것대로
   * 거짓말이다. 상한은 두되 넉넉히 잡는다.
   */
  @Query(
      """
                    SELECT s."id" AS stock_item_id, s."symbol" AS symbol, s."name" AS name
                    FROM "StockItem" s
                    WHERE NOT EXISTS (SELECT 1
                                          FROM "StockPriceHistory" z
                                          WHERE z."stockItem_id" = s."id")
                    ORDER BY s."symbol"
                    LIMIT 50
                """)
  List<ItemWithoutPriceHistory> findItemsWithoutPriceHistory();

  /**
   * 하루 만에 가격제한폭(±30%)을 넘은 행. 거래로는 생길 수 없는 변동이므로 분할·병합 같은 기업행위이거나 수집 오류다. 위의 거래량 0 점검과 짝을 이룬다.
   *
   * <p>응답이 원장 크기를 따라가지 않도록 상한을 두고, 총 개수는 같은 스캔 안에서 COUNT(*) OVER () 로 함께 낸다 - 개수와 행을 따로 물으면 같은 비싼
   * 스캔(시세 57,586행 위 윈도우)을 두 번 한다.
   *
   * <p>오래 쉰 뒤의 첫 거래는 제한폭 판정 대상이 아니므로 직전 거래일과 7일 이내인 행만 본다. 직전 행은 LAG 윈도우로 잡는다 - 예전에는 행마다 LATERAL
   * 서브쿼리로 직전 행을 다시 찾았는데, 거래량 0 점검과 달리 이 조건은 전 행을 대상으로 해 그 방식이 136ms 였다 (2026-09-09, dataStatus
   * 307ms 의 44%).
   *
   * <p>기준을 0.30 이 아니라 0.301 로 두는 이유: 상·하한가는 기준가에 0.7/1.3 을 곱한 뒤 호가단위로 맞추므로, 정상적인 하한가도 30% 를 아주 조금
   * 넘길 수 있다. 실측 2026-09-10: 한화오션 2015-07-15 은 55,526 -> 38,868 로 -30.00036% 였다(이론 하한가 38,868.2 를
   * 호가단위로 내린 값). 호가단위/기준가 최대비는 모든 가격대에서 0.100%p 이므로(2,000 미만 1원 · 5,000 미만 5원 · 20,000 미만 10원 ·
   * 50,000 미만 50원 · 200,000 미만 100원 · 500,000 미만 500원 · 그 이상 1,000원) 0.1%p 여유면 충분하다. 같은 원장의 진짜 이탈은
   * 34.78% · 42.15% · 67.10% · 80.00% 로 이 여유와 한참 떨어져 있다.
   *
   * <p>실측 2026-09-10: 이 조회 하나가 28~29ms 로 dataStatus 57ms 의 절반이다. 같은 윈도우를 인덱스 컬럼만으로 돌리면 13~14ms 이므로
   * 나머지 절반은 closePrice 힙 접근이다(유니크 인덱스는 stockItem_id, tradeDate 뿐).
   */
  @Query(
      """
                    WITH w AS (
                        SELECT "stockItem_id",
                            "tradeDate",
                            "closePrice",
                            LAG("closePrice") OVER (PARTITION BY "stockItem_id" ORDER BY "tradeDate") AS prev_close,
                            LAG("tradeDate") OVER (PARTITION BY "stockItem_id" ORDER BY "tradeDate") AS prev_date
                        FROM "StockPriceHistory"
                    )
                    SELECT "stockItem_id" AS stock_item_id,
                        "tradeDate" AS trade_date,
                        "closePrice" AS close_price,
                        prev_close AS previous_close_price,
                        prev_date AS previous_trade_date,
                        COUNT(*) OVER () AS total_count
                    FROM w
                    WHERE prev_close > 0
                        AND "closePrice" > 0
                        AND "tradeDate" - prev_date <= 7
                        AND ABS("closePrice"::numeric / prev_close::numeric - 1) > 0.301
                    ORDER BY "tradeDate" DESC
                    LIMIT 5
                """)
  List<PriceLimitBreachRow> findPriceLimitBreachRows();

  /**
   * 원주가(수정 전 종가) 열이 있는가. 2026-10-02 schema-alter 묶음으로 생긴 열이라 아직 적용 안 한 DB 도 있다 - 엔티티에 넣지 않고 이 확인
   * 뒤에만 아래 두 질의를 쓴다(열이 없으면 앱은 정수배 추정 그대로).
   */
  @Query(
      """
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_name = 'StockPriceHistory' AND column_name = 'rawClosePrice'
                """)
  long countRawClosePriceColumn();

  /**
   * 원주가를 (다시) 받을 시세 날짜(종가 자리에는 수정 종가): 아직 빈 날 + recentFrom 이후의 날. 최근 날은 채워져 있어도 다시 받는다 - 장중에 시세를
   * 갱신하면 그 날 원주가가 장중 가격으로 들어가는데, 수정 종가는 다음 갱신 때 확정 종가로 덮여도 원주가는 "빈 날만" 채우면 그대로 남는다(2026-10-02).
   */
  @Query(
      """
                    SELECT h."stockItem_id" AS stock_item_id,
                           h."tradeDate"     AS trade_date,
                           h."closePrice"    AS close_price
                    FROM "StockPriceHistory" h
                    WHERE h."stockItem_id" = ANY(string_to_array(:ids, ',')::uuid[])
                      AND (h."rawClosePrice" IS NULL OR h."tradeDate" >= CAST(:recentFrom AS date))
                      AND h."volume" > 0
                """)
  List<StockDailyClosePrice> findRawCloseDaysToFill(
      @Param("ids") String ids, @Param("recentFrom") LocalDate recentFrom);

  /**
   * 이 사용자의 보유 기간 원주가가 채워진 정도(거래가 있던 날만). 보유 기간 = 종목별 첫 매매일부터, 순수량이 0 이하면 마지막 매매일까지 -
   * KisStockPriceUpdateService.fillRawClosePrices 와 같은 범위다.
   */
  @Query(
      """
                    WITH t AS (
                        SELECT tr."stockItem_id" AS sid,
                               MIN((tr."tradeDate" AT TIME ZONE 'UTC')::date) AS f,
                               MAX((tr."tradeDate" AT TIME ZONE 'UTC')::date) AS l,
                               SUM(CASE WHEN tr."type" = 'BUY' THEN tr."quantity" ELSE -tr."quantity" END) AS net
                        FROM "Trade" tr
                        JOIN "Account" a ON tr."account_id" = a."id"
                        WHERE a."user_id" = :userId AND tr."stockItem_id" IS NOT NULL
                        GROUP BY tr."stockItem_id")
                    SELECT COUNT(*) AS day_count,
                           COUNT(*) FILTER (WHERE h."rawClosePrice" IS NULL) AS missing_day_count
                    FROM "StockPriceHistory" h
                    JOIN t ON t.sid = h."stockItem_id"
                    WHERE h."tradeDate" >= t.f
                      AND (t.net > 0 OR h."tradeDate" <= t.l)
                      AND h."volume" > 0
                """)
  net.luversof.api.stock.domain.RawCloseCoverage findRawCloseCoverage(@Param("userId") UUID userId);

  /** 한 행의 원주가만 바꾼다(엔티티 저장은 이 열을 모른다 - 다른 열은 건드리지 않는다). 바뀐 행 수(0 이면 그 날 시세 행이 없다). */
  @Modifying
  @Query(
      """
                    UPDATE "StockPriceHistory" SET "rawClosePrice" = :rawClose
                    WHERE "stockItem_id" = :stockItemId AND "tradeDate" = :tradeDate
                """)
  int updateRawClosePrice(
      @Param("stockItemId") UUID stockItemId,
      @Param("tradeDate") LocalDate tradeDate,
      @Param("rawClose") java.math.BigDecimal rawClose);
}
