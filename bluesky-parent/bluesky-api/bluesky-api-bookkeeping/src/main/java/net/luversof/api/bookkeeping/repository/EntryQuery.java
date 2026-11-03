package net.luversof.api.bookkeeping.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import net.luversof.api.bookkeeping.constant.CategoryKind;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.CategoryTotal;
import net.luversof.api.bookkeeping.domain.DailyTotal;
import net.luversof.api.bookkeeping.domain.Entry;

/** 내역 조회 쿼리: 잔액 증감 · 사용 여부 · 검색 · 일별 / 분류별 합. 모두 book_id 로 한정한다. */
@Repository
public class EntryQuery {

  private static final String COLUMNS =
      """
      "id", "book_id", "type", "entryDate", "entryTime", "amount", "toAmount", "asset_id", "toAsset_id",
      "category_id", "content", "memo", "createdBy", "createdDate"
      """;

  private final JdbcClient jdbcClient;

  public EntryQuery(@Qualifier("bookkeepingJdbcClient") JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /** 자산별 내역 증감 합. 수입 +amount, 지출 · 이체 나감 -amount, 이체 들어옴 +toAmount. 내역 없는 자산은 빠진다. */
  public Map<UUID, BigDecimal> balanceDeltas(UUID bookId) {
    var result = new HashMap<UUID, BigDecimal>();
    jdbcClient
        .sql(
            """
            SELECT "assetId", SUM("delta") AS "delta" FROM (
              SELECT "asset_id" AS "assetId",
                     CASE WHEN "type" = 'INCOME' THEN "amount" ELSE -"amount" END AS "delta"
                FROM "Entry" WHERE "book_id" = :bookId
              UNION ALL
              SELECT "toAsset_id", "toAmount" FROM "Entry" WHERE "book_id" = :bookId AND "type" = 'TRANSFER'
            ) t GROUP BY "assetId"
            """)
        .param("bookId", bookId)
        .query(
            rs -> {
              result.put(rs.getObject("assetId", UUID.class), rs.getBigDecimal("delta"));
            });
    return result;
  }

  public boolean existsByAssetId(UUID assetId) {
    return jdbcClient
        .sql(
            """
            SELECT EXISTS (SELECT 1 FROM "Entry" WHERE "asset_id" = :id OR "toAsset_id" = :id)
            """)
        .param("id", assetId)
        .query(Boolean.class)
        .single();
  }

  public boolean existsByCategoryIdIn(Collection<UUID> categoryIds) {
    return jdbcClient
        .sql("SELECT EXISTS (SELECT 1 FROM \"Entry\" WHERE \"category_id\" IN (:ids))")
        .param("ids", categoryIds)
        .query(Boolean.class)
        .single();
  }

  /**
   * 기간 검색. 최근 날짜 · 시간 먼저.
   *
   * @param q 내용 · 메모 부분 일치(대소문자 무시), null 이면 조건 없음
   * @param categoryIds 이 분류들 중 하나(대분류를 고르면 서비스가 소분류까지 넣는다), null 이면 조건 없음
   * @param assetId 나가거나 들어온 자산, null 이면 조건 없음
   * @param limit 최대 행 수
   */
  public List<Entry> search(
      UUID bookId,
      LocalDate from,
      LocalDate to,
      String q,
      Collection<UUID> categoryIds,
      UUID assetId,
      int limit) {
    var sql =
        new StringBuilder("SELECT ")
            .append(COLUMNS)
            .append(" FROM \"Entry\" WHERE \"book_id\" = :bookId")
            .append(" AND \"entryDate\" BETWEEN :from AND :to");
    var params = new HashMap<String, Object>();
    params.put("bookId", bookId);
    params.put("from", from);
    params.put("to", to);
    if (q != null && !q.isBlank()) {
      sql.append(" AND (\"content\" ILIKE :q ESCAPE '\\' OR \"memo\" ILIKE :q ESCAPE '\\')");
      params.put("q", "%" + escapeLike(q.strip()) + "%");
    }
    if (categoryIds != null) {
      sql.append(" AND \"category_id\" IN (:categoryIds)");
      params.put("categoryIds", categoryIds);
    }
    if (assetId != null) {
      sql.append(" AND (\"asset_id\" = :assetId OR \"toAsset_id\" = :assetId)");
      params.put("assetId", assetId);
    }
    sql.append(
        " ORDER BY \"entryDate\" DESC, \"entryTime\" DESC NULLS LAST, \"createdDate\" DESC LIMIT :limit");
    params.put("limit", limit);
    return jdbcClient.sql(sql.toString()).params(params).query(EntryQuery::mapEntry).list();
  }

  /** 하루 · 통화별 수입 · 지출 합. 통화는 내역 자산의 통화. 내역 없는 날은 빠진다. */
  public List<DailyTotal> dailyTotals(UUID bookId, LocalDate from, LocalDate to) {
    return jdbcClient
        .sql(
            """
            SELECT e."entryDate" AS "date", a."currency" AS "currency",
                   SUM(CASE WHEN e."type" = 'INCOME' THEN e."amount" ELSE 0 END) AS "income",
                   SUM(CASE WHEN e."type" = 'EXPENSE' THEN e."amount" ELSE 0 END) AS "expense"
              FROM "Entry" e JOIN "Asset" a ON a."id" = e."asset_id"
             WHERE e."book_id" = :bookId AND e."type" <> 'TRANSFER'
               AND e."entryDate" BETWEEN :from AND :to
             GROUP BY e."entryDate", a."currency"
             ORDER BY e."entryDate", a."currency"
            """)
        .param("bookId", bookId)
        .param("from", from)
        .param("to", to)
        .query(
            (rs, rowNum) ->
                new DailyTotal(
                    rs.getObject("date", LocalDate.class),
                    rs.getString("currency"),
                    rs.getBigDecimal("income"),
                    rs.getBigDecimal("expense")))
        .list();
  }

  /** 분류 · 통화별 합(수입 · 지출). 큰 금액 먼저. */
  public List<CategoryTotal> categoryTotals(UUID bookId, LocalDate from, LocalDate to) {
    return jdbcClient
        .sql(
            """
            SELECT e."category_id" AS "categoryId", c."kind" AS "kind", a."currency" AS "currency",
                   SUM(e."amount") AS "amount"
              FROM "Entry" e
              JOIN "Asset" a ON a."id" = e."asset_id"
              JOIN "Category" c ON c."id" = e."category_id"
             WHERE e."book_id" = :bookId AND e."type" <> 'TRANSFER'
               AND e."entryDate" BETWEEN :from AND :to
             GROUP BY e."category_id", c."kind", a."currency"
             ORDER BY "amount" DESC
            """)
        .param("bookId", bookId)
        .param("from", from)
        .param("to", to)
        .query(
            (rs, rowNum) ->
                new CategoryTotal(
                    rs.getObject("categoryId", UUID.class),
                    CategoryKind.valueOf(rs.getString("kind")),
                    rs.getString("currency"),
                    rs.getBigDecimal("amount")))
        .list();
  }

  static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static Entry mapEntry(ResultSet rs, int rowNum) throws SQLException {
    return new Entry(
        rs.getObject("id", UUID.class),
        rs.getObject("book_id", UUID.class),
        EntryType.valueOf(rs.getString("type")),
        rs.getObject("entryDate", LocalDate.class),
        rs.getObject("entryTime", LocalTime.class),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("toAmount"),
        rs.getObject("asset_id", UUID.class),
        rs.getObject("toAsset_id", UUID.class),
        rs.getObject("category_id", UUID.class),
        rs.getString("content"),
        rs.getString("memo"),
        rs.getObject("createdBy", UUID.class),
        rs.getTimestamp("createdDate").toInstant());
  }
}
