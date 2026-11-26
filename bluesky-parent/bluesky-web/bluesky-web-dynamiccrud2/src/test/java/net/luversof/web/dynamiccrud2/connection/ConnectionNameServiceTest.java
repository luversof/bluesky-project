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
    // DB 종류(url 앞부분으로 DB 안에서 정한다 - url 은 가져오지 않는다)
    assertThat(connectionNameService.searchWithKind("crud2"))
        .contains(new ConnectionNameService.ConnectionName("dynamiccrud2", "postgresql"));

    assertThat(resolver.resolve("connectioninfo").listTables())
        .extracting(TableRef::name)
        .contains("DataSourceConnectionConfig");
  }
}
