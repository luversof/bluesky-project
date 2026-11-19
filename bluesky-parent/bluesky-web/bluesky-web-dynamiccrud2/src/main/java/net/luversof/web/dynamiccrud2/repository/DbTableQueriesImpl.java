package net.luversof.web.dynamiccrud2.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/** DbTableRepository 조각: 큰따옴표 이름을 쓰는 SQL 을 @Query 밖에 둔다(STS 가 MySQL 로 검사해 오류 표시) */
class DbTableQueriesImpl implements DbTableQueries {

  private final NamedParameterJdbcOperations jdbc;

  DbTableQueriesImpl(NamedParameterJdbcOperations jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> findIdByLocation(
      String connectionName, String catalogName, String schemaName, String tableName) {
    var params =
        new MapSqlParameterSource()
            .addValue("connectionName", connectionName)
            .addValue("catalogName", catalogName, java.sql.Types.VARCHAR)
            .addValue("schemaName", schemaName, java.sql.Types.VARCHAR)
            .addValue("tableName", tableName);
    return jdbc
        .queryForList(
            "SELECT \"id\" FROM \"DbTable\" WHERE \"connectionName\" = :connectionName"
                + " AND COALESCE(\"catalogName\", '') = COALESCE(:catalogName, '')"
                + " AND COALESCE(\"schemaName\", '') = COALESCE(:schemaName, '')"
                + " AND \"tableName\" = :tableName",
            params,
            UUID.class)
        .stream()
        .findFirst();
  }
}
