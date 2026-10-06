package net.luversof.api.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import net.luversof.api.stock.domain.StockDailyClosePrice;

/**
 * 일별 종가 대량 조회. 같은 SQL 을 Spring Data 의 {@code @Query} 대신 JdbcTemplate 으로 직접 읽는다.
 *
 * <p>이 조회는 손익 시뮬레이션 응답 시간의 대부분을 차지한다(스택 샘플 111개 중 90개). 느린 쪽은 DB 가 아니라 행 매핑이었다 — 샘플 최상단이 {@code
 * Unsafe.allocateInstance}, {@code ResolvableType.forClass}, {@code SqlIdentifier.getReference} 등
 * Spring Data 의 행 변환 리플렉션이다. 전체 기간이면 87,465 행을 그 경로로 통과시킨다.
 *
 * <p>컬럼 3개를 위치로 읽는 RowMapper 는 그 변환 기계를 통째로 건너뛴다. SQL 과 결과는 그대로다.
 */
@Repository
public class StockDailyClosePriceQuery {

  private static final String RANGES_SQL =
      """
      SELECT h."stockItem_id", h."tradeDate", h."closePrice"
      FROM unnest(string_to_array(:ids, ',')::uuid[],
                  string_to_array(:froms, ',')::date[],
                  string_to_array(:tos, ',')::date[]) AS f(id, from_date, to_date)
      JOIN "StockPriceHistory" h
        ON h."stockItem_id" = f.id
       AND h."tradeDate" >= f.from_date
       AND h."tradeDate" <= f.to_date
       -- 거래량 0 인 날은 그 날 거래가 없었다는 뜻이고, 종가 자리에는 직전 종가가 들어 있다.
       -- 그 행을 그 날의 종가로 쓰면 화면의 평가 기준 일자가 실제보다 앞당겨진다(실측 2026-08-22:
       -- 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일). 값은 어차피 같으므로
       -- (거래량 0 행 1,352 개 중 종가가 직전과 다른 것은 1 개) 빼도 평가액은 변하지 않는다.
       AND h."volume" > 0
      """;

  /**
   * 위 SQL 과 같은 조인이지만 종목 UUID 대신 입력 배열에서의 순번(ordinality)을 돌려준다.
   *
   * <p>행마다 UUID 를 만들 이유가 없다 — 전체 기간이면 87,465 행이 오는데 서로 다른 종목은 수십 개뿐이고, 순번으로 호출부의 UUID 인스턴스를 그대로
   * 재사용할 수 있다. 조인 조건과 결과 집합은 동일하다.
   */
  private static final String RANGES_ORDINALITY_SQL =
      """
      SELECT f.ord, h."tradeDate", h."closePrice"
      FROM unnest(string_to_array(:ids, ',')::uuid[],
                  string_to_array(:froms, ',')::date[],
                  string_to_array(:tos, ',')::date[]) WITH ORDINALITY AS f(id, from_date, to_date, ord)
      JOIN "StockPriceHistory" h
        ON h."stockItem_id" = f.id
       AND h."tradeDate" >= f.from_date
       AND h."tradeDate" <= f.to_date
       -- 거래량 0 인 날은 그 날 거래가 없었다는 뜻이고, 종가 자리에는 직전 종가가 들어 있다.
       -- 그 행을 그 날의 종가로 쓰면 화면의 평가 기준 일자가 실제보다 앞당겨진다(실측 2026-08-22:
       -- 2026-08-20 행 9건이 전부 거래량 0, 종가는 08-19 와 동일). 값은 어차피 같으므로
       -- (거래량 0 행 1,352 개 중 종가가 직전과 다른 것은 1 개) 빼도 평가액은 변하지 않는다.
       AND h."volume" > 0
      """;

  private static final String ITEMS_FROM_SQL =
      """
      SELECT h."stockItem_id" AS stock_item_id,
             h."tradeDate"    AS trade_date,
             h."closePrice"   AS close_price
      FROM "StockPriceHistory" h
      WHERE h."stockItem_id" = ANY(string_to_array(:ids, ',')::uuid[])
        AND h."volume" > 0
        AND h."tradeDate" >= CAST(:fromDate AS date)
      ORDER BY h."stockItem_id", h."tradeDate"
      """;

  private static final RowMapper<StockDailyClosePrice> MAPPER =
      (rs, rowNum) ->
          new StockDailyClosePrice(
              rs.getObject(1, UUID.class), rs.getObject(2, LocalDate.class), rs.getBigDecimal(3));

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private static final String PAIRS_SQL =
      """
      SELECT p.id, x."tradeDate", x."closePrice"
      FROM unnest(string_to_array(:ids, ',')::uuid[],
                  string_to_array(:days, ',')::date[]) AS p(id, day)
      CROSS JOIN LATERAL (
        -- 거래가 있던 날(거래량 > 0)의 최근 행을 먼저 고르고, 그런 행이 하나도 없는 종목은 값이 사라지면 안 되므로
        -- 그냥 최근 행으로 물러선다. 두 갈래 모두 (종목, 일자) 인덱스를 거꾸로 훑다가 첫 행에서 멈춘다.
        -- 예전에는 '거래량이 있는가' 라는 식을 첫 정렬 키로 둔 한 줄 정렬이었는데, 식이 앞에 있으니 인덱스 순서를
        -- 못 타고 쌍마다 그 종목의 기준일 이전 이력 전체를 정렬했다(실측 2026-09-23: holdingsSnapshotBatch
        -- 요청 표본의 51%). 결과는 같다 - 단, 그 정렬은 내림차순이라 거래량 NULL 행을 맨 앞에 두었는데 여기서는
        -- NULL 을 '거래 없음' 으로 본다(앱은 원시형 long 으로만 써서 NULL 을 만들지 않는다).
        SELECT y."tradeDate", y."closePrice"
        FROM (
          (SELECT h."tradeDate", h."closePrice", 0 AS pick
           FROM "StockPriceHistory" h
           WHERE h."stockItem_id" = p.id
             AND h."tradeDate" <= p.day
             AND h."volume" > 0
           ORDER BY h."tradeDate" DESC
           LIMIT 1)
          UNION ALL
          (SELECT h."tradeDate", h."closePrice", 1 AS pick
           FROM "StockPriceHistory" h
           WHERE h."stockItem_id" = p.id
             AND h."tradeDate" <= p.day
           ORDER BY h."tradeDate" DESC
           LIMIT 1)
        ) AS y
        ORDER BY y.pick
        LIMIT 1
      ) AS x
      """;

  /**
   * (종목, 기준일) 쌍마다 그 날 이하의 최근 종가. 위와 같은 이유로 Spring Data 의 행 변환을 거치지 않는다(실측: 이 조회가
   * holdingsSnapshotBatch 스택 샘플의 32%).
   */
  public List<StockDailyClosePrice> findLatestClosePricesForPairs(String ids, String days) {
    return namedParameterJdbcTemplate.query(PAIRS_SQL, Map.of("ids", ids, "days", days), MAPPER);
  }

  /**
   * 여러 종목의 일별 종가를 한 번에(종목 &rarr; 거래일 순), 거래량 0 행은 뺀다. 월배당 카탈로그가 계산 창(15 개월)만큼 읽는다.
   *
   * <p>예전에는 StockPriceHistoryRepository 의 {@code @Query} 였다. SQL 은 그대로인데 Spring Data 의 행 변환이 약
   * 6,700 행을 지나며 카탈로그 응답 시간의 대부분을 먹었다 &mdash; 실측 2026-09-23: findCatalog 표본의 71% 가 이 호출이었고 그중 DB 는
   * 일부, 나머지는 {@code Lazy.get} &middot; {@code ResolvableType.forType} &middot; {@code
   * SqlIdentifier} 같은 변환 기계였다. 위의 다른 조회와 같은 까닭으로 위치 RowMapper 로 읽는다.
   */
  public List<StockDailyClosePrice> findDailyClosePricesForItems(String ids, LocalDate fromDate) {
    return namedParameterJdbcTemplate.query(
        ITEMS_FROM_SQL, Map.of("ids", ids, "fromDate", fromDate), MAPPER);
  }

  /**
   * 위와 같은 범위의 원주가(수정 전 종가) - 월배당 카탈로그 기간 수익률용(2026-10-02). 원주가가 빈 날은 뺀다. 결과의 종가 자리가 원주가다.
   *
   * <p>수정 종가는 처음에 한꺼번에 받은 날이면 분배금만큼 깎여 있어(0094M0 12 개월 전 원주가 11,285 / 수정 8,840) 가격 수익률이 부풀고 합산 수익률은
   * 분배금을 두 번 센다.
   */
  private static final String ITEMS_RAW_FROM_SQL =
      """
      SELECT h."stockItem_id" AS stock_item_id,
             h."tradeDate"    AS trade_date,
             CASE WHEN h."tradeDate" = l.latest
                  THEN h."closePrice" ELSE h."rawClosePrice" END AS close_price
      FROM (SELECT DISTINCT unnest(string_to_array(:ids, ',')::uuid[]) AS id) s
      CROSS JOIN LATERAL (SELECT max(x."tradeDate") AS latest FROM "StockPriceHistory" x WHERE x."stockItem_id" = s.id AND x."volume" > 0) l
      JOIN "StockPriceHistory" h ON h."stockItem_id" = s.id
      WHERE (h."rawClosePrice" IS NOT NULL OR h."tradeDate" = l.latest)
        AND h."volume" > 0
        AND h."tradeDate" >= CAST(:fromDate AS date)
      ORDER BY h."stockItem_id", h."tradeDate"
      """;

  public List<StockDailyClosePrice> findRawClosePricesForItems(String ids, LocalDate fromDate) {
    return namedParameterJdbcTemplate.query(
        ITEMS_RAW_FROM_SQL, Map.of("ids", ids, "fromDate", fromDate), MAPPER);
  }

  /**
   * 위 조회를 (일자 -> (종목 -> 종가)) 형태로 바로 채워 돌려준다.
   *
   * <p>호출부는 어차피 이 중첩 맵만 쓴다. 예전에는 행마다 {@link StockDailyClosePrice} 를 만들어 87,465 개짜리 리스트에 담은 뒤 다시 전체를
   * 훑어 맵으로 옮겼다. 결과가 같은데 레코드 8.7만 개와 리스트 한 벌, 그리고 두 번째 순회가 통째로 낭비였다. 여기서 바로 담으면 셋 다 사라진다. 종목 키는
   * {@code idOrder} 의 인스턴스를 재사용하므로 UUID 도 새로 만들지 않는다.
   *
   * @param idOrder {@code ids} 문자열에 넣은 것과 같은 순서의 종목 목록(순번 -> UUID)
   */
  public Map<LocalDate, Map<UUID, BigDecimal>> findDailyClosePricesGrouped(
      String ids, String froms, String tos, List<UUID> idOrder) {
    Map<LocalDate, Map<UUID, BigDecimal>> grouped = new HashMap<>();
    namedParameterJdbcTemplate.query(
        RANGES_ORDINALITY_SQL,
        Map.of("ids", ids, "froms", froms, "tos", tos),
        rs -> {
          UUID stockItemId = idOrder.get(rs.getInt(1) - 1);
          LocalDate tradeDate = rs.getObject(2, LocalDate.class);
          grouped
              .computeIfAbsent(tradeDate, k -> new HashMap<>())
              .put(stockItemId, rs.getBigDecimal(3));
        });
    return grouped;
  }

  /**
   * 종목마다 [from, to] 구간의 원주가와 수정 종가(원주가 평가 - TradeProfitService.RawValuation), 종목 &rarr; 거래일 순.
   *
   * <p>처음(2026-10-02)에는 Spring Data {@code @Query} 로 읽어 보유 기간 1.4 만 행이 위의 행 변환 리플렉션을 그대로 통과했다 - 시계열
   * 응답이 25ms 에서 254ms 로 늘었다(api-perf.js). 같은 이유로 위치로 읽고 종목 UUID 는 순번으로 재사용한다.
   *
   * <p><b>종목의 가장 최근 거래일은 원주가 자리에 수정 종가를 쓴다</b>(이 클래스의 원주가 조회 셋 모두, 2026-10-02). 마지막 날은 수정할 과거가 없어
   * 둘이 같은 가격인데, 시세 갱신(시가 &middot; 고가 &middot; 저가 &middot; 종가)과 원주가 채우기가 장중의 다른 시각에 받으면 값이 갈린다 - 실측:
   * 오늘 행이 10:16 시세 그대로인데 원주가는 나중에 다시 받아 20 종목이 달랐고, 계좌 카드 평가액(수정 종가)과 시계열 끝점(원주가)이 0.04~0.18% 어긋났으며
   * 캔들은 종가가 고가 &middot; 저가 밖으로 나갈 수 있었다. 다음 시세 갱신이 둘을 함께 새로 받는다.
   *
   * <p>최근 거래일은 <b>종목마다 한 번</b> 구한다(LATERAL). 처음에는 행마다 상관 부분 조회로 구해 보유 기간 1.4 만 행이 인덱스를 1.4 만 번 탔다
   * &mdash; 시계열 응답이 38ms 에서 78ms 로 늘었다(api-perf.js). 최근 거래일은 거래량이 있던 날이다(2026-10-03 검토) - 조회가 거래량 0
   * 행을 빼므로 마지막 행이 거래량 0 이면 규칙이 어느 행에도 안 걸려, 카드(거래가 있던 최근 행)와 다시 갈렸다.
   */
  private static final String RAW_RANGES_ORDINALITY_SQL =
      """
      SELECT f.ord, h."tradeDate",
             CASE WHEN h."tradeDate" = l.latest
                  THEN h."closePrice" ELSE h."rawClosePrice" END,
             h."closePrice"
      FROM unnest(string_to_array(:ids, ',')::uuid[],
                  string_to_array(:froms, ',')::date[],
                  string_to_array(:tos, ',')::date[]) WITH ORDINALITY AS f(id, from_date, to_date, ord)
      CROSS JOIN LATERAL (SELECT max(x."tradeDate") AS latest FROM "StockPriceHistory" x WHERE x."stockItem_id" = f.id AND x."volume" > 0) l
      JOIN "StockPriceHistory" h
        ON h."stockItem_id" = f.id
       AND h."tradeDate" >= f.from_date
       AND h."tradeDate" <= f.to_date
       -- 다른 종가 조회와 같은 규칙: 거래량 0 인 날(거래정지 - 종가 자리에 직전 값)은 뺀다.
       AND h."volume" > 0
      ORDER BY f.ord, h."tradeDate"
      """;

  public Map<UUID, List<net.luversof.api.stock.domain.StockRawClose>> findRawClosesGrouped(
      String ids, String froms, String tos, List<UUID> idOrder) {
    Map<UUID, List<net.luversof.api.stock.domain.StockRawClose>> grouped = new HashMap<>();
    namedParameterJdbcTemplate.query(
        RAW_RANGES_ORDINALITY_SQL,
        Map.of("ids", ids, "froms", froms, "tos", tos),
        rs -> {
          UUID stockItemId = idOrder.get(rs.getInt(1) - 1);
          grouped
              .computeIfAbsent(stockItemId, k -> new java.util.ArrayList<>())
              .add(
                  new net.luversof.api.stock.domain.StockRawClose(
                      stockItemId,
                      rs.getObject(2, LocalDate.class),
                      rs.getBigDecimal(3),
                      rs.getBigDecimal(4)));
        });
    return grouped;
  }

  /**
   * 한 종목의 일별 시가 · 고가 · 저가 · 종가(수정 주가)와 원주가, 날짜순 - 종목 상세 캔들 차트(2026-10-02). 거래량 0 인 날은 뺀다(다른 종가 조회와
   * 같은 규칙). withRaw=false 면 원주가 열을 읽지 않는다(열이 없는 DB).
   */
  private static final String OHLC_SQL =
      """
      SELECT h."tradeDate", h."openPrice", h."highPrice", h."lowPrice", h."closePrice",
             CASE WHEN h."tradeDate" = l.latest
                  THEN h."closePrice" ELSE h."rawClosePrice" END
      FROM "StockPriceHistory" h
      CROSS JOIN (SELECT max(x."tradeDate") AS latest FROM "StockPriceHistory" x WHERE x."stockItem_id" = :id AND x."volume" > 0) l
      WHERE h."stockItem_id" = :id
        AND h."tradeDate" >= CAST(:fromDate AS date)
        AND h."volume" > 0
      ORDER BY h."tradeDate"
      """;

  private static final String OHLC_NO_RAW_SQL =
      """
      SELECT h."tradeDate", h."openPrice", h."highPrice", h."lowPrice", h."closePrice", NULL
      FROM "StockPriceHistory" h
      WHERE h."stockItem_id" = :id
        AND h."tradeDate" >= CAST(:fromDate AS date)
        AND h."volume" > 0
      ORDER BY h."tradeDate"
      """;

  public List<net.luversof.api.stock.domain.StockOhlcRow> findOhlc(
      UUID id, LocalDate fromDate, boolean withRaw) {
    return namedParameterJdbcTemplate.query(
        withRaw ? OHLC_SQL : OHLC_NO_RAW_SQL,
        Map.of("id", id, "fromDate", fromDate),
        (rs, rowNum) ->
            new net.luversof.api.stock.domain.StockOhlcRow(
                rs.getObject(1, LocalDate.class),
                rs.getBigDecimal(2),
                rs.getBigDecimal(3),
                rs.getBigDecimal(4),
                rs.getBigDecimal(5),
                rs.getBigDecimal(6)));
  }
}
