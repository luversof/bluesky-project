package net.luversof.web.dynamiccrud2.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.TableRef;

/** connectionInfo 저장소 전체 목록 → 고른 이름으로 테이블 목록까지. localdev 프로파일 필요. */
class ConnectionNameServiceTest implements GeneralWebTest {

  @Autowired ConnectionNameService connectionNameService;

  @Autowired DatabaseAdapterResolver resolver;

  @Test
  void listsAllConnectionNamesAndOpensChosenOne() {
    var names = connectionNameService.connectionNames();
    // properties 에 미리 적지 않은 connectioninfo 도 저장소 목록에서 나온다
    assertThat(names).contains("dynamiccrud2", "connectioninfo").isSorted();

    assertThat(resolver.resolve("connectioninfo").listTables())
        .extracting(TableRef::name)
        .contains("DataSourceConnectionConfig");
  }
}
