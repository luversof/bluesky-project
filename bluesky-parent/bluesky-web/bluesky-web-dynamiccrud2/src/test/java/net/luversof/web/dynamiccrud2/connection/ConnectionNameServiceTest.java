package net.luversof.web.dynamiccrud2.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.TableRef;

/** connectionInfo 저장소 검색(자동완성) → 고른 이름으로 테이블 목록까지. localdev 프로파일 필요. */
class ConnectionNameServiceTest implements GeneralWebTest {

  @Autowired ConnectionNameService connectionNameService;

  @Autowired DatabaseAdapterResolver resolver;

  @Test
  void searchesByKeywordIgnoringCaseAndOpensChosenOne() {
    // properties 에 미리 적지 않은 connectioninfo 도 저장소에서 나온다
    assertThat(connectionNameService.search("CONNECTIONINFO")).contains("connectioninfo");
    assertThat(connectionNameService.search("crud2")).contains("dynamiccrud2").isSorted();
    // 검색어가 비면 앞에서부터, 한도까지
    assertThat(connectionNameService.search(""))
        .isNotEmpty()
        .hasSizeLessThanOrEqualTo(ConnectionNameService.SUGGEST_LIMIT);
    // % · _ 는 와일드카드가 아니라 글자로 찾는다
    assertThat(connectionNameService.search("%")).doesNotContain("connectioninfo");
    assertThat(connectionNameService.search("no-such-connection-zzz")).isEmpty();
    // 읽은 출처 = 켜진 JDBC reader key(application.properties 의 readers.postgresql-datasource)
    assertThat(connectionNameService.searchWithSource("crud2"))
        .contains(
            new ConnectionNameService.ConnectionName("dynamiccrud2", "postgresql-datasource"));

    assertThat(resolver.resolve("connectioninfo").listTables())
        .extracting(TableRef::name)
        .contains("DataSourceConnectionConfig");
  }

  /** 출처 표시: 켜진 *-datasource reader 만, 여럿이면 이어서, 없으면 표시 없음(Mongo reader 는 이 표의 출처가 아니다) */
  @Test
  void sourceIsEnabledDatasourceReaderKey() {
    var props = new io.github.luversof.boot.connectioninfo.ConnectionInfoProperties();
    var service = new ConnectionNameService(null, null, props);
    assertThat(service.datasourceSource()).isNull();
    props.getReaders().put("postgresql-datasource", reader(true));
    props.getReaders().put("mariadb-datasource", reader(false));
    props.getReaders().put("mongodb-mongoclient", reader(true));
    assertThat(service.datasourceSource()).isEqualTo("postgresql-datasource");
    props.getReaders().put("mariadb-datasource", reader(true));
    assertThat(service.datasourceSource()).isEqualTo("mariadb-datasource · postgresql-datasource");
  }

  private static io.github.luversof.boot.connectioninfo.ConnectionInfoProperties
          .ConnectionInfoReaderProperties
      reader(boolean enabled) {
    return new io.github.luversof.boot.connectioninfo.ConnectionInfoProperties
        .ConnectionInfoReaderProperties(enabled, java.util.Map.of());
  }
}
