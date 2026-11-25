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

  /** 바인딩 값 복사본: {@link UntypedText} 를 이 DB 에 맞게 - PostgreSQL 은 타입 없이(서버가 컬럼 타입으로), 다른 DB 는 글자 그대로 */
  private Map<String, Object> bindable(Map<String, ?> values) {
    var result = new HashMap<String, Object>();
    values.forEach(
        (name, value) ->
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
