package net.luversof.web.dynamiccrud2.connection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.jdbc.JdbcDatabaseAdapter;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;

/**
 * 메뉴 등록 때 고를 connection 이름 목록. connectionInfo 저장소의 전체 목록을 매번 읽어, 연결 정보가 바뀌어도 재배포가 필요 없다. 이름만 내보내고
 * url · 계정은 읽지 않는다.
 */
@Service
public class ConnectionNameService {

  /** connectionInfo JDBC 저장 테이블(bluesky-boot-connectioninfo 의 reader 와 같은 이름) */
  private static final String CONFIG_TABLE = "DataSourceConnectionConfig";

  private final DatabaseAdapterResolver resolver;
  private final Dynamiccrud2Properties properties;

  public ConnectionNameService(
      DatabaseAdapterResolver resolver, Dynamiccrud2Properties properties) {
    this.resolver = resolver;
    this.properties = properties;
  }

  // 2차: Mongo 연결 정보(별도 Mongo DB 의 MongoClientConnectionConfig 컬렉션)도 합친다
  public List<String> connectionNames() {
    // connectionInfo 저장소는 JDBC 다. 테이블 따옴표만 DB 종류를 따른다(PG 는 "DataSourceConnectionConfig")
    var adapter = (JdbcDatabaseAdapter) resolver.resolve(properties.connectionInfoConnection());
    var sql =
        "SELECT connection FROM "
            + adapter.getDialect().quote(CONFIG_TABLE)
            + " ORDER BY connection";
    return adapter.read(sql, Map.of(), Set.of(), null, 0, properties.pageMaxRows()).rows().stream()
        .map(row -> String.valueOf(row.get(0)))
        .toList();
  }
}
