package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;

/** JDBC DB 종류별 차이(식별자 따옴표 · 테이블 이름 · 페이징 · 시스템 스키마 · 컬럼 주석). */
public enum SqlDialect {
  POSTGRESQL("\"", "\"", Set.of("pg_catalog", "information_schema", "pg_toast")) {
    @Override
    String pagingSuffix(boolean hasOrderBy) {
      return " LIMIT :_limit OFFSET :_offset";
    }
  },
  MYSQL("`", "`", Set.of()) {
    @Override
    String pagingSuffix(boolean hasOrderBy) {
      return " LIMIT :_limit OFFSET :_offset";
    }

    /** MySQL 은 catalog 가 곧 DB 이름이고 schema 가 없다. */
    @Override
    public String qualifiedName(String catalog, String schema, String table) {
      return catalog == null ? quote(table) : quote(catalog) + "." + quote(table);
    }
  },
  MSSQL("[", "]", Set.of("sys", "INFORMATION_SCHEMA")) {
    /** OFFSET ... FETCH 는 ORDER BY 가 있어야 한다. */
    @Override
    String pagingSuffix(boolean hasOrderBy) {
      return (hasOrderBy ? "" : " ORDER BY (SELECT NULL)")
          + " OFFSET :_offset ROWS FETCH NEXT :_limit ROWS ONLY";
    }

    /** MSSQL 드라이버는 REMARKS 를 주지 않아 MS_Description 확장 속성을 직접 읽는다. */
    // shortcut: 실제 MSSQL 로는 아직 확인하지 않았다(로컬 MSSQL 없음), MSSQL 연결이 생기면 통합 테스트를 붙일 것
    @Override
    Map<String, String> columnComments(Connection connection, String schema, String table)
        throws SQLException {
      var sql =
          """
          SELECT c.name, CAST(ep.value AS NVARCHAR(4000))
          FROM sys.columns c
          JOIN sys.extended_properties ep
            ON ep.major_id = c.object_id AND ep.minor_id = c.column_id AND ep.name = 'MS_Description'
          WHERE c.object_id = OBJECT_ID(?)
          """;
      var comments = new HashMap<String, String>();
      try (var statement = connection.prepareStatement(sql)) {
        statement.setString(1, qualifiedName(null, schema, table));
        try (var resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            comments.put(resultSet.getString(1), resultSet.getString(2));
          }
        }
      }
      return comments;
    }
  };

  private final String openQuote;
  private final String closeQuote;
  private final Set<String> systemSchemas;

  SqlDialect(String openQuote, String closeQuote, Set<String> systemSchemas) {
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.systemSchemas = systemSchemas;
  }

  /** {@link java.sql.DatabaseMetaData#getDatabaseProductName()} 값으로 고른다. */
  public static SqlDialect of(String databaseProductName) {
    var name = databaseProductName == null ? "" : databaseProductName.toLowerCase();
    if (name.contains("postgresql")) {
      return POSTGRESQL;
    }
    if (name.contains("mysql") || name.contains("mariadb")) {
      return MYSQL;
    }
    if (name.contains("sql server")) {
      return MSSQL;
    }
    throw new BlueskyException(QueryErrorCode.UNSUPPORTED_DATABASE, databaseProductName);
  }

  /**
   * 이름을 늘 따옴표로 감싼다(닫는 따옴표는 두 번 써서 이스케이프). 예약어 컬럼(예: MySQL 8 의 virtual, order, user)도 깨지지 않게 - 감싸도
   * MySQL · MSSQL 은 대소문자 규칙이 같고, PostgreSQL 은 소문자 이름이면 같은 이름이다(DMT2 UDP-6559 와 같게, 2026-11-26).
   */
  public String quote(String identifier) {
    return openQuote + identifier.replace(closeQuote, closeQuote + closeQuote) + closeQuote;
  }

  public String qualifiedName(String catalog, String schema, String table) {
    return schema == null ? quote(table) : quote(schema) + "." + quote(table);
  }

  /** 쿼리 끝에 붙일 페이징 절. 값은 :_limit · :_offset 으로 바인딩한다. */
  abstract String pagingSuffix(boolean hasOrderBy);

  boolean isSystemSchema(String schema) {
    return schema != null && systemSchemas.contains(schema);
  }

  /** 컬럼 이름 → 주석. 기본은 빈 맵(드라이버 REMARKS 를 쓴다). */
  Map<String, String> columnComments(Connection connection, String schema, String table)
      throws SQLException {
    return Map.of();
  }
}
