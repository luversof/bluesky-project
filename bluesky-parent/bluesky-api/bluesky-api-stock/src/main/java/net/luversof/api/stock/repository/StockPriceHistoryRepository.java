package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import net.luversof.api.stock.domain.PriceHistoryDuplicateSummary;
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
                                 x."closePrice" AS close_price
                    FROM unnest(string_to_array(:ids, ',')::uuid[]) AS i(id)
                    CROSS JOIN LATERAL (
                            SELECT h."tradeDate", h."closePrice"
                            FROM "StockPriceHistory" h
                            WHERE h."stockItem_id" = i.id
                            ORDER BY (h."volume" > 0) DESC, h."tradeDate" DESC
                            LIMIT 1
                    ) AS x
                """)
  List<StockDailyClosePrice> findLatestClosePrices(@Param("ids") String ids);

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
   * 전 구간에서 거래량이 0 인 시세 행 수. 관리 화면의 데이터 품질 표시용이다.
   *
   * <p>거래가 없던 시점에 수집하면 KIS 가 직전 종가를 거래량 0 으로 실어 보낸다. 그런 행이 몇 개나 쌓여 있는지 알아야 "이 행들을 종가로 쓰지 않는다"는 판단이
   * 과거 평가액을 얼마나 흔드는지 가늠할 수 있다.
   */
  @Query(
      """
                    SELECT COUNT(*) FROM "StockPriceHistory" WHERE "volume" = 0
                """)
  long countZeroVolumeRows();

  @Query(
      """
                    SELECT COUNT(*) FROM "StockPriceHistory"
                """)
  long countAllRows();

  /**
   * 거래량이 0 인데 종가가 직전 행과 <b>다른</b> 행 수.
   *
   * <p>거래가 없으면 종가가 바뀔 수 없으므로 이 값은 0 에 가까워야 한다. 0 이면 거래량 0 행은 정보를 갖고 있지 않다는 뜻이고, 그때만 "종가로 쓰지 않는다"는
   * 판단이 과거 평가액을 흔들지 않는다. 0 이 아니면 그 행들을 빼는 순간 과거 값이 달라지므로 함부로 뺄 수 없다.
   */
  @Query(
      """
                    SELECT COUNT(*)
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
                """)
  long countZeroVolumeRowsWithChangedClose();

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
                        x."closePrice" AS previous_close_price
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
   * 하루 만에 가격제한폭(±30%)을 넘은 행의 개수.
   *
   * <p>거래로는 생길 수 없는 변동이므로 분할·병합 같은 기업행위이거나 수집 오류다. 위의 거래량 0 점검은 이걸 못 잡는다 - 분할은 보통 거래량이 붙는다.
   *
   * <p>오래 쉰 뒤의 첫 거래는 제한폭 판정 대상이 아니므로 직전 거래일과 7일 이내인 행만 본다.
   */
  @Query(
      """
                    SELECT COUNT(*)
                    FROM "StockPriceHistory" h
                    CROSS JOIN LATERAL (
                        SELECT p."closePrice", p."tradeDate"
                        FROM "StockPriceHistory" p
                        WHERE p."stockItem_id" = h."stockItem_id"
                            AND p."tradeDate" < h."tradeDate"
                        ORDER BY p."tradeDate" DESC
                        LIMIT 1
                    ) AS x
                    WHERE x."closePrice" > 0
                        AND h."closePrice" > 0
                        AND h."tradeDate" - x."tradeDate" <= 7
                        AND ABS(h."closePrice"::numeric / x."closePrice"::numeric - 1) > 0.30
                """)
  long countPriceLimitBreachRows();

  /** 위 개수에 해당하는 행 자체. 응답이 원장 크기를 따라가지 않도록 상한을 둔다. */
  @Query(
      """
                    SELECT h."stockItem_id" AS stock_item_id,
                        h."tradeDate" AS trade_date,
                        h."closePrice" AS close_price,
                        x."closePrice" AS previous_close_price,
                        x."tradeDate" AS previous_trade_date
                    FROM "StockPriceHistory" h
                    CROSS JOIN LATERAL (
                        SELECT p."closePrice", p."tradeDate"
                        FROM "StockPriceHistory" p
                        WHERE p."stockItem_id" = h."stockItem_id"
                            AND p."tradeDate" < h."tradeDate"
                        ORDER BY p."tradeDate" DESC
                        LIMIT 1
                    ) AS x
                    WHERE x."closePrice" > 0
                        AND h."closePrice" > 0
                        AND h."tradeDate" - x."tradeDate" <= 7
                        AND ABS(h."closePrice"::numeric / x."closePrice"::numeric - 1) > 0.30
                    ORDER BY h."tradeDate" DESC
                    LIMIT 5
                """)
  List<PriceLimitBreachRow> findPriceLimitBreachRows();
}
