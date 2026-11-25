package net.luversof.web.dynamiccrud2.adapter.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * 설정 catalog(dynamiccrud2, PostgreSQL) 자체를 대상 DB 로 써서 어댑터를 실제로 돌린다. 자기가 넣은 행만 지운다. localdev 프로파일
 * 필요.
 */
class JdbcDatabaseAdapterTest implements GeneralWebTest {

  @Autowired DatabaseAdapterResolver resolver;

  private DatabaseAdapter adapter() {
    return resolver.resolve("dynamiccrud2");
  }

  private TableRef table(String name) {
    return adapter().listTables().stream()
        .filter(t -> t.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void listsTablesWithoutSystemSchemas() {
    var tables = adapter().listTables();
    assertThat(tables).extracting(TableRef::name).contains("Menu", "MenuQuery", "DbField");
    assertThat(tables)
        .extracting(TableRef::schema)
        .doesNotContain("pg_catalog", "information_schema");
  }

  @Test
  void readTableSkipsPartialAndExpressionUniqueIndexes() {
    var menuQuery = adapter().readTable(table("MenuQuery"));
    assertThat(menuQuery.primaryKey()).containsExactly("id");
    // uk_menuQuery_menuId_read 는 WHERE 가 붙은 부분 인덱스라 행 키가 될 수 없다
    assertThat(menuQuery.uniqueKeys()).isEmpty();
    assertThat(menuQuery.indexLeadingColumns()).contains("id", "menu_id");
    // 화면용 인덱스 목록: PK · 일반 · 부분 unique 를 모두 보인다
    assertThat(menuQuery.indexes())
        .anySatisfy(i -> {
          assertThat(i.primary()).isTrue();
          assertThat(i.columns()).containsExactly("id");
        })
        .anySatisfy(i -> {
          assertThat(i.name()).isEqualToIgnoringCase("idx_menuQuery_menuId");
          assertThat(i.unique()).isFalse();
        })
        .anySatisfy(i -> {
          assertThat(i.name()).isEqualToIgnoringCase("uk_menuQuery_menuId_read");
          assertThat(i.unique()).isTrue();
          assertThat(i.partial()).isTrue();
        });
    var columns = menuQuery.columns();
    assertThat(columns)
        .filteredOn(c -> c.name().equals("queryText"))
        .singleElement()
        .satisfies(c -> assertThat(c.inputType()).isEqualTo(InputType.TEXTAREA));
    assertThat(columns)
        .filteredOn(c -> c.name().equals("kind"))
        .singleElement()
        .satisfies(c -> {
          assertThat(c.required()).isTrue();
          assertThat(c.maxLength()).isEqualTo(10);
        });

    var dbField = adapter().readTable(table("DbField"));
    assertThat(dbField.primaryKey()).containsExactly("dbTable_id", "columnName");

    // uk_dbTable_location 은 COALESCE 식 인덱스라 키로 쓰지 않지만 첫 컬럼은 정렬에 쓸 수 있다
    var dbTable = adapter().readTable(table("DbTable"));
    assertThat(dbTable.uniqueKeys()).isEmpty();
    assertThat(dbTable.indexLeadingColumns()).contains("id", "connectionName");
  }

  @Test
  void generatedInsertReadDeleteRoundTrip() {
    var adapter = adapter();
    var project = adapter.readTable(table("Project"));
    var insert = adapter.generate(QueryKind.INSERT, project);
    var delete = adapter.generate(QueryKind.DELETE, project);
    adapter.validate(insert, QueryKind.INSERT);
    adapter.validate(delete, QueryKind.DELETE);

    var id = UUID.randomUUID();
    var name = "adapter-test-" + id;
    var inserted =
        adapter.modify(
            insert,
            List.of(
                Map.of(
                    "id",
                    id,
                    "code",
                    "t" + id.toString().substring(0, 8),
                    "name",
                    name,
                    "displayOrder",
                    7,
                    "enableDisplay",
                    true,
                    "createdDate",
                    OffsetDateTime.now())));
    try {
      assertThat(inserted).containsExactly(1);
      var read =
          """
          SELECT p.id, p.name AS "이름", p."displayOrder" FROM "Project" p
          WHERE p.name = :name AND p."displayOrder" = :order
          """;
      var analysis = adapter.analyzeRead(read);
      assertThat(analysis.conditions()).allMatch(c -> c.optionalCapable());

      // :order 를 선택 조건으로 비워 빼고, name 으로만 찾는다
      var result =
          adapter.read(read, Map.of("name", name), Set.of("order"), new Sort("이름", true), 0, 10);
      assertThat(result.total()).isEqualTo(1);
      assertThat(result.labels()).containsExactly("id", "이름", "displayOrder");
      assertThat(result.rows())
          .singleElement()
          .satisfies(row -> assertThat(row.get(1)).isEqualTo(name));

      // 행 키로 지금 값 읽기(동시 수정 확인)
      assertThat(adapter.currentRow(table("Project"), List.of("name"), Map.of("id", id)))
          .containsExactly(name);
      assertThat(
              adapter.currentRow(
                  table("Project"), List.of("name"), Map.of("id", UUID.randomUUID())))
          .isNull();

      // 검색 셀렉트 "DB 값 전체": 서로 다른 값(NULL 제외), 한도를 넘으면 한도 + 1 개까지
      assertThat(adapter.distinctValues(table("Project"), "name", 100_000)).contains(name);
      assertThat(adapter.distinctValues(table("Project"), "name", 1)).hasSizeLessThanOrEqualTo(2);

      // 조회는 읽기 전용 트랜잭션 - 조회로 저장된 쓰기 CTE 는 실패하고 행은 그대로
      var writingRead =
          "WITH changed AS (UPDATE \"Project\" SET name = 'hacked' WHERE id = :id RETURNING id)"
              + " SELECT id FROM changed WHERE :_limit > 0";
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> adapter.sample(writingRead, Map.of("id", id), Set.of(), 10))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(adapter.currentRow(table("Project"), List.of("name"), Map.of("id", id)))
          .containsExactly(name);
    } finally {
      assertThat(adapter.modify(delete, List.of(Map.of("id", id)))).containsExactly(1);
    }
  }
}
