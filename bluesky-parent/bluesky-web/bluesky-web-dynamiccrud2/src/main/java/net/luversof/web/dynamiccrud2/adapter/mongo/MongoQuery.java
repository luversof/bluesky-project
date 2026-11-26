package net.luversof.web.dynamiccrud2.adapter.mongo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.jdbc.core.SqlParameterValue;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * Mongo 작업 원문 = JSON 문서 하나. 예:
 *
 * <pre>
 * {"collection": "users", "operation": "find", "filter": {"status": :status}, "sort": {"_id": -1}, "projection": {"name": 1}}
 * {"collection": "users", "operation": "insertOne", "document": {"name": :name}}
 * {"collection": "users", "operation": "updateMany", "filter": {"_id": :_id}, "update": {"$set": {"name": :name}}}
 * {"collection": "users", "operation": "deleteMany", "filter": {"_id": :_id}}
 * </pre>
 *
 * <p>파라미터 {@code :name} 은 값 자리에 따옴표 없이 쓴다. 파싱한 뒤 자리 표시 문서로 바꿔 두고 실행할 때 타입 있는 BSON 값을 넣는다 - DMT 1차처럼
 * 글자를 치환하지 않으므로 값이 쿼리 구조를 바꿀 수 없다. ObjectId("…") · ISODate("…") 같은 셸 표기도 읽는다(Document.parse).
 */
record MongoQuery(
    String collection,
    Operation operation,
    Document filter,
    Document sort,
    Document projection,
    Document document,
    Document update) {

  /** 자리 표시 문서의 키({"$dmtParam": "이름"}) */
  static final String PARAM_KEY = "$dmtParam";

  private static final Pattern IDENTIFIER_START = Pattern.compile("[A-Za-z_]");

  enum Operation {
    FIND("find", QueryKind.READ),
    INSERT_ONE("insertOne", QueryKind.INSERT),
    UPDATE_MANY("updateMany", QueryKind.UPDATE),
    DELETE_MANY("deleteMany", QueryKind.DELETE);

    final String json;
    final QueryKind kind;

    Operation(String json, QueryKind kind) {
      this.json = json;
      this.kind = kind;
    }

    static Operation of(String json) {
      for (var operation : values()) {
        if (operation.json.equals(json)) {
          return operation;
        }
      }
      return null;
    }

    static Operation of(QueryKind kind) {
      for (var operation : values()) {
        if (operation.kind == kind) {
          return operation;
        }
      }
      throw new IllegalArgumentException(String.valueOf(kind));
    }
  }

  /** 원문 → 구조(파라미터는 자리 표시 문서로 남는다). 형식이 틀리면 BlueskyException(QUERY_CHECK_FAILED) */
  static MongoQuery parse(String text) {
    if (text == null || text.isBlank()) {
      throw new BlueskyException(QueryErrorCode.EMPTY_QUERY);
    }
    Document root;
    try {
      root = Document.parse(markParameters(text));
    } catch (JsonParseException | IllegalArgumentException e) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "JSON: " + e.getMessage());
    }
    var collection = root.get("collection");
    if (!(collection instanceof String name) || name.isBlank()) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "\"collection\" is required");
    }
    var operation = Operation.of(String.valueOf(root.get("operation")));
    if (operation == null) {
      throw new BlueskyException(
          QueryErrorCode.QUERY_CHECK_FAILED,
          "\"operation\" must be find · insertOne · updateMany · deleteMany");
    }
    for (var key : root.keySet()) {
      if (!Set.of("collection", "operation", "filter", "sort", "projection", "document", "update")
          .contains(key)) {
        throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "unknown key \"" + key + "\"");
      }
    }
    var query =
        new MongoQuery(
            name,
            operation,
            document(root, "filter"),
            document(root, "sort"),
            document(root, "projection"),
            document(root, "document"),
            document(root, "update"));
    if (operation == Operation.INSERT_ONE && query.document == null) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "insertOne needs \"document\"");
    }
    if (operation == Operation.UPDATE_MANY && query.update == null) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "updateMany needs \"update\"");
    }
    return query;
  }

  private static Document document(Document root, String key) {
    var value = root.get(key);
    if (value == null) {
      return null;
    }
    if (!(value instanceof Document document)) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, "\"" + key + "\" must be an object");
    }
    return document;
  }

  Document filterOrEmpty() {
    return filter == null ? new Document() : filter;
  }

  /** 파라미터 이름(나온 순서, 겹치면 한 번) */
  static List<String> parameterNames(String text) {
    var names = new LinkedHashSet<String>();
    scan(text, names, null);
    return new ArrayList<>(names);
  }

  /** 값 자리의 :name 을 {"$dmtParam": "name"} 로 바꾼다. 문자열 안은 그대로 */
  static String markParameters(String text) {
    var out = new StringBuilder(text.length() + 32);
    scan(text, null, out);
    return out.toString();
  }

  /**
   * 문자열(" · ') 밖에서, 앞의 공백 아닌 글자가 값 자리 표시(: , [ 또는 시작)인 :이름 을 찾는다. 키 뒤 구분자 ':' 는 앞이 따옴표라 걸리지 않는다.
   */
  private static void scan(String text, Set<String> names, StringBuilder out) {
    char quote = 0;
    char previous = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quote != 0) {
        append(out, c);
        if (c == '\\' && i + 1 < text.length()) {
          append(out, text.charAt(++i));
        } else if (c == quote) {
          quote = 0;
          previous = c;
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        append(out, c);
        continue;
      }
      if (c == ':'
          && (previous == ':' || previous == ',' || previous == '[')
          && i + 1 < text.length()
          && IDENTIFIER_START.matcher(String.valueOf(text.charAt(i + 1))).matches()) {
        int end = i + 1;
        while (end < text.length()
            && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
          end++;
        }
        var name = text.substring(i + 1, end);
        if (names != null) {
          names.add(name);
        }
        if (out != null) {
          out.append("{\"").append(PARAM_KEY).append("\": \"").append(name).append("\"}");
        }
        previous = '}';
        i = end - 1;
        continue;
      }
      append(out, c);
      if (!Character.isWhitespace(c)) {
        previous = c;
      }
    }
  }

  private static void append(StringBuilder out, char c) {
    if (out != null) {
      out.append(c);
    }
  }

  // ---- 바인딩 ----

  /** 자리 표시를 값으로 바꾼 사본. 없는 파라미터는 null */
  @SuppressWarnings("unchecked")
  static <T> T bind(T node, Map<String, Object> params) {
    if (node instanceof Document document) {
      var name = parameterOf(document);
      if (name != null) {
        return (T) toBson(params.get(name));
      }
      var copy = new Document();
      document.forEach((key, value) -> copy.put(key, bind(value, params)));
      return (T) copy;
    }
    if (node instanceof List<?> list) {
      var copy = new ArrayList<Object>(list.size());
      list.forEach(value -> copy.add(bind(value, params)));
      return (T) copy;
    }
    return node;
  }

  /** 자리 표시 문서면 파라미터 이름 */
  static String parameterOf(Object node) {
    if (node instanceof Document document
        && document.size() == 1
        && document.get(PARAM_KEY) instanceof String name) {
      return name;
    }
    return null;
  }

  /** 이 값(문서 · 배열) 안에 쓰인 파라미터 */
  static Set<String> parametersIn(Object node) {
    var names = new LinkedHashSet<String>();
    collect(node, names);
    return names;
  }

  private static void collect(Object node, Set<String> names) {
    var name = parameterOf(node);
    if (name != null) {
      names.add(name);
    } else if (node instanceof Document document) {
      document.values().forEach(value -> collect(value, names));
    } else if (node instanceof Collection<?> list) {
      list.forEach(value -> collect(value, names));
    }
  }

  /** 값이 빈 선택 파라미터를 쓰는 최상위 조건(키)을 뺀 filter. $and · $or 등 연산자 키 안의 조건은 뺄 수 없다(선택 불가로 분석된다) */
  static Document dropOptional(Document filter, Set<String> emptyOptional) {
    var copy = new Document();
    filter.forEach((key, value) -> {
      var used = parametersIn(value);
      if (key.startsWith("$") || used.stream().noneMatch(emptyOptional::contains)) {
        copy.put(key, value);
      }
    });
    return copy;
  }

  /** 화면 검색 조건 → Mongo 조건. 값은 params 에서 */
  static Document filterOf(List<Filter> filters, Map<String, Object> params) {
    var conditions = new ArrayList<Document>();
    for (var filter : filters) {
      var parts = new ArrayList<Document>();
      for (var label : filter.labels()) {
        parts.add(condition(label, filter, params));
      }
      conditions.add(parts.size() == 1 ? parts.getFirst() : new Document("$or", parts));
    }
    return conditions.size() == 1 ? conditions.getFirst() : new Document("$and", conditions);
  }

  private static Document condition(String label, Filter filter, Map<String, Object> params) {
    return switch (filter.operator()) {
      case CONTAINS, STARTS_WITH ->
          new Document(
              label,
              new Document("$regex", likeToRegex(String.valueOf(params.get(filter.param()))))
                  .append("$options", "i"));
      case EQUALS -> new Document(label, toBson(params.get(filter.param())));
      case RANGE -> {
        var range = new Document();
        if (filter.param() != null) {
          range.put("$gte", toBson(params.get(filter.param())));
        }
        if (filter.paramTo() != null) {
          range.put("$lte", toBson(params.get(filter.paramTo())));
        }
        yield new Document(label, range);
      }
    };
  }

  /** 호출하는 쪽이 만든 LIKE 패턴(% _ , 이스케이프 !) → 정규식(전체 일치) */
  static String likeToRegex(String like) {
    var regex = new StringBuilder("^");
    for (int i = 0; i < like.length(); i++) {
      char c = like.charAt(i);
      if (c == Filter.ESCAPE && i + 1 < like.length()) {
        regex.append(Pattern.quote(String.valueOf(like.charAt(++i))));
      } else if (c == '%') {
        regex.append(".*");
      } else if (c == '_') {
        regex.append('.');
      } else {
        regex.append(Pattern.quote(String.valueOf(c)));
      }
    }
    return regex.append('$').toString();
  }

  /** 화면 값(ValueConverter) → BSON 값. 숫자는 정수면 int · long, 아니면 double(Decimal128 로 넣으면 필드 타입이 바뀐다) */
  static Object toBson(Object value) {
    if (value instanceof SqlParameterValue parameter) {
      return toBson(parameter.getValue());
    }
    // 표준 문자 타입이 아닌 필드(ValueConverter) - Mongo 는 글자 그대로
    if (value instanceof net.luversof.web.dynamiccrud2.adapter.UntypedText text) {
      return text.value();
    }
    if (value instanceof BigDecimal number) {
      var stripped = number.stripTrailingZeros();
      if (stripped.scale() <= 0) {
        try {
          var longValue = stripped.longValueExact();
          return longValue == (int) longValue ? (Object) (int) longValue : (Object) longValue;
        } catch (ArithmeticException e) {
          return number.doubleValue();
        }
      }
      return number.doubleValue();
    }
    if (value instanceof LocalDateTime dateTime) {
      return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    if (value instanceof LocalDate date) {
      return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    if (value instanceof OffsetDateTime dateTime) {
      return Date.from(dateTime.toInstant());
    }
    if (value instanceof Instant instant) {
      return Date.from(instant);
    }
    return value;
  }

  /** BSON 값 → 목록 값(ValueConverter.toText 가 읽는 모양). 문서 · 배열은 JSON 글자 */
  static Object fromBson(Object value) {
    if (value instanceof ObjectId id) {
      return id.toHexString();
    }
    if (value instanceof Date date) {
      return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    if (value instanceof Decimal128 decimal) {
      return decimal.bigDecimalValue();
    }
    if (value instanceof Document document) {
      return document.toJson();
    }
    if (value instanceof List<?> list) {
      return new Document("v", list).toJson().replaceFirst("^\\{\"v\": ", "").replaceFirst("}$", "");
    }
    if (value instanceof Binary binary) {
      return Base64.getEncoder().encodeToString(binary.getData());
    }
    return value;
  }
}
