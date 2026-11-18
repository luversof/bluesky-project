package net.luversof.web.dynamiccrud2.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;
import net.luversof.web.dynamiccrud2.domain.SearchSettings;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;

/**
 * 메뉴 검색 설정: 조회 결과의 실제 컬럼 중에서 검색칸 · 통합 검색 컬럼을 고른다. 계산 결과처럼 원래 컬럼이 없는 값은 고를 수 없다(WHERE 에 그 식을 붙일 수
 * 없다).
 */
@Service
public class SearchService {

  private final MenuRepository menuRepository;
  private final FieldService fieldService;
  private final DbTableRepository dbTableRepository;
  private final DatabaseAdapterResolver resolver;

  public SearchService(
      MenuRepository menuRepository,
      FieldService fieldService,
      DbTableRepository dbTableRepository,
      DatabaseAdapterResolver resolver) {
    this.menuRepository = menuRepository;
    this.fieldService = fieldService;
    this.dbTableRepository = dbTableRepository;
    this.resolver = resolver;
  }

  /**
   * 고를 수 있는 컬럼 하나(설정 화면 한 줄).
   *
   * @param label 조회 결과 이름
   * @param header 화면 제목
   * @param operators 쓸 수 있는 비교 방식(첫 번째가 기본)
   * @param searchOn 검색칸으로 고름
   * @param operator 고른 방식(안 골랐으면 기본)
   * @param keywordAllowed 통합 검색에 넣을 수 있는 글자 컬럼
   * @param keywordOn 통합 검색에 넣음
   * @param indexUse 방식마다 인덱스를 쓰는지와 그 까닭(화면이 고른 방식의 것을 보인다)
   */
  public record Candidate(
      String label,
      String header,
      InputType inputType,
      List<SearchOperator> operators,
      boolean searchOn,
      SearchOperator operator,
      boolean keywordAllowed,
      boolean keywordOn,
      Map<SearchOperator, IndexUse> indexUse) {}

  /** 검색 방식 하나가 인덱스를 쓰는지(ok)와 화면 문구 */
  public record IndexUse(boolean ok, String text) {}

  /**
   * 인덱스 표 하나(메뉴 테이블 + 조회가 조인한 테이블).
   *
   * @param title 스키마.테이블
   * @param indexes 인덱스(읽지 못했으면 빈 목록)
   * @param error 읽지 못한 까닭(읽었으면 null)
   */
  public record TableIndexes(String title, List<TableMeta.IndexInfo> indexes, String error) {}

  /**
   * @param parsed 조회 쿼리 구조를 해석했는지(못 했으면 결과 컬럼을 몰라 고를 수 없다)
   * @param candidates 고를 수 있는 컬럼(고른 검색칸이 저장 순서로 앞, 나머지는 결과 순서)
   * @param keywordCandidates 통합 검색에 넣을 수 있는 글자 컬럼(넣은 것이 저장 순서로 앞)
   * @param tables 인덱스 표(메뉴 테이블이 먼저)
   */
  public record SearchSetup(
      boolean parsed,
      List<Candidate> candidates,
      List<Candidate> keywordCandidates,
      List<TableIndexes> tables) {}

  public SearchSetup setup(Menu menu) {
    var settings = SearchSettings.of(menu.getSearchSettings());
    var chosen = new HashMap<String, SearchOperator>();
    settings.fields().forEach(item -> chosen.put(item.label(), item.operator()));
    var base = fieldService.get(menu.getDbTableId());
    var read =
        menu.getQueries().stream()
            .filter(q -> q.getKind() == QueryKind.READ)
            .findFirst()
            .orElseThrow();
    var adapter = resolver.resolve(base.getConnectionName());
    var analysis = adapter.analyzeRead(read.getQueryText());
    // 테이블 인덱스: 스키마.테이블(소문자) → 읽은 정보(못 읽으면 meta null)
    var tables = new LinkedHashMap<String, TableIndexes>();
    var metas = new HashMap<String, TableMeta>();
    readIndexes(adapter, base, base.getSchemaName(), base.getTableName(), tables, metas);
    var candidates = new ArrayList<Candidate>();
    var seen = new java.util.HashSet<String>();
    var otherTables = new HashMap<String, Map<String, DbField>>();
    for (var column : analysis.columns()) {
      if (column.kind() != ResultColumn.Kind.COLUMN || !seen.add(column.label())) {
        continue;
      }
      var field = fieldOf(base, column, otherTables);
      var inputType =
          field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType();
      var meta =
          readIndexes(
              adapter,
              base,
              base.schemaOf(column.tableSchema()),
              column.tableName() == null ? base.getTableName() : column.tableName(),
              tables,
              metas);
      var operators = new ArrayList<>(SearchOperator.allowedFor(inputType));
      // 행을 가리키는 값(PK · unique)과 uuid 는 일부만 맞춰 찾을 일이 드물다 - 같음을 기본(첫 번째)으로
      var keyLike =
          (meta != null && meta.isKeyColumn(column.columnName()))
              || (field != null && "uuid".equalsIgnoreCase(field.getDataType()));
      if (keyLike && operators.remove(SearchOperator.EQUALS)) {
        operators.addFirst(SearchOperator.EQUALS);
      }
      var operator = chosen.get(column.label());
      var keywordAllowed = SearchOperator.isText(inputType);
      var indexUse = new LinkedHashMap<SearchOperator, IndexUse>();
      operators.forEach(o -> indexUse.put(o, indexUse(meta, column.columnName(), o)));
      candidates.add(
          new Candidate(
              column.label(),
              column.alias() != null ? column.alias() : headerOf(field, column.label()),
              inputType,
              List.copyOf(operators),
              operator != null,
              operator != null && operators.contains(operator) ? operator : operators.get(0),
              keywordAllowed,
              keywordAllowed && settings.keyword().contains(column.label()),
              indexUse));
    }
    // 화면 순서: 고른 검색칸은 저장한 순서로 앞에, 나머지는 결과 순서. 통합 검색 대상도 저장한 순서로 앞에
    var fieldOrder = settings.fields().stream().map(SearchSettings.Item::label).toList();
    var keywordCandidates =
        candidates.stream()
            .filter(Candidate::keywordAllowed)
            .sorted(java.util.Comparator.comparingInt(c -> rank(settings.keyword(), c.label())))
            .toList();
    candidates.sort(java.util.Comparator.comparingInt(c -> rank(fieldOrder, c.label())));
    return new SearchSetup(
        analysis.parsed(), candidates, keywordCandidates, List.copyOf(tables.values()));
  }

  /** 저장한 목록 안 위치(없으면 맨 뒤 - 안정 정렬이라 결과 순서를 지킨다) */
  private static int rank(List<String> order, String label) {
    var index = order.indexOf(label);
    return index < 0 ? Integer.MAX_VALUE : index;
  }

  /** 테이블 인덱스를 한 번만 읽어 표 목록에 더한다. 못 읽으면 null(표에는 까닭을 남긴다) */
  private static TableMeta readIndexes(
      DatabaseAdapter adapter,
      DbTable base,
      String schema,
      String table,
      Map<String, TableIndexes> tables,
      Map<String, TableMeta> metas) {
    var key = ((schema == null ? "" : schema) + "." + table).toLowerCase();
    if (tables.containsKey(key)) {
      return metas.get(key);
    }
    var title = (schema == null ? "" : schema + ".") + table;
    try {
      var meta = adapter.readTable(new TableRef(base.getCatalogName(), schema, table));
      metas.put(key, meta);
      tables.put(key, new TableIndexes(title, meta.indexes(), null));
      return meta;
    } catch (RuntimeException e) {
      tables.put(key, new TableIndexes(title, List.of(), "인덱스 정보를 읽지 못했습니다"));
      return null;
    }
  }

  /**
   * 검색 방식 하나가 인덱스를 쓰는지. 실행 쿼리 모양을 따른다(ReadQueryRewriter): 포함 · 시작은 LOWER(컬럼) LIKE 라 컬럼에 인덱스가 있어도 못
   * 쓴다. 같음 · 범위는 그 컬럼이 어떤 인덱스의 첫 컬럼이면 쓴다. 실제로 쓸지는 DB 가 데이터 분포를 보고 정하므로 짐작이다.
   */
  static IndexUse indexUse(TableMeta meta, String column, SearchOperator operator) {
    if (operator == SearchOperator.CONTAINS) {
      return new IndexUse(false, "부분 일치(%값%)는 인덱스를 못 씀");
    }
    if (operator == SearchOperator.STARTS_WITH) {
      return new IndexUse(false, "앞부분 일치는 대소문자 무시 비교라 인덱스를 못 씀");
    }
    if (meta == null) {
      return new IndexUse(false, "인덱스 정보를 읽지 못함");
    }
    String later = null;
    for (var index : meta.indexes()) {
      if (index.expression() || index.columns().isEmpty()) {
        continue;
      }
      if (index.columns().get(0).equals(column)) {
        return new IndexUse(
            true, "인덱스 " + index.name() + (index.partial() ? " 사용(부분 인덱스 - 조건에 맞는 행만)" : " 사용"));
      }
      if (later == null && index.columns().contains(column)) {
        later = index.name();
      }
    }
    return later == null
        ? new IndexUse(false, "인덱스 없음 · 큰 테이블에서 느림")
        : new IndexUse(false, "인덱스 " + later + " 의 첫 컬럼이 아니라 단독으로는 못 씀");
  }

  /**
   * 저장. 고를 수 없는 컬럼 · 방식은 버린다(화면 값을 그대로 믿지 않는다). 아무것도 안 고르면 null 로 지운다.
   *
   * @param operators 결과 이름 → 고른 방식(검색칸으로 고른 것만, 방식이 null 이면 그 컬럼 기본)
   * @param keyword 통합 검색에 넣은 결과 이름
   */
  public void save(UUID menuId, Map<String, SearchOperator> operators, List<String> keyword) {
    var menu = menuRepository.findById(menuId).orElseThrow();
    var byLabel = new HashMap<String, Candidate>();
    setup(menu).candidates().forEach(c -> byLabel.put(c.label(), c));
    // 받은 순서(화면에서 끌어 정한 순서)대로 남긴다
    var fields = new ArrayList<SearchSettings.Item>();
    operators.forEach((label, operator) -> {
      var candidate = byLabel.get(label);
      if (candidate != null) {
        fields.add(
            new SearchSettings.Item(
                label,
                candidate.operators().contains(operator)
                    ? operator
                    : candidate.operators().get(0)));
      }
    });
    var keywordLabels = new ArrayList<String>();
    for (var label : keyword) {
      var candidate = byLabel.get(label);
      if (candidate != null && candidate.keywordAllowed() && !keywordLabels.contains(label)) {
        keywordLabels.add(label);
      }
    }
    var settings = new SearchSettings(fields, keywordLabels);
    menu.setSearchSettings(settings.isEmpty() ? null : settings.toMap());
    menuRepository.save(menu);
  }

  /** 결과 컬럼의 필드: 메뉴 테이블이면 그 필드, 조인한 다른 테이블이면 등록된 그 테이블 필드 */
  private DbField fieldOf(
      DbTable base, ResultColumn column, Map<String, Map<String, DbField>> otherTables) {
    if (column.tableName() == null || column.tableName().equalsIgnoreCase(base.getTableName())) {
      return base.getFields().stream()
          .filter(f -> f.getColumnName().equals(column.columnName()))
          .findFirst()
          .orElse(null);
    }
    return otherTables
        .computeIfAbsent(
            column.tableName(),
            table -> {
              var map = new HashMap<String, DbField>();
              dbTableRepository
                  .findIdByLocation(
                      base.getConnectionName(),
                      base.getCatalogName(),
                      base.schemaOf(column.tableSchema()),
                      table)
                  .flatMap(dbTableRepository::findById)
                  .ifPresent(t -> t.getFields().forEach(f -> map.put(f.getColumnName(), f)));
              return map;
            })
        .get(column.columnName());
  }

  private static String headerOf(DbField field, String fallback) {
    if (field == null) {
      return fallback;
    }
    if (field.getLabel() != null && !field.getLabel().isBlank()) {
      return field.getLabel();
    }
    if (field.getDbComment() != null && !field.getDbComment().isBlank()) {
      return field.getDbComment();
    }
    return field.getColumnName();
  }
}
