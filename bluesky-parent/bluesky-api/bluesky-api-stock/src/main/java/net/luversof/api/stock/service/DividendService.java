package net.luversof.api.stock.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;

@Service
public class DividendService {

  private final JdbcClient jdbcClient;
  private final StockItemService stockItemService;

  public DividendService(
      @Qualifier("stockJdbcClient") JdbcClient jdbcClient, StockItemService stockItemService) {
    this.jdbcClient = jdbcClient;
    this.stockItemService = stockItemService;
  }

  /**
   * 위 SELECT 의 컬럼 순서를 그대로 위치로 읽는다.
   *
   * <p>{@code query(Dividend.class)} 의 프로퍼티 매핑은 컬럼마다 리플렉션과 타입 변환을 돈다. 193 행뿐인 이 조회가 손익 시뮬레이션 스택 샘플의
   * 23% 를 차지했고 그중 23/25 가 DB 대기가 아닌 CPU 였다.
   *
   * <p>SELECT 목록을 바꾸면 이 매퍼도 함께 바꿔야 한다. 순서는 fee 가 tax 보다 앞이다.
   */
  private static final org.springframework.jdbc.core.RowMapper<Dividend> DIVIDEND_ROW_MAPPER =
      (rs, rowNum) -> {
        Dividend dividend = new Dividend();
        dividend.setId(rs.getObject(1, UUID.class));
        dividend.setAccountId(rs.getObject(2, UUID.class));
        dividend.setStockItemId(rs.getObject(3, UUID.class));
        dividend.setStockItemName(rs.getString(4));
        dividend.setType(rs.getString(5));
        int quantity = rs.getInt(6);
        dividend.setQuantity(rs.wasNull() ? null : quantity);
        dividend.setAmountPerShare(rs.getBigDecimal(7));
        dividend.setTaxPerShare(rs.getBigDecimal(8));
        dividend.setGrossAmount(rs.getBigDecimal(9));
        dividend.setFee(rs.getBigDecimal(10));
        dividend.setTax(rs.getBigDecimal(11));
        dividend.setTaxableAmount(rs.getBigDecimal(12));
        // pgjdbc 는 timestamptz -> Instant 직접 변환을 지원하지 않는다.
        java.time.OffsetDateTime recordDate = rs.getObject(13, java.time.OffsetDateTime.class);
        dividend.setRecordDate(recordDate != null ? recordDate.toInstant() : null);
        java.time.OffsetDateTime payDate = rs.getObject(14, java.time.OffsetDateTime.class);
        dividend.setPayDate(payDate != null ? payDate.toInstant() : null);
        return dividend;
      };

  @Transactional(readOnly = true)
  /** 배당 조회의 WHERE 절. 목록과 합계가 반드시 같은 조건을 쓰도록 한 곳에서 만든다. */
  private String buildDividendWhere(DividendSearchRequest request, Map<String, Object> params) {
    StringBuilder where = new StringBuilder(" WHERE a.\"user_id\" = :userId");
    params.put("userId", request.getUserId());
    if (!CollectionUtils.isEmpty(request.getAccountIdList())) {
      where.append(" AND d.\"account_id\" IN (:accountIdList)");
      params.put("accountIdList", request.getAccountIdList());
    }
    if (!CollectionUtils.isEmpty(request.getStockItemIdList())) {
      where.append(" AND d.\"stockItem_id\" IN (:stockItemIdList)");
      params.put("stockItemIdList", request.getStockItemIdList());
    }
    if (request.getStartDate() != null) {
      where.append(" AND d.\"payDate\" >= :startDate");
      params.put("startDate", Timestamp.from(request.getStartDate()));
    }
    if (request.getEndDate() != null) {
      // endDate 는 배타적이다(게이트가 "다음 날 00:00" 을 보낸다). 필터 id 조회도 < 를 쓴다.
      where.append(" AND d.\"payDate\" < :endDate");
      params.put("endDate", Timestamp.from(request.getEndDate()));
    }
    return where.toString();
  }

  /**
   * 같은 조건의 세후 배당 합계.
   *
   * <p>게이트 요약 화면이 계좌/종목 필터를 건 상태에서도 배당만 전체 합계를 쓰고 있었다(실측: 계좌 하나를 골라도 '누적 확정 수익' 에 <b>전 계좌 배당</b>이
   * 그대로 더해져, KB증권 위탁 값이 그 계좌 실제 배당의 <b>6.1 배</b>로 표시됐다). 실현손익과 같은 조건으로 집계해 두 항이 어긋나지 않게 한다.
   */
  public java.math.BigDecimal sumNetAmount(DividendSearchRequest request) {
    if (request.getUserId() == null) {
      StockErrorCode.NOT_EXIST_USER_ID.throwException();
    }
    Map<String, Object> params = new HashMap<>();
    String where = buildDividendWhere(request, params);
    String sql =
        "SELECT COALESCE(SUM(COALESCE(d.\"grossAmount\", 0) - COALESCE(d.\"tax\", 0)"
            + " - COALESCE(d.\"fee\", 0)), 0) FROM \"Dividend\" d"
            + " JOIN \"Account\" a ON d.\"account_id\" = a.\"id\""
            + where;
    return jdbcClient
        .sql(sql)
        .params(params)
        .query(java.math.BigDecimal.class)
        .optional()
        .orElse(java.math.BigDecimal.ZERO);
  }

  /**
   * 같은 조건의 세후 배당 합계를 <b>종목별로</b> 나눈 것.
   *
   * <p>요약 화면의 "수익권 종목 비율"은 종목마다 손익 부호를 세는데, 그 손익({@code totalProfitNet})에는 배당이 들어 있지 않다. 배당이 큰 종목은
   * 실제로 이익인데 손실로 세어진다(실측 2026-08-24: TIGER 리츠부동산인프라는 실현+평가가 손실인데 배당이 그 <b>1.75 배</b>라 합치면 이익이다 - 42
   * 종목 중 1 종목이 뒤집혀 76.19% 대신 78.57%).
   *
   * <p>합계 하나만 주는 {@code sumNetAmount} 로는 종목별로 가를 수 없고, 목록을 통째로 받으면 예전에 걷어낸 79,919 바이트가 그대로 돌아온다. 같은
   * WHERE 절로 종목별 합계만 집계한다(실측: 이 사용자 18행).
   *
   * <p>합계는 {@code sumNetAmount} 와 같아야 한다 - 값들의 합이 곧 그 값이다.
   */
  public Map<UUID, java.math.BigDecimal> sumNetAmountByStockItem(DividendSearchRequest request) {
    if (request.getUserId() == null) {
      StockErrorCode.NOT_EXIST_USER_ID.throwException();
    }
    Map<String, Object> params = new HashMap<>();
    String where = buildDividendWhere(request, params);
    String sql =
        "SELECT d.\"stockItem_id\" AS stock_item_id,"
            + " COALESCE(SUM(COALESCE(d.\"grossAmount\", 0) - COALESCE(d.\"tax\", 0)"
            + " - COALESCE(d.\"fee\", 0)), 0) AS net_total"
            + " FROM \"Dividend\" d"
            + " JOIN \"Account\" a ON d.\"account_id\" = a.\"id\""
            + where
            + " GROUP BY d.\"stockItem_id\"";
    Map<UUID, java.math.BigDecimal> result = new java.util.LinkedHashMap<>();
    jdbcClient
        .sql(sql)
        .params(params)
        .query(
            (rs, rowNum) -> {
              UUID stockItemId = rs.getObject(1, UUID.class);
              if (stockItemId != null) {
                result.put(stockItemId, rs.getBigDecimal(2));
              }
              return stockItemId;
            })
        .list();
    return result;
  }

  public List<Dividend> findDividends(DividendSearchRequest request) {
    if (request.getUserId() == null) {
      StockErrorCode.NOT_EXIST_USER_ID.throwException();
    }

    StringBuilder sql =
        new StringBuilder()
            .append(
                "SELECT d.\"id\" as \"id\", d.\"account_id\" as \"accountId\", d.\"stockItem_id\" as \"stockItemId\", si.\"name\" as \"stockItemName\", d.\"type\" as \"type\", d.\"quantity\" as \"quantity\", ")
            .append(
                "d.\"amountPerShare\" as \"amountPerShare\", d.\"taxPerShare\" as \"taxPerShare\", d.\"grossAmount\" as \"grossAmount\", ")
            .append(
                "d.\"fee\" as \"fee\", d.\"tax\" as \"tax\", d.\"taxableAmount\" as \"taxableAmount\", d.\"recordDate\" as \"recordDate\", d.\"payDate\" as \"payDate\" ")
            .append("FROM \"Dividend\" d ")
            .append("JOIN \"Account\" a ON d.\"account_id\" = a.\"id\" ")
            .append("LEFT JOIN \"StockItem\" si ON d.\"stockItem_id\" = si.\"id\" ");

    Map<String, Object> params = new HashMap<>();
    sql.append(buildDividendWhere(request, params));
    sql.append(" ORDER BY d.\"payDate\" DESC, d.\"id\" DESC");

    var result = jdbcClient.sql(sql.toString()).params(params).query(DIVIDEND_ROW_MAPPER).list();

    // Fill missing stockItemId by looking up StockItem by name
    for (var dividend : result) {
      if (dividend != null
          && dividend.getStockItemId() == null
          && dividend.getStockItemName() != null) {
        try {
          StockItem si = stockItemService.findByName(dividend.getStockItemName());
          if (si != null && si.getId() != null) {
            dividend.setStockItemId(si.getId());
          }
        } catch (Exception ignored) {
          // ignore lookup failures
        }
      }
    }

    return result;
  }
}
