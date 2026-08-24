package net.luversof.api.stock.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Trade;

/**
 * 손익 시뮬레이션이 쓰는 거래 대량 조회. 파생 쿼리 대신 명시적 RowMapper 로 읽는다.
 *
 * <p>행 수는 250 건뿐인데 스택 샘플의 21% 가 이 조회에 있었고 그중 28/30 이 DB 대기가 아닌 CPU 였다 — Spring Data 의 엔티티 머티리얼라이즈
 * 비용이다. 반환 타입은 그대로 {@link Trade} 라 호출부는 바뀌지 않는다.
 *
 * <p>ORDER BY 를 붙이지 않은 것은 의도적이다. 파생 쿼리도 정렬이 없었고, 정렬을 추가하면 기존 출력과 달라질 수 있다.
 */
@Repository
public class TradeQuery {

  private static final String SELECT =
      """
      SELECT t."id", t."account_id", t."stockItem_id", t."type", t."quantity", t."price",
             t."fee", t."tax", t."tradeDate", t."realizedProfit", t."exchangeRate"
      FROM "Trade"
      """;

  private static final RowMapper<Trade> MAPPER = TradeQuery::mapRow;

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private static Trade mapRow(ResultSet rs, int rowNum) throws SQLException {
    Trade trade = new Trade();
    trade.setId(rs.getObject(1, UUID.class));
    trade.setAccountId(rs.getObject(2, UUID.class));
    trade.setStockItemId(rs.getObject(3, UUID.class));
    String type = rs.getString(4);
    trade.setType(type != null ? TradeType.valueOf(type) : null);
    trade.setQuantity(rs.getInt(5));
    trade.setPrice(rs.getBigDecimal(6));
    trade.setFee(rs.getBigDecimal(7));
    trade.setTax(rs.getBigDecimal(8));
    // pgjdbc 는 timestamptz -> Instant 직접 변환을 지원하지 않는다(측정 중 확인).
    // OffsetDateTime 으로 받아 변환해야 타임존 해석이 드라이버와 일치한다.
    java.time.OffsetDateTime tradeDate = rs.getObject(9, java.time.OffsetDateTime.class);
    trade.setTradeDate(tradeDate != null ? tradeDate.toInstant() : null);
    trade.setRealizedProfit(rs.getBigDecimal(10));
    trade.setExchangeRate(rs.getBigDecimal(11));
    return trade;
  }

  /**
   * 사용자의 모든 거래. 계좌 목록을 먼저 읽어 id 를 뽑고 다시 거래를 읽던 것을 조인 한 번으로 합친다(DB 왕복 2회 -> 1회).
   *
   * <p>계좌 목록은 id 를 얻는 용도였을 뿐 다른 데 쓰이지 않았다(호출부 확인).
   */
  public List<Trade> findByUserId(UUID userId) {
    return namedParameterJdbcTemplate.query(
        SELECT
            + """
             t JOIN "Account" a ON t."account_id" = a."id" WHERE a."user_id" = :userId
            """,
        Map.of("userId", userId),
        MAPPER);
  }

  public List<Trade> findByAccountIdIn(List<UUID> accountIdList) {
    if (accountIdList == null || accountIdList.isEmpty()) {
      return List.of();
    }
    return namedParameterJdbcTemplate.query(
        SELECT + " t WHERE t.\"account_id\" IN (:accountIdList)",
        Map.of("accountIdList", accountIdList),
        MAPPER);
  }
}
