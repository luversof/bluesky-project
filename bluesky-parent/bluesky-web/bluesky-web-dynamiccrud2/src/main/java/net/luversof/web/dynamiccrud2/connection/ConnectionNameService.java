package net.luversof.web.dynamiccrud2.connection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.jdbc.JdbcDatabaseAdapter;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;

/**
 * 메뉴 등록 때 고를 connection 이름 검색. connectionInfo 저장소를 매번 읽어 연결 정보가 바뀌어도 재배포가 필요 없다. 이름만 내보내고 url · 계정은
 * 읽지 않는다. 연결이 수백 개라 전체 목록 대신 검색어로 찾는다(자동완성).
 */
@Service
public class ConnectionNameService {

  /** connectionInfo JDBC 저장 테이블(bluesky-boot-connectioninfo 의 reader 와 같은 이름) */
  private static final String CONFIG_TABLE = "DataSourceConnectionConfig";

  /** 자동완성 후보 수 */
  public static final int SUGGEST_LIMIT = 30;

  private final DatabaseAdapterResolver resolver;
  private final Dynamiccrud2Properties properties;

  public ConnectionNameService(
      DatabaseAdapterResolver resolver, Dynamiccrud2Properties properties) {
    this.resolver = resolver;
    this.properties = properties;
  }

  /**
   * 자동완성 후보 하나. kind = DB 종류(postgresql · mysql · mariadb · sqlserver, 모르면 null) - 이름 옆 흐린 글자(DMT2 UDP-6560 의 mongodb ·
   * dbconn 표시와 같은 자리)
   */
  public record ConnectionName(String name, String kind) {}

  /**
   * 이름에 검색어가 들어간 connection(대소문자 무시, 이름순, 최대 {@link #SUGGEST_LIMIT}개). 검색어가 비면 앞에서부터. % · _ 는 글자
   * 그대로 찾는다.
   */
  public List<String> search(String keyword) {
    return searchWithKind(keyword).stream().map(ConnectionName::name).toList();
  }

  /**
   * {@link #search} 에 DB 종류를 붙인다. 종류는 DB 안에서 url 앞부분(jdbc:postgresql: …)만 보고 정한다 - url · 계정은 앱으로 가져오지
   * 않는다.
   */
  // 2차: Mongo 연결 정보(별도 Mongo DB 의 MongoClientConnectionConfig 컬렉션)도 합친다
  public List<ConnectionName> searchWithKind(String keyword) {
    // connectionInfo 저장소는 JDBC 다. 테이블 따옴표만 DB 종류를 따른다(PG 는 "DataSourceConnectionConfig")
    var adapter = (JdbcDatabaseAdapter) resolver.resolve(properties.connectionInfoConnection());
    // ESCAPE 문자는 '!' - 백슬래시는 MySQL 문자열 안에서 따로 해석돼 벤더마다 달라진다
    var sql =
        "SELECT connection,"
            + " CASE WHEN LOWER(url) LIKE 'jdbc:postgresql:%' THEN 'postgresql'"
            + " WHEN LOWER(url) LIKE 'jdbc:mariadb:%' THEN 'mariadb'"
            + " WHEN LOWER(url) LIKE 'jdbc:mysql:%' THEN 'mysql'"
            + " WHEN LOWER(url) LIKE 'jdbc:sqlserver:%' THEN 'sqlserver' END"
            + " FROM "
            + adapter.getDialect().quote(CONFIG_TABLE)
            + " WHERE LOWER(connection) LIKE LOWER(:keyword) ESCAPE '!' ORDER BY connection";
    var escaped =
        (keyword == null ? "" : keyword.strip())
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    return adapter
        .read(sql, Map.of("keyword", "%" + escaped + "%"), Set.of(), null, 0, SUGGEST_LIMIT)
        .rows()
        .stream()
        .map(row ->
            new ConnectionName(
                String.valueOf(row.get(0)), row.get(1) == null ? null : String.valueOf(row.get(1))))
        .toList();
  }
}
