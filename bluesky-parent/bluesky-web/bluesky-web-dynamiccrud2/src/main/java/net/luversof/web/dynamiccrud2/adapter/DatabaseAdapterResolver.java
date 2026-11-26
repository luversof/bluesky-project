package net.luversof.web.dynamiccrud2.adapter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;

import io.github.luversof.boot.connectioninfo.ConnectionInfoUtil;
import io.github.luversof.boot.connectioninfo.mongodb.MongoDbConnectionMapProperties;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.jdbc.JdbcDatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.jdbc.SqlDialect;
import net.luversof.web.dynamiccrud2.adapter.mongo.MongoDatabaseAdapter;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;

/**
 * connection 이름 → 어댑터. Mongo 연결 목록(bluesky-boot.connection-info.mongodb.connection-map)에 있는 이름이면
 * Mongo, 아니면 JDBC. 연결은 connectionInfo 에서 그때그때 읽고, JDBC DB 종류는 제품 이름으로 고른다. Mongo 연결 목록을 설정하지 않으면(지금
 * 기본) 모두 JDBC 다(DMT2 9dfa599 UDP-6560 이식).
 */
@Component
public class DatabaseAdapterResolver {

  private final Dynamiccrud2Properties properties;

  /** Mongo 연결 설정. connectionInfo 가 꺼져 있으면 빈이 없다 */
  private final ObjectProvider<MongoDbConnectionMapProperties> mongoConnections;

  // shortcut: 같은 이름의 연결이 다른 DB 종류로 바뀌면 재기동해야 반영된다, 그런 일이 생기면 캐시를 없앨 것
  private final Map<String, SqlDialect> dialects = new ConcurrentHashMap<>();

  public DatabaseAdapterResolver(
      Dynamiccrud2Properties properties,
      ObjectProvider<MongoDbConnectionMapProperties> mongoConnections) {
    this.properties = properties;
    this.mongoConnections = mongoConnections;
  }

  /**
   * Mongo 연결 이름(앱 설정의 connection-map). 설정이 없으면 빈 값. 연결 정보를 Mongo 컬렉션에서 읽는 mongodb-mongoclient
   * reader 는 데이터베이스 이름을 MongoClient 에서 알 수 없어 쓰지 않는다
   */
  // shortcut: properties-mongoclient(connection-map)만, mongodb-mongoclient 저장소 연결이 필요해지면 이름 ·
  // database 를 그 컬렉션에서 읽을 것
  public Set<String> mongoConnectionNames() {
    var mongo = mongoConnections.getIfAvailable();
    return mongo == null || mongo.getConnectionMap() == null
        ? Set.of()
        : mongo.getConnectionMap().keySet();
  }

  public DatabaseAdapter resolve(String connectionName) {
    if (mongoConnectionNames().contains(connectionName)) {
      // 데이터베이스 이름은 연결 설정에만 있다(MongoClient 는 모른다)
      var database =
          mongoConnections.getObject().getConnectionMap().get(connectionName).getDatabase();
      var client = ConnectionInfoUtil.getConnection(connectionName, MongoClient.class);
      if (client == null || database == null) {
        throw new BlueskyException(QueryErrorCode.CONNECTION_NOT_FOUND, connectionName);
      }
      return new MongoDatabaseAdapter(client.getDatabase(database), properties);
    }
    DataSource dataSource = ConnectionInfoUtil.getConnection(connectionName, DataSource.class);
    if (dataSource == null) {
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
