package net.luversof.web.dynamiccrud2.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import net.luversof.web.dynamiccrud2.domain.ChangeLog;

/**
 * ChangeLogRepository 조각: 걸러 id 만 고르고(정렬 · 쪽), 행은 집합 조회로 읽어 jsonb 변환을 그대로 쓴다.
 * idx_changeLog_menuId_createdDate 를 탄다.
 */
class ChangeLogQueriesImpl implements ChangeLogQueries {

  private final NamedParameterJdbcOperations jdbc;
  private final JdbcAggregateOperations aggregates;

  ChangeLogQueriesImpl(NamedParameterJdbcOperations jdbc, JdbcAggregateOperations aggregates) {
    this.jdbc = jdbc;
    this.aggregates = aggregates;
  }

  private static SqlWhere where(
      UUID menuId, String username, UUID queryId, Instant fromDate, Instant toDate) {
    return new SqlWhere()
        .always("\"menu_id\" = :menuId", Map.of("menuId", menuId))
        .when(username, "\"username\" = :username", "username")
        .when(queryId, "\"menuQuery_id\" = :queryId", "queryId")
        .when(fromDate, "\"createdDate\" >= :fromDate", "fromDate")
        .when(toDate, "\"createdDate\" < :toDate", "toDate");
  }

  @Override
  public List<ChangeLog> search(
      UUID menuId,
      String username,
      UUID queryId,
      Instant fromDate,
      Instant toDate,
      int limit,
      long offset) {
    var where =
        where(menuId, username, queryId, fromDate, toDate)
            .param("limit", limit)
            .param("offset", offset);
    var ids =
        jdbc.queryForList(
            "SELECT \"id\" FROM \"ChangeLog\""
                + where.sql()
                + " ORDER BY \"createdDate\" DESC LIMIT :limit OFFSET :offset",
            where.params(),
            UUID.class);
    return SqlWhere.inOrder(aggregates.findAllById(ids, ChangeLog.class), ids, ChangeLog::getId);
  }

  @Override
  public long countSearch(
      UUID menuId, String username, UUID queryId, Instant fromDate, Instant toDate) {
    var where = where(menuId, username, queryId, fromDate, toDate);
    var count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"ChangeLog\"" + where.sql(), where.params(), Long.class);
    return count == null ? 0 : count;
  }

  @Override
  public List<String> findUsernames(UUID menuId) {
    return jdbc.queryForList(
        "SELECT DISTINCT \"username\" FROM \"ChangeLog\" WHERE \"menu_id\" = :menuId ORDER BY \"username\"",
        Map.of("menuId", menuId),
        String.class);
  }
}
