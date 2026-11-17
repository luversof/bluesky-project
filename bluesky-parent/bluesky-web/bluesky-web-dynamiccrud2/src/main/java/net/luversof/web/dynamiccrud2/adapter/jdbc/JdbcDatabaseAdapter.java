package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.transaction.support.TransactionTemplate;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;
import net.luversof.web.dynamiccrud2.setting.domain.QueryKind;

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
      Sort sort,
      int page,
      int size) {
    var limit = Math.max(1, Math.min(size, properties.pageMaxRows()));
    var bind = new HashMap<String, Object>(params);
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, Math.max(0, page) * limit);
    var template = template(limit);
    var total =
        template.queryForObject(
            ReadQueryRewriter.countQuery(query, emptyOptionalParams), bind, Long.class);
    var pageQuery = ReadQueryRewriter.pageQuery(query, dialect, emptyOptionalParams, sort);
    return template.query(pageQuery, bind, extractor(total == null ? 0 : total));
  }

  @Override
  public ReadResult export(
      String query, Map<String, Object> params, Set<String> emptyOptionalParams, Sort sort) {
    var limit = properties.excelMaxRows();
    var bind = new HashMap<String, Object>(params);
    bind.put(ReadQueryRewriter.LIMIT_PARAM, limit);
    bind.put(ReadQueryRewriter.OFFSET_PARAM, 0);
    var pageQuery = ReadQueryRewriter.pageQuery(query, dialect, emptyOptionalParams, sort);
    return template(limit).query(pageQuery, bind, extractor(-1));
  }

  private static ResultSetExtractor<ReadResult> extractor(long total) {
    return rs -> {
      var meta = rs.getMetaData();
      var labels = new ArrayList<String>();
      for (int i = 1; i <= meta.getColumnCount(); i++) {
        labels.add(JdbcUtils.lookupColumnName(meta, i));
      }
      var rows = new ArrayList<List<Object>>();
      while (rs.next()) {
        var row = new ArrayList<Object>(labels.size());
        for (int i = 1; i <= labels.size(); i++) {
          row.add(JdbcUtils.getResultSetValue(rs, i));
        }
        rows.add(row);
      }
      return new ReadResult(labels, rows, total);
    };
  }

  @Override
  public int[] modify(String query, List<Map<String, Object>> rows) {
    var template = template(0);
    var sql = QueryText.trimTrailingSemicolon(query);
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(status -> {
          var counts = new int[rows.size()];
          for (int i = 0; i < rows.size(); i++) {
            counts[i] = template.update(sql, rows.get(i));
          }
          return counts;
        });
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
