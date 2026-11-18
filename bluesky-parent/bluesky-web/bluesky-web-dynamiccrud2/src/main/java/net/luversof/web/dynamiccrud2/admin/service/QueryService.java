package net.luversof.web.dynamiccrud2.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.adapter.jdbc.ReadQueryRewriter;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;

/** 메뉴 작업 쿼리: 기본 쿼리 초안, 등록 화면의 검사(파라미터 · 결과 컬럼 · 경고), 저장. 작업이 쓰는 필드는 쿼리에서 유추한다. */
@Service
public class QueryService {

  private static final Set<String> RESERVED_PARAMS =
      Set.of(ReadQueryRewriter.LIMIT_PARAM, ReadQueryRewriter.OFFSET_PARAM);

  private final MenuRepository menuRepository;
  private final FieldService fieldService;
  private final DatabaseAdapterResolver resolver;

  public QueryService(
      MenuRepository menuRepository, FieldService fieldService, DatabaseAdapterResolver resolver) {
    this.menuRepository = menuRepository;
    this.fieldService = fieldService;
    this.resolver = resolver;
  }

  /**
   * 쿼리 등록 화면 아래 표.
   *
   * @param error 저장할 수 없는 사유(없으면 null)
   * @param params 파라미터별 설정
   * @param columns 조회 결과 컬럼(조회 작업만)
   * @param warnings 저장은 되지만 알아야 할 것(해석 불가 · 인덱스 없는 검색 등)
   * @param passed DB 에서 확인해 문제가 없을 때 그 안내(확인하지 않았거나 문제가 있으면 null)
   */
  public record QueryInspection(
      String error,
      List<ParamRow> params,
      List<ResultColumn> columns,
      List<String> warnings,
      String passed) {

    /** 쿼리가 비어 아직 검사하지 않은 상태 */
    public static final QueryInspection EMPTY =
        new QueryInspection(null, List.of(), List.of(), List.of(), null);
  }

  /**
   * @param optionalCapable 선택(값이 비면 조건 제거)으로 바꿀 수 있는지(조회 작업만 의미 있음)
   * @param expression 조건 원문(조회 작업의 WHERE 조건)
   */
  public record ParamRow(
      String name, ParamSetting setting, boolean optionalCapable, String expression) {}

  /** 작업 추가 때 보여 줄 기본 쿼리. PK · unique 가 없으면 빈 문자열과 사유를 준다. */
  public record Draft(String queryText, String error) {}

  public Draft draft(Menu menu, QueryKind kind) {
    var base = fieldService.get(menu.getDbTableId());
    var adapter = resolver.resolve(base.getConnectionName());
    try {
      return new Draft(adapter.generate(kind, adapter.readTable(refOf(base))), null);
    } catch (BlueskyException e) {
      return new Draft("", QueryErrorCode.messageOf(e));
    }
  }

  public QueryInspection inspect(
      Menu menu, QueryKind kind, String queryText, Map<String, ParamSetting> submitted) {
    var base = fieldService.get(menu.getDbTableId());
    var adapter = resolver.resolve(base.getConnectionName());
    try {
      adapter.validate(queryText, kind);
    } catch (BlueskyException e) {
      return new QueryInspection(
          QueryErrorCode.messageOf(e), List.of(), List.of(), List.of(), null);
    }
    var meta = adapter.readTable(refOf(base));
    var warnings = new ArrayList<String>();
    var analysis = kind == QueryKind.READ ? adapter.analyzeRead(queryText) : null;
    if (analysis != null && !analysis.parsed()) {
      warnings.add("쿼리 구조를 해석하지 못해 검색 조건은 모두 필수로 고정되고 페이징은 감싸기로 처리합니다: " + analysis.parseError());
    }
    var conditions =
        analysis == null
            ? Map.<String, ReadAnalysis.SearchCondition>of()
            : analysis.conditions().stream()
                .collect(
                    Collectors.toMap(
                        ReadAnalysis.SearchCondition::paramName,
                        Function.identity(),
                        // 같은 파라미터가 여러 조건에 나오면 하나라도 선택 불가면 불가
                        (a, b) -> a.optionalCapable() ? b : a,
                        LinkedHashMap::new));

    var params = new ArrayList<ParamRow>();
    for (var name : adapter.parameterNames(queryText)) {
      if (RESERVED_PARAMS.contains(name)) {
        continue;
      }
      var condition = conditions.get(name);
      var optionalCapable = condition != null && condition.optionalCapable();
      var setting = submitted == null ? null : submitted.get(name);
      if (setting == null) {
        setting = guess(kind, name, meta);
      }
      if (!optionalCapable && kind == QueryKind.READ && !setting.required()) {
        setting = new ParamSetting(setting.role(), true, setting.defaultValue());
      }
      params.add(
          new ParamRow(
              name, setting, optionalCapable, condition == null ? null : condition.expression()));
      if (condition != null) {
        warnIndex(name, condition.expression(), meta, warnings);
      }
    }
    // DB 에서 확인: 조회는 한 행 시험 실행, 입력 · 수정 · 삭제는 실행하지 않고 검사(EXPLAIN 등). 문법 · 없는 테이블 · 컬럼이면 오류
    var error =
        analysis != null
            ? tryEmptyRun(adapter, queryText, params, warnings)
            : checkModify(adapter, queryText, warnings);
    String passed = null;
    if (error == null) {
      passed =
          analysis != null
              ? "DB 에서 한 행을 시험 실행했습니다(파라미터는 NULL 로). 문제가 없습니다."
              : "DB 에서 실행하지 않고 검사했습니다(파라미터는 NULL 로). 문제가 없습니다.";
    }
    return new QueryInspection(
        error, params, analysis == null ? List.of() : analysis.columns(), warnings, passed);
  }

  /** 작업 저장(queryId null = 새 작업). 조회 작업이면 결과 컬럼 중 다른 테이블 · 계산 컬럼을 필드로 등록한다. */
  public Menu save(
      UUID menuId,
      UUID queryId,
      QueryKind kind,
      String name,
      String queryText,
      Map<String, ParamSetting> submitted,
      String authority) {
    var menu = menuRepository.findById(menuId).orElseThrow();
    var base = fieldService.get(menu.getDbTableId());
    var adapter = resolver.resolve(base.getConnectionName());
    adapter.validate(queryText, kind);
    var inspection = inspect(menu, kind, queryText, submitted);
    // DB 에서 확인되지 않는 쿼리(문법 · 없는 테이블 · 컬럼)는 저장하지 않는다
    if (inspection.error() != null) {
      throw new BlueskyException(QueryErrorCode.QUERY_CHECK_FAILED, inspection.error());
    }
    for (var row : inspection.params()) {
      var asked = submitted == null ? null : submitted.get(row.name());
      if (asked != null && !asked.required() && row.setting().required()) {
        throw new BlueskyException(QueryErrorCode.NOT_OPTIONAL_CAPABLE, row.name());
      }
    }

    var query =
        queryId == null
            ? new MenuQuery()
            : menu.getQueries().stream()
                .filter(q -> q.getId().equals(queryId))
                .findFirst()
                .orElseThrow();
    if (queryId == null) {
      if (kind == QueryKind.READ) {
        // 조회는 메뉴당 하나: 새로 만들지 않고 기존 것을 고친다
        query =
            menu.getQueries().stream()
                .filter(q -> q.getKind() == QueryKind.READ)
                .findFirst()
                .orElse(query);
      }
      query.setKind(kind);
      menu.getQueries().add(query);
    }
    query.setName(name == null || name.isBlank() ? defaultName(kind) : name.strip());
    query.setQueryText(queryText.strip());
    query.setAuthority(authority == null || authority.isBlank() ? null : authority.strip());
    var settings = new HashMap<String, Object>();
    inspection.params().forEach(row -> settings.put(row.name(), row.setting().toMap()));
    query.setParamSettings(settings);

    // 메뉴를 만든 뒤 DB 에 늘어난 컬럼도 필드로(작업 초안은 지금 컬럼으로 만들어지므로 필드와 어긋나지 않게)
    fieldService.resync(base.getId());
    if (kind == QueryKind.READ) {
      registerResultFields(base, inspection.columns());
    }
    return menuRepository.save(menu);
  }

  public Menu delete(UUID menuId, UUID queryId) {
    var menu = menuRepository.findById(menuId).orElseThrow();
    var query =
        menu.getQueries().stream().filter(q -> q.getId().equals(queryId)).findFirst().orElseThrow();
    if (query.getKind() == QueryKind.READ) {
      throw new BlueskyException(QueryErrorCode.READ_QUERY_REQUIRED);
    }
    menu.getQueries().remove(query);
    return menuRepository.save(menu);
  }

  /** 저장된 설정을 화면 맵으로 */
  public static Map<String, ParamSetting> settingsOf(MenuQuery query) {
    var result = new LinkedHashMap<String, ParamSetting>();
    if (query != null && query.getParamSettings() != null) {
      query.getParamSettings().forEach((k, v) -> result.put(k, ParamSetting.from(v)));
    }
    return result;
  }

  static String defaultName(QueryKind kind) {
    return switch (kind) {
      case READ -> "조회";
      case INSERT -> "입력";
      case UPDATE -> "수정";
      case DELETE -> "삭제";
    };
  }

  static TableRef refOf(DbTable dbTable) {
    return new TableRef(dbTable.getCatalogName(), dbTable.getSchemaName(), dbTable.getTableName());
  }

  /** 기본 역할 추정: 조회는 검색(필수), 입력 · 수정 · 삭제는 행 키 컬럼 이름이면 행 키 아니면 입력 */
  private static ParamSetting guess(QueryKind kind, String name, TableMeta meta) {
    if (kind == QueryKind.READ) {
      return new ParamSetting(Role.SEARCH, true, null);
    }
    if (kind != QueryKind.INSERT && meta.rowKey().contains(name)) {
      return new ParamSetting(Role.ROW_KEY, true, null);
    }
    var column = meta.columns().stream().filter(c -> c.name().equals(name)).findFirst();
    return new ParamSetting(Role.INPUT, column.map(c -> c.required()).orElse(true), null);
  }

  /** 인덱스 없는 컬럼 검색 · 앞이 % 인 LIKE 는 경고만 한다 */
  // shortcut: 파라미터 이름 = 메뉴 테이블 컬럼 이름일 때만 인덱스를 본다, 조건 왼쪽 식을 정확히 봐야 하면 ReadAnalysis 에 컬럼을 넣을 것
  private static void warnIndex(
      String name, String expression, TableMeta meta, List<String> warnings) {
    var upper = expression.toUpperCase();
    if (upper.contains("LIKE") && (expression.contains("'%'") || expression.contains("'%' ||"))) {
      warnings.add(name + ": 앞이 % 인 LIKE 는 인덱스를 쓰지 못합니다(" + expression + ")");
      return;
    }
    var isColumn = meta.columns().stream().anyMatch(c -> c.name().equals(name));
    if (isColumn && !meta.indexLeadingColumns().contains(name)) {
      warnings.add(name + ": 인덱스 첫 컬럼이 아니라 큰 테이블에서는 느릴 수 있습니다");
    }
  }

  /**
   * 조회 쿼리를 한 행만 실제로 실행해 본다. 문법 · 없는 테이블 · 컬럼(BadSqlGrammarException)은 오류로 돌려주고(저장 거부), 그 밖의 실패(값 형식
   * 등 - 파라미터를 NULL 로 넣어 생길 수 있다)는 경고로만 남긴다.
   *
   * @return 오류 문구(없으면 null)
   */
  private static String tryEmptyRun(
      DatabaseAdapter adapter, String queryText, List<ParamRow> params, List<String> warnings) {
    var bind = new HashMap<String, Object>();
    var empty = new java.util.HashSet<String>();
    for (var row : params) {
      bind.put(row.name(), null);
      if (!row.setting().required()) {
        empty.add(row.name());
      }
    }
    try {
      adapter.read(queryText, bind, empty, null, 0, 1);
    } catch (BadSqlGrammarException e) {
      return "DB 가 쿼리를 받지 않습니다: " + firstLine(e);
    } catch (DataAccessException e) {
      warnings.add("시험 실행 실패(파라미터를 NULL 로 넣어 실행, 쿼리 문법은 통과): " + firstLine(e));
    } catch (IllegalArgumentException e) {
      warnings.add("시험 실행을 하지 못했습니다: " + e.getMessage());
    }
    return null;
  }

  /** 입력 · 수정 · 삭제 쿼리를 실행하지 않고 DB 에서 검사한다(tryEmptyRun 과 같은 오류 · 경고 규칙). */
  private static String checkModify(
      DatabaseAdapter adapter, String queryText, List<String> warnings) {
    try {
      adapter.check(queryText);
    } catch (BadSqlGrammarException e) {
      return "DB 가 쿼리를 받지 않습니다: " + firstLine(e);
    } catch (DataAccessException e) {
      warnings.add("DB 검사 중 오류(쿼리 문법은 통과): " + firstLine(e));
    }
    return null;
  }

  private static String firstLine(DataAccessException e) {
    var message = e.getMostSpecificCause().getMessage();
    return message == null
        ? e.getClass().getSimpleName()
        : message.lines().findFirst().orElse(message);
  }

  private void registerResultFields(DbTable base, List<ResultColumn> columns) {
    var virtual = new ArrayList<String>();
    var others = new LinkedHashMap<TableRef, List<String>>();
    for (var column : columns) {
      switch (column.kind()) {
        case EXPRESSION -> virtual.add(column.label());
        case COLUMN -> {
          if (column.tableName() == null
              || column.tableName().equalsIgnoreCase(base.getTableName())) {
            continue;
          }
          var schema = base.schemaOf(column.tableSchema());
          others
              .computeIfAbsent(
                  new TableRef(base.getCatalogName(), schema, column.tableName()),
                  k -> new ArrayList<>())
              .add(column.columnName());
        }
        default -> {}
      }
    }
    others.forEach(
        (ref, names) -> fieldService.ensureColumns(base.getConnectionName(), ref, names));
    if (!virtual.isEmpty()) {
      fieldService.ensureVirtual(base.getId(), virtual);
    }
  }
}
