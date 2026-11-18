package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.luversof.web.dynamiccrud2.adapter.ColumnMeta;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;

/** {@link DatabaseMetaData} 로 테이블 목록 · 컬럼 · PK · unique · 인덱스를 읽는다(벤더 공통). */
final class JdbcMetadataReader {

  private static final String[] TABLE_TYPES = {"TABLE", "VIEW"};

  private JdbcMetadataReader() {}

  /** 연결의 현재 catalog 에서 시스템 스키마를 뺀 테이블 · 뷰 목록 */
  static List<TableRef> listTables(Connection connection, SqlDialect dialect) throws SQLException {
    var tables = new ArrayList<TableRef>();
    try (var rs =
        connection.getMetaData().getTables(connection.getCatalog(), null, "%", TABLE_TYPES)) {
      while (rs.next()) {
        var schema = rs.getString("TABLE_SCHEM");
        if (!dialect.isSystemSchema(schema)) {
          tables.add(new TableRef(rs.getString("TABLE_CAT"), schema, rs.getString("TABLE_NAME")));
        }
      }
    }
    return tables;
  }

  static TableMeta readTable(Connection connection, SqlDialect dialect, TableRef ref)
      throws SQLException {
    var meta = connection.getMetaData();
    var comments = dialect.columnComments(connection, ref.schema(), ref.name());
    var columns = new ArrayList<ColumnMeta>();
    try (var rs = meta.getColumns(ref.catalog(), ref.schema(), ref.name(), "%")) {
      while (rs.next()) {
        var name = rs.getString("COLUMN_NAME");
        var sizeValue = rs.getInt("COLUMN_SIZE");
        Integer size = rs.wasNull() ? null : sizeValue;
        var autoIncrement =
            "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"))
                || "YES".equalsIgnoreCase(rs.getString("IS_GENERATEDCOLUMN"));
        var remarks = rs.getString("REMARKS");
        columns.add(
            new ColumnMeta(
                name,
                rs.getInt("DATA_TYPE"),
                rs.getString("TYPE_NAME"),
                size,
                rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                rs.getString("COLUMN_DEF"),
                autoIncrement,
                remarks == null || remarks.isBlank() ? comments.get(name) : remarks));
      }
    }

    var primaryKey = new TreeMap<Integer, String>();
    try (var rs = meta.getPrimaryKeys(ref.catalog(), ref.schema(), ref.name())) {
      while (rs.next()) {
        primaryKey.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME"));
      }
    }

    var columnNames = new HashSet<String>();
    columns.forEach(c -> columnNames.add(c.name()));
    // 인덱스 이름 → (순서 → 컬럼). 부분 인덱스(FILTER_CONDITION) · 식 인덱스는 키로 쓰지 않는다
    Map<String, TreeMap<Integer, String>> uniqueIndexes = new LinkedHashMap<>();
    var excludedIndexes = new HashSet<String>();
    var leading = new HashSet<String>();
    // 화면에 보일 인덱스 전체: 이름 → 순서 → 컬럼(식이면 DB 가 준 글자)
    Map<String, TreeMap<Integer, String>> allIndexes = new LinkedHashMap<>();
    var nonUnique = new HashSet<String>();
    var partialIndexes = new HashSet<String>();
    var expressionIndexes = new HashSet<String>();
    try (var rs = meta.getIndexInfo(ref.catalog(), ref.schema(), ref.name(), false, true)) {
      while (rs.next()) {
        var indexName = rs.getString("INDEX_NAME");
        var column = rs.getString("COLUMN_NAME");
        if (indexName == null || rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
          continue;
        }
        var partial = rs.getString("FILTER_CONDITION") != null;
        var expression = column == null || !columnNames.contains(column);
        var ordinal = rs.getInt("ORDINAL_POSITION");
        allIndexes
            .computeIfAbsent(indexName, k -> new TreeMap<>())
            .put(ordinal, column == null ? "(식)" : column);
        if (rs.getBoolean("NON_UNIQUE")) {
          nonUnique.add(indexName);
        }
        if (partial) {
          partialIndexes.add(indexName);
        }
        if (expression) {
          expressionIndexes.add(indexName);
        }
        if (partial || expression) {
          excludedIndexes.add(indexName);
        }
        if (ordinal == 1 && !partial && !expression) {
          leading.add(column);
        }
        if (!rs.getBoolean("NON_UNIQUE")) {
          uniqueIndexes.computeIfAbsent(indexName, k -> new TreeMap<>()).put(ordinal, column);
        }
      }
    }
    var pk = List.copyOf(primaryKey.values());
    var uniqueKeys = new ArrayList<List<String>>();
    uniqueIndexes.forEach((indexName, keyColumns) -> {
      var key = List.copyOf(keyColumns.values());
      if (!excludedIndexes.contains(indexName) && !key.equals(pk)) {
        uniqueKeys.add(key);
      }
    });
    pk.stream().findFirst().ifPresent(leading::add);
    var indexes = new ArrayList<TableMeta.IndexInfo>();
    allIndexes.forEach((indexName, indexColumns) -> {
      var columnList = List.copyOf(indexColumns.values());
      var unique = !nonUnique.contains(indexName);
      indexes.add(
          new TableMeta.IndexInfo(
              indexName,
              columnList,
              unique && !pk.isEmpty() && columnList.equals(pk),
              unique,
              partialIndexes.contains(indexName),
              expressionIndexes.contains(indexName)));
    });
    return new TableMeta(ref, columns, pk, uniqueKeys, leading, indexes);
  }
}
