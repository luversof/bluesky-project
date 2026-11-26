package net.luversof.web.dynamiccrud2.adapter.mongo;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.jdbc.BadSqlGrammarException;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.ColumnMeta;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.Filter;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.SearchCondition;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.config.Dynamiccrud2Properties;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/**
 * MongoDB 어댑터. 작업 원문 형식은 {@link MongoQuery}. 테이블 = 컬렉션(TableRef.name), 컬럼 = 문서 {@value #SAMPLE_SIZE}개를 읽어 모은
 * 최상위 필드, 행 키 = _id.
 *
 * <p>조회: find + countDocuments + sort + skip/limit. 결과 이름은 projection(포함)이 있으면 그 키, 없으면 이 쪽 문서들의 필드(처음 나온
 * 순서, _id 먼저).
 */
// shortcut: 여러 행 입력 · 수정 · 삭제가 한 트랜잭션이 아니다(Mongo 트랜잭션은 replica set 필요), 중간에 실패하면 앞 행은 남는다 - 묶음 실행이 생기면
// ClientSession 트랜잭션으로 바꿀 것
public class MongoDatabaseAdapter implements DatabaseAdapter {

  static final int SAMPLE_SIZE = 100;

  private static final String ID = "_id";

  private final MongoDatabase database;

  private final Dynamiccrud2Properties properties;

  public MongoDatabaseAdapter(MongoDatabase database, Dynamiccrud2Properties properties) {
    this.database = database;
    this.properties = properties;
  }

  @Override
  public String databaseType() {
    return "MongoDB";
  }

  @Override
  public String targetNoun() {
    return Msg.t("target.collection");
  }

  @Override
  public List<TableRef> listTables() {
    var names = new ArrayList<String>();
    database.listCollectionNames().into(names);
    return names.stream()
        .filter(name -> !name.startsWith("system."))
        .sorted()
        .map(name -> new TableRef(null, null, name))
        .toList();
  }

  /** 문서 SAMPLE_SIZE 개(최근 _id 부터)의 최상위 필드. 타입은 처음 본 값으로 정한다 */
  @Override
  public TableMeta readTable(TableRef ref) {
    var types = new LinkedHashMap<String, Object>();
    types.put(ID, null);
    for (var document :
        collection(ref.name()).find().sort(new Document(ID, -1)).limit(SAMPLE_SIZE)) {
      document.forEach((key, value) -> {
        if (types.get(key) == null) {
          types.put(key, value);
        }
      });
    }
    var columns = new ArrayList<ColumnMeta>();
    types.forEach((name, sample) ->
        columns.add(
            new ColumnMeta(
                name,
                jdbcTypeOf(sample),
                typeNameOf(sample),
                null,
                !ID.equals(name),
                null,
                ID.equals(name),
                null)));
    var indexes = new ArrayList<TableMeta.IndexInfo>();
    var leading = new LinkedHashSet<String>();
    var uniqueKeys = new ArrayList<List<String>>();
    for (var index : collection(ref.name()).listIndexes()) {
      var keys = new ArrayList<>(((Document) index.get("key")).keySet());
      var unique = Boolean.TRUE.equals(index.get("unique"));
      var partial = index.containsKey("partialFilterExpression");
      indexes.add(
          new TableMeta.IndexInfo(
              index.getString("name"), keys, keys.equals(List.of(ID)), unique, partial, false));
      leading.add(keys.getFirst());
      if (unique && !partial && !keys.equals(List.of(ID))) {
        uniqueKeys.add(keys);
      }
    }
    return new TableMeta(ref, columns, List.of(ID), uniqueKeys, leading, indexes);
  }

  private static int jdbcTypeOf(Object sample) {
    if (sample instanceof Integer || sample instanceof Long) {
      return Types.BIGINT;
    }
    if (sample instanceof Double || sample instanceof Decimal128) {
      return Types.DOUBLE;
    }
    if (sample instanceof Boolean) {
      return Types.BOOLEAN;
    }
    if (sample instanceof java.util.Date) {
      return Types.TIMESTAMP;
    }
    if (sample instanceof Document || sample instanceof List<?>) {
      return Types.LONGVARCHAR;
    }
    return Types.VARCHAR;
  }

  /** ValueConverter 가 objectId 면 화면 글자를 ObjectId 로 바꾼다 */
  private static String typeNameOf(Object sample) {
    if (sample == null) {
      return "objectId";
    }
    return switch (sample) {
      case ObjectId id -> "objectId";
      case String text -> "string";
      case Integer number -> "int";
      case Long number -> "long";
      case Double number -> "double";
      case Decimal128 number -> "decimal";
      case Boolean flag -> "bool";
      case java.util.Date date -> "date";
      case Document document -> "object";
      case List<?> list -> "array";
      case Binary binary -> "binData";
      default -> sample.getClass().getSimpleName();
    };
  }

  @Override
  public String generate(QueryKind kind, TableMeta table) {
    var name = table.ref().name();
    var fields = table.columns().stream().map(ColumnMeta::name).filter(f -> !ID.equals(f)).toList();
    var head = "{\"collection\": \"" + name + "\", \"operation\": \"" + MongoQuery.Operation.of(kind).json + "\", ";
    return switch (kind) {
      case READ -> head + "\"filter\": {}, \"sort\": {\"_id\": -1}}";
      case INSERT -> head + "\"document\": " + paramsObject(fields) + "}";
      case UPDATE ->
          head + "\"filter\": {\"_id\": :_id}, \"update\": {\"$set\": " + paramsObject(fields) + "}}";
      case DELETE -> head + "\"filter\": {\"_id\": :_id}}";
    };
  }

  private static String paramsObject(List<String> fields) {
    var parts = new ArrayList<String>();
    for (var field : fields) {
      parts.add("\"" + field + "\": :" + parameterName(field));
    }
    return "{" + String.join(", ", parts) + "}";
  }

  /** 필드 이름 → 파라미터 이름(JDBC DefaultQueryGenerator 와 같은 규칙) */
  private static String parameterName(String field) {
    var name = field.replaceAll("\\W", "_");
    return Character.isDigit(name.charAt(0)) ? "_" + name : name;
  }

  /** 작업 종류 일치 · 형식. 수정 · 삭제는 filter 에 파라미터가 있어야 한다(전체 문서를 바꾸지 않게) */
  @Override
  public void validate(String query, QueryKind kind) {
    var parsed = MongoQuery.parse(query);
    if (parsed.operation().kind != kind) {
      throw new BlueskyException(
          QueryErrorCode.STATEMENT_KIND_MISMATCH, kind, parsed.operation().json);
    }
    if ((kind == QueryKind.UPDATE || kind == QueryKind.DELETE)
        && MongoQuery.parametersIn(parsed.filterOrEmpty()).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.NO_PARAMETER_IN_MONGO_FILTER);
    }
  }

  /** 실행 없는 검사: 형식 + 컬렉션이 있는가. 없으면 BadSqlGrammarException(저장 거부, JDBC 와 같은 처리) */
  @Override
  public void check(String query) {
    var parsed = MongoQuery.parse(query);
    if (listTables().stream().noneMatch(ref -> ref.name().equals(parsed.collection()))) {
      throw new BadSqlGrammarException(
          "mongo",
          query,
          new SQLException("collection not found: " + parsed.collection()));
    }
  }

  @Override
  public List<String> parameterNames(String query) {
    return MongoQuery.parameterNames(query);
  }

  /** 검색 조건 = filter 의 파라미터. 최상위 키(연산자 키 제외)의 조건만 선택으로 바꿀 수 있다 */
  @Override
  public ReadAnalysis analyzeRead(String query) {
    MongoQuery parsed;
    try {
      parsed = MongoQuery.parse(query);
    } catch (BlueskyException e) {
      var conditions = new ArrayList<SearchCondition>();
      MongoQuery.parameterNames(query).forEach(n -> conditions.add(new SearchCondition(n, null, false)));
      return new ReadAnalysis(false, QueryErrorCode.messageOf(e), conditions, List.of(), false, false);
    }
    var conditions = new ArrayList<SearchCondition>();
    var seen = new HashSet<String>();
    parsed.filterOrEmpty().forEach((key, value) -> {
      for (var name : MongoQuery.parametersIn(value)) {
        if (seen.add(name)) {
          conditions.add(
              new SearchCondition(
                  name, key + ": " + toJson(value), !key.startsWith("$")));
        }
      }
    });
    var columns = new ArrayList<ResultColumn>();
    var included = includedFields(parsed.projection());
    if (included.isEmpty()) {
      columns.add(new ResultColumn("*", null, null, parsed.collection(), null, ResultColumn.Kind.ALL));
    } else {
      included.forEach(field ->
          columns.add(
              new ResultColumn(field, null, null, parsed.collection(), field, ResultColumn.Kind.COLUMN)));
    }
    return new ReadAnalysis(
        true, null, conditions, columns, false, parsed.sort() != null && !parsed.sort().isEmpty());
  }

  private static String toJson(Object value) {
    return value instanceof Document document ? document.toJson() : String.valueOf(value);
  }

  /** projection 의 포함 필드(값이 1 · true). _id 는 따로 빼지 않으면 같이 온다 */
  private static List<String> includedFields(Document projection) {
    if (projection == null) {
      return List.of();
    }
    var fields = new ArrayList<String>();
    projection.forEach((key, value) -> {
      if (Boolean.TRUE.equals(value) || (value instanceof Number n && n.intValue() == 1)) {
        fields.add(key);
      }
    });
    if (!fields.isEmpty() && !fields.contains(ID) && !isExcluded(projection.get(ID))) {
      fields.addFirst(ID);
    }
    return fields;
  }

  private static boolean isExcluded(Object value) {
    return Boolean.FALSE.equals(value) || (value instanceof Number n && n.intValue() == 0);
  }

  @Override
  public ReadResult read(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      int page,
      int size) {
    var limit = Math.max(1, Math.min(size, properties.pageMaxRows()));
    var parsed = MongoQuery.parse(query);
    var filter = filterOf(parsed, params, emptyOptionalParams, filters);
    var total =
        collection(parsed.collection())
            .countDocuments(
                filter,
                new CountOptions().maxTime(properties.queryTimeoutSeconds(), TimeUnit.SECONDS));
    var found = find(parsed, filter, sort).skip(Math.max(0, page) * limit).limit(limit);
    return collect(parsed, found, total);
  }

  @Override
  public void export(
      String query,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters,
      Sort sort,
      RowSink sink) {
    var parsed = MongoQuery.parse(query);
    var found =
        find(parsed, filterOf(parsed, params, emptyOptionalParams, filters), sort)
            .limit(properties.excelMaxRows());
    // 결과 이름은 projection 이나 첫 문서들로 정한다 - 엑셀은 문서를 모으지 않으므로 앞 SAMPLE_SIZE 개로 이름을 정하고 나머지는 그 이름만
    var head = new ArrayList<Document>();
    try (var cursor = found.iterator()) {
      while (cursor.hasNext() && head.size() < SAMPLE_SIZE) {
        head.add(cursor.next());
      }
      var labels = labelsOf(parsed, head);
      sink.labels(labels);
      head.forEach(document -> sink.row(values(document, labels)));
      while (cursor.hasNext()) {
        sink.row(values(cursor.next(), labels));
      }
    }
  }

  @Override
  public List<Object> currentRow(TableRef table, List<String> columns, Map<String, Object> keys) {
    var filter = new Document();
    keys.forEach((key, value) -> filter.put(key, ID.equals(key) ? idValue(value) : MongoQuery.toBson(value)));
    var found = new ArrayList<Document>();
    collection(table.name()).find(filter).limit(2).into(found);
    return found.size() == 1 ? values(found.getFirst(), columns) : null;
  }

  @Override
  public List<Object> distinctValues(TableRef table, String column, int limit) {
    var found = new ArrayList<Document>();
    collection(table.name())
        .aggregate(
            List.of(
                new Document("$match", new Document(column, new Document("$ne", null))),
                new Document("$group", new Document(ID, "$" + column)),
                new Document("$sort", new Document(ID, 1)),
                new Document("$limit", limit + 1)))
        .maxTime(properties.queryTimeoutSeconds(), TimeUnit.SECONDS)
        .into(found);
    return found.stream().map(d -> MongoQuery.fromBson(d.get(ID))).toList();
  }

  /** 지운 문서 되돌리기: 행마다 문서 하나(_id 글자가 ObjectId 모양이면 ObjectId). 여러 문서는 한 트랜잭션이 아니다(replica set 필요) */
  @Override
  public int[] insertRows(TableRef table, List<Map<String, Object>> rows) {
    var documents = new ArrayList<Document>(rows.size());
    for (var row : rows) {
      var document = new Document();
      row.forEach((key, value) -> document.put(key, ID.equals(key) ? idValue(value) : MongoQuery.toBson(value)));
      documents.add(document);
    }
    if (!documents.isEmpty()) {
      collection(table.name()).insertMany(documents);
    }
    var counts = new int[documents.size()];
    java.util.Arrays.fill(counts, 1);
    return counts;
  }

  // shortcut: Mongo 실행 계획은 없다(find().explain() 으로 붙일 수 있음), 요청이 있으면 find 연산부터 붙일 것
  @Override
  public Explain explain(String query) {
    throw new UnsupportedOperationException();
  }

  /** 행 키로 넘어온 _id 글자가 ObjectId 모양이면 ObjectId 로 */
  private static Object idValue(Object value) {
    if (value instanceof String text && ObjectId.isValid(text)) {
      return new ObjectId(text);
    }
    return MongoQuery.toBson(value);
  }

  @Override
  public ReadResult sample(
      String query, Map<String, Object> params, Set<String> emptyOptionalParams, int size) {
    var parsed = MongoQuery.parse(query);
    var found =
        find(parsed, filterOf(parsed, params, emptyOptionalParams, List.of()), null)
            .limit(Math.max(1, Math.min(size, properties.pageMaxRows())));
    return collect(parsed, found, -1);
  }

  @Override
  public int[] modify(String query, List<Map<String, Object>> rows) {
    var parsed = MongoQuery.parse(query);
    var collection = collection(parsed.collection());
    var counts = new int[rows.size()];
    for (int i = 0; i < rows.size(); i++) {
      var params = withIds(rows.get(i));
      counts[i] =
          switch (parsed.operation()) {
            case INSERT_ONE -> {
              collection.insertOne(MongoQuery.bind(parsed.document(), params));
              yield 1;
            }
            case UPDATE_MANY ->
                (int)
                    collection
                        .updateMany(
                            MongoQuery.bind(parsed.filterOrEmpty(), params),
                            MongoQuery.<Document>bind(parsed.update(), params))
                        .getMatchedCount();
            case DELETE_MANY ->
                (int)
                    collection
                        .deleteMany(MongoQuery.bind(parsed.filterOrEmpty(), params))
                        .getDeletedCount();
            case FIND -> throw new IllegalArgumentException("find is not a modify operation");
          };
    }
    return counts;
  }

  /** :_id 값이 ObjectId 모양 글자면 ObjectId 로(행 키는 화면에서 글자로 돌아온다) */
  private static Map<String, Object> withIds(Map<String, Object> params) {
    if (!(params.get(ID) instanceof String)) {
      return params;
    }
    var copy = new LinkedHashMap<>(params);
    copy.put(ID, idValue(params.get(ID)));
    return copy;
  }

  // ---- 내부 ----

  private MongoCollection<Document> collection(String name) {
    return database.getCollection(name);
  }

  private static Document filterOf(
      MongoQuery parsed,
      Map<String, Object> params,
      Set<String> emptyOptionalParams,
      List<Filter> filters) {
    var bindParams = withIds(params);
    Document filter =
        MongoQuery.bind(
            MongoQuery.dropOptional(parsed.filterOrEmpty(), emptyOptionalParams), bindParams);
    if (filters == null || filters.isEmpty()) {
      return filter;
    }
    var screen = MongoQuery.filterOf(filters, bindParams);
    return filter.isEmpty() ? screen : new Document("$and", List.of(filter, screen));
  }

  private FindIterable<Document> find(MongoQuery parsed, Document filter, Sort sort) {
    var found =
        collection(parsed.collection())
            .find(filter)
            .maxTime(properties.queryTimeoutSeconds(), TimeUnit.SECONDS);
    if (parsed.projection() != null) {
      found = found.projection(parsed.projection());
    }
    if (sort != null) {
      found = found.sort(new Document(sort.label(), sort.descending() ? -1 : 1));
    } else if (parsed.sort() != null) {
      found = found.sort(parsed.sort());
    }
    return found;
  }

  private static ReadResult collect(MongoQuery parsed, FindIterable<Document> found, long total) {
    var documents = new ArrayList<Document>();
    found.into(documents);
    var labels = labelsOf(parsed, documents);
    var rows = new ArrayList<List<Object>>(documents.size());
    documents.forEach(document -> rows.add(values(document, labels)));
    return new ReadResult(labels, rows, total);
  }

  // shortcut: projection 이 없으면 결과 이름이 쪽마다 다를 수 있다(문서마다 필드가 달라서), 화면 칸이 흔들리면 쿼리에 projection 을 넣게 안내할 것
  private static List<String> labelsOf(MongoQuery parsed, List<Document> documents) {
    var included = includedFields(parsed.projection());
    if (!included.isEmpty()) {
      return included;
    }
    var labels = new LinkedHashSet<String>();
    labels.add(ID);
    documents.forEach(document -> labels.addAll(document.keySet()));
    if (parsed.projection() != null && isExcluded(parsed.projection().get(ID))) {
      labels.remove(ID);
    }
    return new ArrayList<>(labels);
  }

  private static List<Object> values(Document document, List<String> labels) {
    var values = new ArrayList<Object>(labels.size());
    labels.forEach(label -> values.add(MongoQuery.fromBson(document.get(label))));
    return values;
  }
}
