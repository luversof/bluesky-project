package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;

/** JDBC DB 종류별 차이(식별자 따옴표 · 테이블 이름 · 페이징 · 시스템 스키마 · 컬럼 주석). */
public enum SqlDialect {
  POSTGRESQL(
      "\"", "\"", "^[a-z_][a-z0-9_]*$", Set.of("pg_catalog", "information_schema", "pg_toast")) {
    @Override
    String pagingSuffix(boolean hasOrderBy) {
      return " LIMIT :_limit OFFSET :_offset";
    }
  },
  MYSQL("`", "`", "^[A-Za-z_][A-Za-z0-9_]*$", Set.of()) {
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
  MSSQL("[", "]", "^[A-Za-z_][A-Za-z0-9_]*$", Set.of("sys", "INFORMATION_SCHEMA")) {
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
  private final Pattern simpleIdentifier;
  private final Set<String> systemSchemas;

  SqlDialect(
      String openQuote, String closeQuote, String simpleIdentifier, Set<String> systemSchemas) {
    this.openQuote = openQuote;
    this.closeQuote = closeQuote;
    this.simpleIdentifier = Pattern.compile(simpleIdentifier);
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

  /** 따옴표 없이 써도 되는 이름은 그대로, 아니면 따옴표로 감싼다(닫는 따옴표는 두 번 써서 이스케이프). */
  // shortcut: 예약어(예: order, user)는 따옴표를 안 붙인다, 자동 생성 쿼리가 예약어 컬럼에서 깨지면 예약어 목록을 붙일 것
  public String quote(String identifier) {
    if (simpleIdentifier.matcher(identifier).matches()) {
      return identifier;
    }
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
