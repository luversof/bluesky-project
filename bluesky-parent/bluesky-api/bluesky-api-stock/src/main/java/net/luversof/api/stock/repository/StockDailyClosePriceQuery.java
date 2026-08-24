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
        SELECT h."tradeDate", h."closePrice"
        FROM "StockPriceHistory" h
        WHERE h."stockItem_id" = p.id
          AND h."tradeDate" <= p.day
        -- 거래가 있던 날을 먼저 고른다. 그런 행이 하나도 없는 종목은 값이 사라지면 안 되므로
        -- 정렬 우선순위로만 두고 폴백을 남긴다.
        ORDER BY (h."volume" > 0) DESC, h."tradeDate" DESC
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
}
