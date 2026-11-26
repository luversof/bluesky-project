package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.transaction.support.TransactionTemplate;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.adapter.UntypedText;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/** JDBC 3종(PostgreSQL · MySQL/MariaDB · MSSQL) 어댑터. 차이는 {@link SqlDialect} 에 둔다. */
public class JdbcDatabaseAdapter implements DatabaseAdapter {

  private final DataSource dataSource;
  private final SqlDialect dialect;
  private final Dynamiccrud2Properties properties;

  public JdbcDatabaseAdapter(
      DataSource dataSource, SqlDialect dialect, Dynamiccrud2Properties properties) {
    this.dataSource = dataSource;
    this.dialect = dialect;
    this.properties = properties;
  }

  public SqlDialect getDialect() {
    return dialect;
  }

  @Override
  public String databaseType() {
    return switch (dialect) {
      case POSTGRESQL -> "PostgreSQL";
      case MYSQL -> "MySQL · MariaDB";
      case MSSQL -> "SQL Server";
    };
  }

  @Override
  public List<TableRef> listTables() {
    return withConnection(connection -> JdbcMetadataReader.listTables(connection, dialect));
  }

  @Override
  public TableMeta readTable(TableRef ref) {
    return withConnection(connection -> JdbcMetadataReader.readTable(connection, dialect, ref));
  }

  @Override
  public String generate(QueryKind kind, TableMeta table) {
    return DefaultQueryGenerator.generate(kind, table, dialect);
  }

  @Override
  public void validate(String query, QueryKind kind) {
    QueryText.validate(query, kind);
  }

  /**
   * 실행하지 않는 검사: PostgreSQL · MySQL 은 EXPLAIN, MSSQL 은 SET NOEXEC ON(컴파일만). 파라미터는 모두 NULL. 예외는
   * JdbcTemplate 이 Spring 예외로 바꾼다(문법 · 없는 이름 = BadSqlGrammarException).
   */
  // shortcut: MSSQL NOEXEC 는 실제 MSSQL 로 아직 확인하지 않았다, MSSQL 연결이 생기면 통합 테스트를 붙일 것
  @Override
  public void check(String query) {
    var sql = QueryText.trimTrailingSemicolon(query);
    var parsed = NamedParameterUtils.parseSqlStatement(sql);
    var source = new MapSqlParameterSource();
    QueryText.parameterNames(sql).forEach(name -> source.addValue(name, null));
    var positional = NamedParameterUtils.substituteNamedParameters(parsed, source);
    var count = NamedParameterUtils.buildValueArray(parsed, source, null).length;
    var template = new JdbcTemplate(dataSource);
    template.setQueryTimeout(properties.queryTimeoutSeconds());
    template.execute(
        (org.springframework.jdbc.core.ConnectionCallback<Void>)
            connection -> {
              if (dialect == SqlDialect.MSSQL) {
                try (var statement = connection.createStatement()) {
                  statement.execute("SET NOEXEC ON");
                }
                try {
                  runWithNulls(connection, positional, count);
                } finally {
                  try (var statement = connection.createStatement()) {
                    statement.execute("SET NOEXEC OFF");
                  }
                }
              } else {
                runWithNulls(connection, "EXPLAIN " + positional, count);
              }
              return null;
            });
  }

  private static void runWithNulls(java.sql.Connection connection, String sql, int count)
      throws SQLException {
    try (var statement = connection.prepareStatement(sql)) {
      for (int i = 1; i <= count; i++) {
        statement.setNull(i, java.sql.Types.NULL);
      }
      statement.execute();
    }
  }

  @Override
  public List<String> parameterNames(String query) {
    return QueryText.parameterNames(query);
  }

  @Override
  public ReadAnalysis analyzeRead(String query) {
    return ReadQueryRewriter.analyze(query);
  }

  @Override
  public ReadResult read(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      int page,
      int size) {
    var limit = Math.max(1, Math.min(size, properties.pageMaxRows()));
    var bind = bindable(params);
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, Math.max(0, page) * limit);
    var template = template(limit);
    var countQuery = ReadQueryRewriter.countQuery(query, dialect, emptyOptionalParams, filters);
    var pageQuery = ReadQueryRewriter.pageQuery(query, dialect, emptyOptionalParams, filters, sort);
    return readOnly(() -> {
      var total = template.queryForObject(countQuery, bind, Long.class);
      return template.query(pageQuery, bind, extractor(total == null ? 0 : total));
    });
  }

  // shortcut: MySQL Connector/J 는 useCursorFetch=true 가 없으면 fetchSize 를 무시하고 한 번에 다 받는다, MySQL 메뉴의
  // 엑셀이 커지면 연결 설정에 넣을 것
  private static final int EXPORT_FETCH_SIZE = 1000;

  @Override
  public void export(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      RowSink sink) {
    var limit = properties.excelMaxRows();
    var bind = bindable(params);
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, 0);
    var pageQuery = ReadQueryRewriter.pageQuery(query, dialect, emptyOptionalParams, filters, sort);
    var template = template(limit);
    template.getJdbcTemplate().setFetchSize(EXPORT_FETCH_SIZE);
    readOnly(() ->
        template.query(
            pageQuery,
            bind,
            (ResultSetExtractor<Void>)
                rs -> {
                  forEachRow(rs, sink);
                  return null;
                }));
  }

  @Override
  public List<Object> distinctValues(TableRef table, String column, int limit) {
    var quoted = dialect.quote(column);
    var sql =
        "SELECT DISTINCT "
            + quoted
            + " FROM "
            + dialect.qualifiedName(table.catalog(), table.schema(), table.name())
            + " WHERE "
            + quoted
            + " IS NOT NULL ORDER BY "
            + quoted
            + dialect.pagingSuffix(true);
    var bind = new HashMap<String, Object>();
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit + 1);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, 0);
    return readOnly(() -> template(limit + 1).query(sql, bind, extractor(-1))).rows().stream()
        .map(row -> row.get(0))
        .toList();
  }

  @Override
  public List<Object> currentRow(TableRef table, List<String> columns, Map<String, Object> keys) {
    var bind = new HashMap<String, Object>();
    var where = new ArrayList<String>();
    for (var key : keys.entrySet()) {
      // 컬럼 이름은 파라미터 이름으로 못 쓸 수 있어(공백 · 한글) 번호로 바인딩한다
      var param = "k" + bind.size();
      bind.put(param, key.getValue());
      where.add(dialect.quote(key.getKey()) + " = :" + param);
    }
    var sql =
        "SELECT "
            + String.join(", ", columns.stream().map(dialect::quote).toList())
            + " FROM "
            + dialect.qualifiedName(table.catalog(), table.schema(), table.name())
            + " WHERE "
            + String.join(" AND ", where);
    var rows = readOnly(() -> template(2).query(sql, bindable(bind), extractor(-1))).rows();
    return rows.size() == 1 ? rows.get(0) : null;
  }

  /**
   * 조회는 읽기 전용 트랜잭션에서 돌린다. 조회로 저장된 쓰기 CTE(WITH x AS (DELETE ...) SELECT) · 부작용 함수가 대상 DB 를 바꾸지 못하게
   * 한다. PostgreSQL · MySQL 드라이버는 Connection.setReadOnly 를 서버에 READ ONLY 로 보낸다.
   */
  // shortcut: MSSQL 드라이버는 setReadOnly 를 무시한다, MSSQL 연결이 생기면 읽기 전용 DB 계정으로 막을 것
  private <T> T readOnly(java.util.function.Supplier<T> work) {
    var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    transaction.setReadOnly(true);
    return transaction.execute(status -> work.get());
  }

  @Override
  public ReadResult sample(
      String query, Map<String, Object> params, Set<String> emptyOptionalParams, int size) {
    return firstRows(
        query,
        params,
        emptyOptionalParams,
        List.of(),
        null,
        Math.max(1, Math.min(size, properties.pageMaxRows())));
  }

  /** 첫 행부터 limit 행, 건수는 세지 않는다(total = -1) */
  private ReadResult firstRows(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      int limit) {
    var bind = bindable(params);
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, 0);
    var pageQuery = ReadQueryRewriter.pageQuery(query, dialect, emptyOptionalParams, filters, sort);
    return readOnly(() -> template(limit).query(pageQuery, bind, extractor(-1)));
  }

  private static ResultSetExtractor<ReadResult> extractor(long total) {
    return rs -> {
      var labels = new ArrayList<String>();
      var rows = new ArrayList<List<Object>>();
      forEachRow(
          rs,
          new RowSink() {
            @Override
            public void labels(List<String> names) {
              labels.addAll(names);
            }

            @Override
            public void row(List<Object> values) {
              rows.add(values);
            }
          });
      return new ReadResult(labels, rows, total);
    };
  }

  private static void forEachRow(java.sql.ResultSet rs, RowSink sink) throws SQLException {
    var meta = rs.getMetaData();
    var count = meta.getColumnCount();
    var labels = new ArrayList<String>(count);
    for (int i = 1; i <= count; i++) {
      labels.add(JdbcUtils.lookupColumnName(meta, i));
    }
    sink.labels(labels);
    while (rs.next()) {
      var row = new ArrayList<Object>(count);
      for (int i = 1; i <= count; i++) {
        row.add(JdbcUtils.getResultSetValue(rs, i));
      }
      sink.row(row);
    }
  }

  @Override
  public int[] modify(String query, List<Map<String, Object>> rows) {
    var template = template(0);
    var sql = QueryText.trimTrailingSemicolon(query);
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(status -> {
          var counts = new int[rows.size()];
          for (int i = 0; i < rows.size(); i++) {
            counts[i] = template.update(sql, bindable(rows.get(i)));
          }
          return counts;
        });
  }

  /**
   * PostgreSQL: 값과 무관한 일반 계획(genericPlan, 12 부터) - 안 되면 NULL 을 넣어 본 계획(approximate). MySQL ·
   * MariaDB: FORMAT=TREE (MySQL 8.0.16 부터) - 안 되면 표 EXPLAIN 을 줄로, 파라미터는 NULL(approximate). MSSQL 은
   * SHOWPLAN 이 따로 일괄 처리여야 해 지원하지 않는다.
   */
  // shortcut: MSSQL 실행 계획은 없다(SET SHOWPLAN_TEXT ON 을 따로 보내야 함), MSSQL 연결이 생기면 붙일 것
  @Override
  public Explain explain(String query) {
    if (dialect == SqlDialect.MSSQL) {
      throw new UnsupportedOperationException();
    }
    var sql = QueryText.trimTrailingSemicolon(query);
    var hasParams = !QueryText.parameterNames(sql).isEmpty();
    // 트랜잭션 없이 시도마다 따로: EXPLAIN(ANALYZE 없음)은 실행하지 않고, PostgreSQL 은 한 번 실패한 트랜잭션을 더 쓰지 못한다
    var template = new JdbcTemplate(dataSource);
    template.setQueryTimeout(properties.queryTimeoutSeconds());
    if (dialect == SqlDialect.POSTGRESQL) {
      if (!hasParams) {
        return new Explain(planLines(template, "EXPLAIN " + sql, List.of()), false);
      }
      try {
        return new Explain(genericPlan(template, sql), false);
      } catch (org.springframework.dao.DataAccessException e) {
        // 12 전 PostgreSQL(plan_cache_mode 없음) · 파라미터 타입을 정할 수 없는 쿼리
      }
    } else {
      try {
        return new Explain(planLines(template, "EXPLAIN FORMAT=TREE " + sql, List.of()), hasParams);
      } catch (org.springframework.dao.DataAccessException e) {
        // MariaDB · 옛 MySQL: FORMAT=TREE 를 모른다
      }
    }
    return new Explain(planLines(template, "EXPLAIN " + sql, List.of()), hasParams);
  }

  /**
   * PostgreSQL 값과 무관한 일반 계획: PREPARE(:이름 → $n) 뒤 plan_cache_mode = force_generic_plan 으로 EXPLAIN
   * EXECUTE(NULL …). 일반 계획은 넣은 값을 보지 않아 NULL 을 넣어도 "One-Time Filter: false" 로 접히지 않는다. EXPLAIN
   * (GENERIC_PLAN) 은 드라이버가 확장 프로토콜이라 $n 바인드 오류가 나 쓰지 않는다. 연결은 풀로 돌아가므로 준비한 문장 · 설정을 되돌린다.
   */
  private static List<String> genericPlan(JdbcTemplate template, String sql) {
    var count = QueryText.parameterNames(sql).size();
    var nulls = String.join(", ", java.util.Collections.nCopies(count, "NULL"));
    return template.execute(
        (org.springframework.jdbc.core.ConnectionCallback<List<String>>)
            connection -> {
              try (var statement = connection.createStatement()) {
                statement.execute("SET plan_cache_mode = force_generic_plan");
                try {
                  statement.execute(
                      "PREPARE dc2_explain AS " + QueryText.toNumberedParameters(sql));
                  try (var rs =
                      statement.executeQuery("EXPLAIN EXECUTE dc2_explain(" + nulls + ")")) {
                    var lines = new ArrayList<String>();
                    while (rs.next()) {
                      lines.add(rs.getString(1));
                    }
                    return lines;
                  } finally {
                    statement.execute("DEALLOCATE dc2_explain");
                  }
                } finally {
                  statement.execute("RESET plan_cache_mode");
                }
              }
            });
  }

  /**
   * 계획 결과를 줄로: 한 열이면 그 값(여러 줄이면 나눠서), 여러 열(MySQL 표 EXPLAIN)이면 "열=값" 을 이어서.
   *
   * @param nulls null 이면 :이름이 없는 SQL 을 그대로(Statement) 보낸다, 아니면 :이름을 모두 NULL 로 넣는다
   */
  private static List<String> planLines(JdbcTemplate template, String sql, List<Object> nulls) {
    org.springframework.jdbc.core.ResultSetExtractor<List<String>> extractor =
        rs -> {
          var lines = new ArrayList<String>();
          var meta = rs.getMetaData();
          var count = meta.getColumnCount();
          while (rs.next()) {
            if (count == 1) {
              var value = rs.getString(1);
              if (value != null) {
                lines.addAll(value.lines().toList());
              }
            } else {
              var parts = new ArrayList<String>();
              for (int i = 1; i <= count; i++) {
                var value = rs.getString(i);
                if (value != null) {
                  parts.add(JdbcUtils.lookupColumnName(meta, i) + "=" + value);
                }
              }
              lines.add(String.join("  ", parts));
            }
          }
          return lines;
        };
    if (nulls == null) {
      return template.query(sql, extractor);
    }
    var source = new MapSqlParameterSource();
    QueryText.parameterNames(sql).forEach(name -> source.addValue(name, null));
    return new NamedParameterJdbcTemplate(template).query(sql, source, extractor);
  }

  @Override
  public int[] insertRows(TableRef table, List<Map<String, Object>> rows) {
    var template = template(0);
    var name = dialect.qualifiedName(table.catalog(), table.schema(), table.name());
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(status -> {
          var counts = new int[rows.size()];
          for (int i = 0; i < rows.size(); i++) {
            // 컬럼 이름은 파라미터 이름으로 못 쓸 수 있어(공백 · 한글) 번호로 바인딩한다
            var bind = new HashMap<String, Object>();
            var columns = new ArrayList<String>();
            var params = new ArrayList<String>();
            for (var entry : rows.get(i).entrySet()) {
              var param = "c" + bind.size();
              bind.put(param, entry.getValue());
              columns.add(dialect.quote(entry.getKey()));
              params.add(":" + param);
            }
            var sql =
                "INSERT INTO "
                    + name
                    + " ("
                    + String.join(", ", columns)
                    + ") VALUES ("
                    + String.join(", ", params)
                    + ")";
            counts[i] = template.update(sql, bindable(bind));
          }
          return counts;
        });
  }

  /**
   * 바인딩 값 복사본: {@link UntypedText} 를 이 DB 에 맞게 - PostgreSQL 은 타입 없이(서버가 컬럼 타입으로), 다른 DB 는 글자 그대로
   */
  private Map<String, Object> bindable(Map<String, ?> values) {
    var result = new HashMap<String, Object>();
    values.forEach((name, value) ->
        result.put(
            name,
            value instanceof UntypedText text
                ? (dialect == SqlDialect.POSTGRESQL
                    ? new SqlParameterValue(Types.OTHER, text.value())
                    : text.value())
                : value));
    return result;
  }

  /** 타임아웃 · 최대 행 수를 건 템플릿. maxRows 0 = 제한 없음 */
  private NamedParameterJdbcTemplate template(int maxRows) {
    var jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setQueryTimeout(properties.queryTimeoutSeconds());
    jdbcTemplate.setMaxRows(maxRows);
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  private interface ConnectionCallback<T> {
    T apply(java.sql.Connection connection) throws SQLException;
  }

  private <T> T withConnection(ConnectionCallback<T> callback) {
    return new JdbcTemplate(dataSource).execute(callback::apply);
  }
}
