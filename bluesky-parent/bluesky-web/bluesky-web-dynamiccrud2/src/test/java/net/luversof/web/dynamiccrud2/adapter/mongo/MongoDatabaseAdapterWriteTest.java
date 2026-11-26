package net.luversof.web.dynamiccrud2.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;

/** Mongo 조회 · 입력 · 수정 · 삭제를 버리는 컨테이너(mongo:8.2)에서. Docker 가 없으면 건너뛴다 */
@Tag("integration")
class MongoDatabaseAdapterWriteTest {

  private static MongoDBContainer container;
  private static MongoClient client;
  private static MongoDatabaseAdapter adapter;

  @BeforeAll
  static void start() {
    Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker");
    container = new MongoDBContainer("mongo:8.2");
    container.start();
    client = MongoClients.create(container.getConnectionString());
    var database = client.getDatabase("dmt");
    database
        .getCollection("users")
        .insertMany(
            List.of(
                new Document("name", "alpha").append("age", 20).append("tags", List.of("a")),
                new Document("name", "beta").append("age", 30),
                new Document("name", "Gamma")
                    .append("age", 40)
                    .append("profile", new Document("lv", 3))));
    adapter =
        new MongoDatabaseAdapter(
            database, new Dynamiccrud2Properties(null, null, null, null, null));
  }

  @AfterAll
  static void stop() {
    if (client != null) {
      client.close();
    }
    if (container != null) {
      container.stop();
    }
  }

  /** 검색 셀렉트 "DB 값 전체": 없는 필드(NULL)는 빼고 정렬, 한도를 넘으면 한도 + 1 개 */
  @Test
  void distinctValues() {
    var users = new TableRef(null, null, "users");
    assertThat(adapter.distinctValues(users, "profile.lv", 100)).containsExactly(3);
    assertThat(adapter.distinctValues(users, "name", 100)).contains("Gamma", "alpha", "beta");
    assertThat(adapter.distinctValues(users, "name", 1)).hasSize(2);
  }

  @Test
  void readSearchSortAndPage() {
    var meta = adapter.readTable(new TableRef(null, null, "users"));
    assertThat(meta.columns())
        .extracting(c -> c.name())
        .containsExactly("_id", "name", "age", "profile", "tags");
    assertThat(meta.columns().get(0).typeName()).isEqualTo("objectId");

    var query =
        """
        {"collection": "users", "operation": "find", "filter": {"age": {"$gte": :minAge}}, "sort": {"age": 1}}
        """;
    var analysis = adapter.analyzeRead(query);
    assertThat(analysis.conditions())
        .singleElement()
        .satisfies(c -> assertThat(c.optionalCapable()).isTrue());

    var page =
        adapter.read(
            query, Map.of("minAge", new BigDecimal("25")), Set.of(), List.of(), null, 0, 1);
    assertThat(page.total()).isEqualTo(2);
    assertThat(page.rows())
        .singleElement()
        .satisfies(row -> assertThat(row.get(page.labels().indexOf("name"))).isEqualTo("beta"));
    var next =
        adapter.read(
            query, Map.of("minAge", new BigDecimal("25")), Set.of(), List.of(), null, 1, 1);
    assertThat(next.rows().getFirst().get(next.labels().indexOf("name"))).isEqualTo("Gamma");
    // 빈 선택 조건은 뺀다 · 화면 정렬 · 화면 검색(부분 일치, 대소문자 무시)
    var all =
        adapter.read(query, Map.of(), Set.of("minAge"), List.of(), new Sort("name", true), 0, 10);
    assertThat(all.total()).isEqualTo(3);
    // Mongo 기본 정렬은 대소문자를 가린다(바이트 순: 소문자 > 대문자)
    assertThat(all.rows())
        .extracting(r -> r.get(all.labels().indexOf("name")))
        .containsExactly("beta", "alpha", "Gamma");
    var searched =
        adapter.read(
            query,
            Map.of("kw", "%AMM%"),
            Set.of("minAge"),
            List.of(new Filter(List.of("name"), SearchOperator.CONTAINS, "kw", null)),
            null,
            0,
            10);
    assertThat(searched.total()).isEqualTo(1);
    // 중첩 문서 · 배열은 JSON 글자
    assertThat(all.rows().get(2).get(all.labels().indexOf("profile"))).isEqualTo("{\"lv\": 3}");
    assertThat(all.rows().get(1).get(all.labels().indexOf("tags"))).isEqualTo("[\"a\"]");
  }

  @Test
  void generatedInsertUpdateDeleteRoundTrip() {
    var meta = adapter.readTable(new TableRef(null, null, "users"));
    var insert = adapter.generate(QueryKind.INSERT, meta);
    var update = adapter.generate(QueryKind.UPDATE, meta);
    var delete = adapter.generate(QueryKind.DELETE, meta);
    for (var entry :
        Map.of(QueryKind.INSERT, insert, QueryKind.UPDATE, update, QueryKind.DELETE, delete)
            .entrySet()) {
      adapter.validate(entry.getValue(), entry.getKey());
      adapter.check(entry.getValue());
    }
    assertThat(adapter.parameterNames(update))
        .containsExactly("_id", "name", "age", "profile", "tags");

    var values = new java.util.HashMap<String, Object>();
    values.put("name", "delta");
    values.put("age", new BigDecimal("50"));
    assertThat(adapter.modify(insert, List.of(values))).containsExactly(1);
    var read = adapter.generate(QueryKind.READ, meta);
    var found =
        adapter.read(
            read,
            Map.of("n", "delta"),
            Set.of(),
            List.of(new Filter(List.of("name"), SearchOperator.EQUALS, "n", null)),
            null,
            0,
            10);
    assertThat(found.total()).isEqualTo(1);
    var row = found.rows().getFirst();
    var id = (String) row.get(found.labels().indexOf("_id"));
    // 숫자는 정수 그대로(Decimal128 아님)
    assertThat(row.get(found.labels().indexOf("age"))).isEqualTo(50);

    // 행 키는 화면에서 글자로 온다 - ObjectId 로 바꿔 찾는다
    values.put("_id", id);
    values.put("name", "delta2");
    assertThat(adapter.modify(update, List.of(values))).containsExactly(1);
    assertThat(adapter.currentRow(meta.ref(), List.of("name"), Map.of("_id", id)))
        .containsExactly("delta2");

    assertThat(adapter.modify(delete, List.of(Map.of("_id", id)))).containsExactly(1);
    assertThat(adapter.currentRow(meta.ref(), List.of("name"), Map.of("_id", id))).isNull();
  }

  @Test
  void rejectsUnsafeOrWrongQueries() {
    // filter 에 파라미터가 없는 수정 · 삭제는 전체 문서를 바꿀 수 있다
    assertThatThrownBy(() ->
            adapter.validate(
                "{\"collection\": \"users\", \"operation\": \"deleteMany\", \"filter\": {}}",
                QueryKind.DELETE))
        .isInstanceOf(BlueskyException.class);
    assertThatThrownBy(() ->
            adapter.validate(
                "{\"collection\": \"users\", \"operation\": \"find\"}", QueryKind.DELETE))
        .isInstanceOf(BlueskyException.class);
    assertThatThrownBy(() -> adapter.check("{\"collection\": \"nope\", \"operation\": \"find\"}"))
        .isInstanceOf(DataAccessException.class);
  }
}
