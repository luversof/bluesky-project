package net.luversof.web.dynamiccrud2.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * 조건이 있는 것만 AND 로 잇는 WHERE 조립(이력 조회 거르기). 값이 null 이면 그 조건을 빼므로 "CAST(:x AS ...) IS NULL OR ..." 꼼수가
 * 필요 없다.
 *
 * <p>catalog 는 PostgreSQL 이고 대소문자가 섞인 이름은 큰따옴표가 필요하다. 이런 SQL 을 {@code @Query} 에 두면 STS 가 MySQL 문법으로
 * 검사해 오류로 표시하므로(2026-11-19) 동적 조회는 NamedParameterJdbcTemplate 로 실행한다.
 */
final class SqlWhere {

  private final List<String> conditions = new ArrayList<>();
  private final MapSqlParameterSource params = new MapSqlParameterSource();

  /** 값이 있을 때만 "조건" 을 붙이고 값을 name 으로 묶는다 */
  SqlWhere when(Object value, String condition, String name) {
    if (value != null) {
      conditions.add(condition);
      params.addValue(name, value instanceof Instant instant ? Timestamp.from(instant) : value);
    }
    return this;
  }

  /** 늘 붙는 조건(범위 등) */
  SqlWhere always(String condition, Map<String, Object> values) {
    conditions.add(condition);
    values.forEach(params::addValue);
    return this;
  }

  SqlWhere param(String name, Object value) {
    params.addValue(name, value);
    return this;
  }

  String sql() {
    return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
  }

  MapSqlParameterSource params() {
    return params;
  }

  /** findAllById 결과를 ids 순서(정렬된 조회 순서)로 되돌린다 */
  static <T, K> List<T> inOrder(Iterable<T> found, List<K> ids, Function<T, K> idOf) {
    var byId = new java.util.HashMap<K, T>();
    found.forEach(item -> byId.put(idOf.apply(item), item));
    return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
  }
}
