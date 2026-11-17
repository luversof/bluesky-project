package net.luversof.web.dynamiccrud2.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.jdbc.JdbcDatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.jdbc.SqlDialect;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;

/** connection 이름 → 어댑터. 연결은 connectionInfo 에서 그때그때 읽고, DB 종류는 제품 이름으로 고른다. */
@Component
public class DatabaseAdapterResolver {

  private final Dynamiccrud2Properties properties;

  // shortcut: 같은 이름의 연결이 다른 DB 종류로 바뀌면 재기동해야 반영된다, 그런 일이 생기면 캐시를 없앨 것
  private final Map<String, SqlDialect> dialects = new ConcurrentHashMap<>();

  public DatabaseAdapterResolver(Dynamiccrud2Properties properties) {
    this.properties = properties;
  }

  public DatabaseAdapter resolve(String connectionName) {
    // getConnection(이름, DataSource.class) 는 레지스트리 빈이 제네릭 변수로 등록돼 못 찾는다(실측 2026-11-17), 종류는
    // instanceof 로 가른다
    Object connection = ConnectionInfoUtil.getConnection(connectionName);
    // 2차: MongoClient 연결이면 Mongo 어댑터를 돌려준다
    if (!(connection instanceof DataSource dataSource)) {
      throw new BlueskyException(QueryErrorCode.CONNECTION_NOT_FOUND, connectionName);
    }
    var dialect =
        dialects.computeIfAbsent(
            connectionName,
            name ->
                SqlDialect.of(
                    new JdbcTemplate(dataSource)
                        .execute(
                            (java.sql.Connection c) -> c.getMetaData().getDatabaseProductName())));
    return new JdbcDatabaseAdapter(dataSource, dialect, properties);
  }
}
