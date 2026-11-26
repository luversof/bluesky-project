package net.luversof.web.dynamiccrud2.connection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import io.github.luversof.boot.connectioninfo.ConnectionInfoProperties;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.jdbc.JdbcDatabaseAdapter;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;

/**
 * 메뉴 등록 때 고를 connection 이름 검색. connectionInfo 저장소를 매번 읽어 연결 정보가 바뀌어도 재배포가 필요 없다. 이름만 내보내고 url · 계정은
 * 읽지 않는다. 연결이 수백 개라 전체 목록 대신 검색어로 찾는다(자동완성).
 */
@Service
public class ConnectionNameService {

  /** connectionInfo JDBC 저장 테이블(bluesky-boot-connectioninfo 의 *-datasource reader 가 읽는 테이블) */
  private static final String CONFIG_TABLE = "DataSourceConnectionConfig";

  /**
   * JDBC 연결 정보 reader key 끝(postgresql-datasource · mariadb-datasource · mysql-datasource ·
   * sqlserver-datasource)
   */
  private static final String DATASOURCE_READER_SUFFIX = "-datasource";

  /** Mongo 연결 이름을 읽는 reader key(bluesky-boot.connection-info.mongodb.connection-map) */
  static final String MONGO_READER = "properties-mongoclient";

  /** 자동완성 후보 수 */
  public static final int SUGGEST_LIMIT = 30;

  private final DatabaseAdapterResolver resolver;
  private final Dynamiccrud2Properties properties;
  private final ConnectionInfoProperties connectionInfoProperties;

  public ConnectionNameService(
      DatabaseAdapterResolver resolver,
      Dynamiccrud2Properties properties,
      ConnectionInfoProperties connectionInfoProperties) {
    this.resolver = resolver;
    this.properties = properties;
    this.connectionInfoProperties = connectionInfoProperties;
  }

  /**
   * 자동완성 후보 하나. source = 이 연결 정보를 읽는 reader key(bluesky-boot.connection-info.readers.{key}, 예
   * postgresql-datasource) - 이름 옆 흐린 글자(DMT2 UDP-6560 의 mongodb · dbconn 표시와 같은 자리). 출처가 늘면(Mongo 의
   * mongodb-mongoclient 등) 그 reader key 로 보인다(사용자 결정 11-26 - DB 종류가 아니라 읽는 기준으로).
   */
  public record ConnectionName(String name, String source) {}

  /**
   * 이름에 검색어가 들어간 connection(대소문자 무시, 이름순, 최대 {@link #SUGGEST_LIMIT}개). 검색어가 비면 앞에서부터. % · _ 는 글자
   * 그대로 찾는다.
   */
  public List<String> search(String keyword) {
    return searchWithSource(keyword).stream().map(ConnectionName::name).toList();
  }

  /**
   * {@link #search} 에 읽은 출처(reader key)를 붙인다. Mongo 연결(앱 설정 connection-map, 출처 {@value #MONGO_READER})과 JDBC
   * 표를 합쳐 이름순(DMT2 9dfa599 와 같게). 같은 이름이 둘 다 있으면 Mongo 로 쓰인다(DatabaseAdapterResolver)
   */
  public List<ConnectionName> searchWithSource(String keyword) {
    var needle = keyword == null ? "" : keyword.strip().toLowerCase(java.util.Locale.ROOT);
    var mongoNames = resolver.mongoConnectionNames();
    var names = new java.util.ArrayList<ConnectionName>();
    mongoNames.stream()
        .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains(needle))
        .forEach(name -> names.add(new ConnectionName(name, MONGO_READER)));
    jdbcNames(keyword).stream().filter(name -> !mongoNames.contains(name.name())).forEach(names::add);
    names.sort(java.util.Comparator.comparing(ConnectionName::name, String.CASE_INSENSITIVE_ORDER));
    return names.size() > SUGGEST_LIMIT ? List.copyOf(names.subList(0, SUGGEST_LIMIT)) : names;
  }

  private List<ConnectionName> jdbcNames(String keyword) {
    // connectionInfo 저장소는 JDBC 다. 테이블 따옴표만 DB 종류를 따른다(PG 는 "DataSourceConnectionConfig")
    var adapter = (JdbcDatabaseAdapter) resolver.resolve(properties.connectionInfoConnection());
    // ESCAPE 문자는 '!' - 백슬래시는 MySQL 문자열 안에서 따로 해석돼 벤더마다 달라진다
    var sql =
        "SELECT connection FROM "
            + adapter.getDialect().quote(CONFIG_TABLE)
            + " WHERE LOWER(connection) LIKE LOWER(:keyword) ESCAPE '!' ORDER BY connection";
    var escaped =
        (keyword == null ? "" : keyword.strip())
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    var source = datasourceSource();
    return adapter
        .read(sql, Map.of("keyword", "%" + escaped + "%"), Set.of(), null, 0, SUGGEST_LIMIT)
        .rows()
        .stream()
        .map(row -> new ConnectionName(String.valueOf(row.get(0)), source))
        .toList();
  }

  /**
   * 이 표({@value #CONFIG_TABLE})를 읽는 켜진 JDBC reader key. 하나면 그것, 여럿이면 이어서(어느 reader 의 표인지 이름만으로는 가릴
   * 수 없다), 없으면 null(표시 없음)
   */
  String datasourceSource() {
    var keys =
        connectionInfoProperties.getReaders().entrySet().stream()
            .filter(e -> e.getValue() != null && e.getValue().isEnabled())
            .map(Map.Entry::getKey)
            .filter(key -> key.endsWith(DATASOURCE_READER_SUFFIX))
            .sorted()
            .toList();
    return keys.isEmpty() ? null : String.join(" · ", keys);
  }
}
