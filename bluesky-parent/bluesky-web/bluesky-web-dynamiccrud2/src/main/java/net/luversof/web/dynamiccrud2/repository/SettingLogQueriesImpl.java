package net.luversof.web.dynamiccrud2.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import net.luversof.web.dynamiccrud2.domain.SettingLog;

/** SettingLogRepository 조각: 범위 · 거르기로 id 만 고르고(정렬 · 쪽), 행은 집합 조회로 읽는다(jsonb · enum 변환 그대로) */
class SettingLogQueriesImpl implements SettingLogQueries {

  private final NamedParameterJdbcOperations jdbc;
  private final JdbcAggregateOperations aggregates;

  SettingLogQueriesImpl(NamedParameterJdbcOperations jdbc, JdbcAggregateOperations aggregates) {
    this.jdbc = jdbc;
    this.aggregates = aggregates;
  }

  /** 범위: 메뉴면 그 메뉴 + 그 메뉴 테이블의 필드 변경, 아니면 프로젝트. 둘 다 없으면 아무것도 안 고른다 */
  private static SqlWhere scope(UUID menuId, UUID tableId, UUID projectId) {
    var values = new HashMap<String, Object>();
    String condition;
    if (menuId != null) {
      values.put("menuId", menuId);
      values.put("tableId", tableId);
      condition =
          tableId == null
              ? "\"menu_id\" = :menuId"
              : "(\"menu_id\" = :menuId OR (\"target\" = 'FIELD' AND \"targetId\" = :tableId))";
    } else if (projectId != null) {
      values.put("projectId", projectId);
      condition = "\"project_id\" = :projectId";
    } else {
      condition = "1 = 0";
    }
    return new SqlWhere().always(condition, values);
  }

  private static SqlWhere where(
      UUID menuId,
      UUID tableId,
      UUID projectId,
      String username,
      String target,
      Instant fromDate,
      Instant toDate) {
    return scope(menuId, tableId, projectId)
        .when(username, "\"username\" = :username", "username")
        .when(target, "\"target\" = :target", "target")
        .when(fromDate, "\"createdDate\" >= :fromDate", "fromDate")
        .when(toDate, "\"createdDate\" < :toDate", "toDate");
  }

  @Override
  public List<SettingLog> search(
      UUID menuId,
      UUID tableId,
      UUID projectId,
      String username,
      String target,
      Instant fromDate,
      Instant toDate,
      int limit,
      long offset) {
    var where =
        where(menuId, tableId, projectId, username, target, fromDate, toDate)
            .param("limit", limit)
            .param("offset", offset);
    var ids =
        jdbc.queryForList(
            "SELECT \"id\" FROM \"SettingLog\""
                + where.sql()
                + " ORDER BY \"createdDate\" DESC LIMIT :limit OFFSET :offset",
            where.params(),
            UUID.class);
    return SqlWhere.inOrder(aggregates.findAllById(ids, SettingLog.class), ids, SettingLog::getId);
  }

  @Override
  public long countSearch(
      UUID menuId,
      UUID tableId,
      UUID projectId,
      String username,
      String target,
      Instant fromDate,
      Instant toDate) {
    var where = where(menuId, tableId, projectId, username, target, fromDate, toDate);
    var count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"SettingLog\"" + where.sql(), where.params(), Long.class);
    return count == null ? 0 : count;
  }

  @Override
  public List<String> findUsernames(UUID menuId, UUID tableId, UUID projectId) {
    var where = scope(menuId, tableId, projectId);
    return jdbc.queryForList(
        "SELECT DISTINCT \"username\" FROM \"SettingLog\"" + where.sql() + " ORDER BY \"username\"",
        where.params(),
        String.class);
  }
}
