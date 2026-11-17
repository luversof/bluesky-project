package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.util.List;
import java.util.stream.Collectors;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.ColumnMeta;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/** 테이블 정보로 기본 C/R/U/D 쿼리를 만든다. 사용자가 편집하기 전 출발점이다. */
public final class DefaultQueryGenerator {

  private DefaultQueryGenerator() {}

  public static String generate(QueryKind kind, TableMeta table, SqlDialect dialect) {
    var name =
        dialect.qualifiedName(table.ref().catalog(), table.ref().schema(), table.ref().name());
    return switch (kind) {
      case READ -> read(table, dialect, name);
      case INSERT -> insert(table, dialect, name);
      case UPDATE -> update(table, dialect, name);
      case DELETE -> "DELETE FROM " + name + where(table, dialect);
    };
  }

  /** 파라미터 이름. 이름에 쓸 수 없는 글자는 _ 로 바꾼다. */
  // shortcut: 바꾼 결과가 다른 컬럼 이름과 겹치는 경우는 막지 않는다, 실제로 겹치면 번호를 붙일 것
  static String parameterName(String columnName) {
    var name = columnName.replaceAll("\\W", "_");
    return Character.isDigit(name.charAt(0)) ? "_" + name : name;
  }

  private static String read(TableMeta table, SqlDialect dialect, String name) {
    var columns =
        table.columns().stream()
            .map(c -> dialect.quote(c.name()))
            .collect(Collectors.joining(", "));
    var sql = "SELECT " + columns + " FROM " + name;
    var key = table.rowKey();
    if (!key.isEmpty()) {
      sql += " ORDER BY " + key.stream().map(dialect::quote).collect(Collectors.joining(", "));
    }
    return sql;
  }

  private static String insert(TableMeta table, SqlDialect dialect, String name) {
    var columns = table.columns().stream().filter(c -> !c.autoIncrement()).toList();
    return "INSERT INTO "
        + name
        + " ("
        + columns.stream().map(c -> dialect.quote(c.name())).collect(Collectors.joining(", "))
        + ") VALUES ("
        + columns.stream().map(c -> ":" + parameterName(c.name())).collect(Collectors.joining(", "))
        + ")";
  }

  private static String update(TableMeta table, SqlDialect dialect, String name) {
    var key = rowKey(table);
    List<ColumnMeta> columns =
        table.columns().stream()
            .filter(c -> !c.autoIncrement() && !key.contains(c.name()))
            .toList();
    return "UPDATE "
        + name
        + " SET "
        + columns.stream()
            .map(c -> dialect.quote(c.name()) + " = :" + parameterName(c.name()))
            .collect(Collectors.joining(", "))
        + where(table, dialect);
  }

  private static String where(TableMeta table, SqlDialect dialect) {
    return " WHERE "
        + rowKey(table).stream()
            .map(k -> dialect.quote(k) + " = :" + parameterName(k))
            .collect(Collectors.joining(" AND "));
  }

  private static List<String> rowKey(TableMeta table) {
    var key = table.rowKey();
    if (key.isEmpty()) {
      throw new BlueskyException(QueryErrorCode.NO_ROW_KEY, table.ref().name());
    }
    return key;
  }
}
