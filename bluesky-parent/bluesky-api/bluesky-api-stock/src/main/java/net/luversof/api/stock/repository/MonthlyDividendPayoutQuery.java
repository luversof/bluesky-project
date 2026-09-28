package net.luversof.api.stock.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import net.luversof.api.stock.domain.MonthlyDividendPayout;

/**
 * 월배당 카탈로그가 쓰는 지급 이력 일괄 조회. 파생 쿼리 대신 명시적 RowMapper 로 읽는다({@link TradeQuery} 와 같은 까닭).
 *
 * <p>실측 2026-09-23: 카탈로그 응답(약 32ms)의 62% 가 이 조회였고 그중 85% 가 DB 대기가 아닌 Spring Data 행 변환이었다. SQL 은 파생
 * 쿼리 {@code findByStockItemIdInOrderByPayDateDescRecordDateDesc} 가 만들던 것과 같다(컬럼 · 조건 · 정렬). 반환 타입도
 * 그대로 {@link MonthlyDividendPayout} 이라 호출부 계산은 바뀌지 않는다.
 */
@Repository
public class MonthlyDividendPayoutQuery {

  private static final String BY_STOCK_ITEMS_SQL =
      """
      SELECT "id", "stockItem_id", "recordDate", "payDate", "distributionRatePct",
             "dividendAmountPerShare", "taxableBasePerShare", "createdDate", "updatedDate"
      FROM "MonthlyDividendPayout"
      WHERE "stockItem_id" IN (:ids)
      ORDER BY "payDate" DESC, "recordDate" DESC
      """;

  /**
   * 전 종목 지급 이력 - 파생 쿼리 {@code findAllByOrderByPayDateDescRecordDateDesc} 와 같은 SQL. 관리 화면 데이터 상태 ·
   * 원장 점검 · 지급 이력 목록이 쓴다(실측 2026-09-23: dataStatus 72ms 의 45% 가 이 조회였고 그중 81% 가 Spring Data 행 변환).
   */
  private static final String ALL_SQL =
      """
      SELECT "id", "stockItem_id", "recordDate", "payDate", "distributionRatePct",
             "dividendAmountPerShare", "taxableBasePerShare", "createdDate", "updatedDate"
      FROM "MonthlyDividendPayout"
      ORDER BY "payDate" DESC, "recordDate" DESC
      """;

  private static final RowMapper<MonthlyDividendPayout> MAPPER = MonthlyDividendPayoutQuery::mapRow;

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  /** 컬럼 위치 -> 필드. 시험이 직접 부른다(위치가 하나라도 어긋나면 값이 조용히 뒤바뀐다). */
  static MonthlyDividendPayout mapRow(ResultSet rs, int rowNum) throws SQLException {
    MonthlyDividendPayout payout = new MonthlyDividendPayout();
    payout.setId(rs.getObject(1, UUID.class));
    payout.setStockItemId(rs.getObject(2, UUID.class));
    payout.setRecordDate(rs.getObject(3, LocalDate.class));
    payout.setPayDate(rs.getObject(4, LocalDate.class));
    payout.setDistributionRatePct(rs.getBigDecimal(5));
    payout.setDividendAmountPerShare(rs.getBigDecimal(6));
    payout.setTaxableBasePerShare(rs.getBigDecimal(7));
    // pgjdbc 는 timestamptz -> Instant 직접 변환을 지원하지 않는다(TradeQuery 참고).
    OffsetDateTime createdDate = rs.getObject(8, OffsetDateTime.class);
    payout.setCreatedDate(createdDate != null ? createdDate.toInstant() : null);
    OffsetDateTime updatedDate = rs.getObject(9, OffsetDateTime.class);
    payout.setUpdatedDate(updatedDate != null ? updatedDate.toInstant() : null);
    return payout;
  }

  /** 전 종목 지급 이력(지급일 &middot; 기준일 내림차순). */
  public List<MonthlyDividendPayout> findAllByOrderByPayDateDescRecordDateDesc() {
    return namedParameterJdbcTemplate.query(ALL_SQL, Map.of(), MAPPER);
  }

  /** 여러 종목의 지급 이력을 한 번에(지급일 &middot; 기준일 내림차순). 빈 목록이면 조회하지 않는다. */
  public List<MonthlyDividendPayout> findByStockItemIdInOrderByPayDateDescRecordDateDesc(
      Collection<UUID> stockItemIds) {
    if (stockItemIds == null || stockItemIds.isEmpty()) {
      return List.of();
    }
    return namedParameterJdbcTemplate.query(
        BY_STOCK_ITEMS_SQL, Map.of("ids", stockItemIds), MAPPER);
  }
}
