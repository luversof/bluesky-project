package net.luversof.web.dynamiccrud2.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;

class MongoQueryTest {

  private static final String FIND =
      """
      {"collection": "users", "operation": "find",
       "filter": {"status": :status, "name": "a:b", "age": {"$gte": :minAge}, "$or": [{"x": :x}, {"y": 1}]},
       "sort": {"_id": -1}}
      """;

  @Test
  void parametersAreBareTokensInValuePositions() {
    assertThat(MongoQuery.parameterNames(FIND)).containsExactly("status", "minAge", "x");
    // 문자열 안 · 키 뒤 구분자는 파라미터가 아니다
    assertThat(MongoQuery.parameterNames("{\"a\": \"x :y\", 'b': ':z', \"c\": [:p, :q]}"))
        .containsExactly("p", "q");
  }

  @Test
  void bindsTypedValuesNotText() {
    var parsed = MongoQuery.parse(FIND);
    var params = new HashMap<String, Object>();
    // 값에 JSON 을 넣어도 구조가 바뀌지 않는다(1차의 글자 치환과 다름)
    params.put("status", "x\"}, \"$where\": \"1");
    params.put("minAge", new BigDecimal("20"));
    params.put("x", new BigDecimal("1.5"));
    Document filter = MongoQuery.bind(parsed.filter(), params);
    assertThat(filter.get("status")).isEqualTo("x\"}, \"$where\": \"1");
    assertThat(filter.keySet()).containsExactly("status", "name", "age", "$or");
    assertThat(((Document) filter.get("age")).get("$gte")).isEqualTo(20);
    assertThat(filter.get("name")).isEqualTo("a:b");
    assertThat(MongoQuery.toBson(new BigDecimal("10000000000"))).isEqualTo(10_000_000_000L);
    assertThat(MongoQuery.toBson(new BigDecimal("1.5"))).isEqualTo(1.5);
    assertThat(MongoQuery.toBson(LocalDateTime.of(2026, 11, 25, 10, 0))).isInstanceOf(Date.class);
  }

  @Test
  void dropsOnlyTopLevelOptionalConditions() {
    var parsed = MongoQuery.parse(FIND);
    var dropped = MongoQuery.dropOptional(parsed.filter(), Set.of("status", "x"));
    // $or 안의 조건은 빼지 않는다(분석에서 선택 불가)
    assertThat(dropped.keySet()).containsExactly("name", "age", "$or");
  }

  @Test
  void screenFiltersBecomeMongoConditions() {
    var params =
        Map.<String, Object>of("kw", "%a!_b%", "from", new BigDecimal("1"), "eq", "x");
    var filter =
        MongoQuery.filterOf(
            List.of(
                new Filter(List.of("name", "nick"), SearchOperator.CONTAINS, "kw", null),
                new Filter(List.of("age"), SearchOperator.RANGE, "from", null),
                new Filter(List.of("code"), SearchOperator.EQUALS, "eq", null)),
            params);
    var and = filter.getList("$and", Document.class);
    assertThat(and).hasSize(3);
    var or = and.get(0).getList("$or", Document.class);
    var regex = ((Document) or.get(0).get("name")).getString("$regex");
    assertThat("XaZ_bY".matches("(?i)" + regex)).isFalse();
    assertThat("xa_by".matches("(?i)" + regex)).isTrue();
    assertThat(and.get(1)).isEqualTo(new Document("age", new Document("$gte", 1)));
    assertThat(and.get(2)).isEqualTo(new Document("code", "x"));
  }

  @Test
  void rejectsBrokenOrUnknownShapes() {
    assertThatThrownBy(() -> MongoQuery.parse("{\"collection\": \"u\", \"operation\": \"find\""))
        .isInstanceOf(BlueskyException.class);
    assertThatThrownBy(() -> MongoQuery.parse("{\"collection\": \"u\", \"operation\": \"aggregate\"}"))
        .isInstanceOf(BlueskyException.class);
    assertThatThrownBy(() -> MongoQuery.parse("{\"operation\": \"find\"}"))
        .isInstanceOf(BlueskyException.class);
    assertThatThrownBy(
            () -> MongoQuery.parse("{\"collection\": \"u\", \"operation\": \"updateMany\", \"filter\": {}}"))
        .isInstanceOf(BlueskyException.class);
  }

  @Test
  void bsonValuesBecomeListValues() {
    var id = new ObjectId();
    assertThat(MongoQuery.fromBson(id)).isEqualTo(id.toHexString());
    assertThat(MongoQuery.fromBson(new Document("a", 1))).isEqualTo("{\"a\": 1}");
    assertThat(MongoQuery.fromBson(List.of(1, 2))).isEqualTo("[1, 2]");
    assertThat(MongoQuery.fromBson(new Date(0))).isInstanceOf(LocalDateTime.class);
  }
}
