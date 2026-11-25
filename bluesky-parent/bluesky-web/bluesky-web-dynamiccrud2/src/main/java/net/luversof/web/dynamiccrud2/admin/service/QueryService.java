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
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Action;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Target;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;

/** 메뉴 작업 쿼리: 기본 쿼리 초안, 등록 화면의 검사(파라미터 · 결과 컬럼 · 경고), 저장. 작업이 쓰는 필드는 쿼리에서 유추한다. */
@Service
public class QueryService {

  private static final Set<String> RESERVED_PARAMS =
      Set.of(ReadQueryRewriter.LIMIT_PARAM, ReadQueryRewriter.OFFSET_PARAM);

  private final MenuRepository menuRepository;
  private final FieldService fieldService;
  private final DatabaseAdapterResolver resolver;
  private final SettingLogService settingLog;

  public QueryService(
      MenuRepository menuRepository,
      FieldService fieldService,
      DatabaseAdapterResolver resolver,
      SettingLogService settingLog) {
    this.menuRepository = menuRepository;
    this.fieldService = fieldService;
    this.resolver = resolver;
    this.settingLog = settingLog;
  }

  /**
   * 쿼리 등록 화면 아래 표.
   *
   * @param error 저장할 수 없는 사유(없으면 null)
   * @param params 파라미터별 설정
   * @param columns 조회 결과 컬럼(조회 작업만)
   * @param views 결과 컬럼마다 사용 화면에서 보일 모습(필드 설정을 적용한 제목 · 형식 · 정렬)
   * @param warnings 저장은 되지만 알아야 할 것(해석 불가 · 인덱스 없는 검색 등)
   * @param passed DB 에서 확인해 문제가 없을 때 그 안내(확인하지 않았거나 문제가 있으면 null)
   * @param preview 조회 작업 시험 실행 결과 앞 몇 행(조회가 아니거나 실행하지 못했으면 null)
   */
  public record QueryInspection(
      String error,
      List<ParamRow> params,
      List<ResultColumn> columns,
      List<ColumnView> views,
      List<String> warnings,
      String passed,
      DatabaseAdapter.ReadResult preview) {

    /** 쿼리가 비어 아직 검사하지 않은 상태 */
    public static final QueryInspection EMPTY =
        new QueryInspection(null, List.of(), List.of(), List.of(), List.of(), null, null);

    /** 필수 파라미터가 있으면 값 없이(NULL) 실행해 미리 보기가 비어 보일 수 있다 */
    public boolean previewWithoutValues() {
      return params.stream().anyMatch(p -> p.setting().required());
    }

    /** 미리 보기 열(결과 이름)의 모습. 분석하지 못한 열(* 등)은 이름 그대로 · 글자 */
    public ColumnView viewOf(String label) {
      return views.stream()
          .filter(v -> v.label().equals(label))
          .findFirst()
          .orElse(new ColumnView(label, label, null, InputType.TEXT, false, null, null, false));
    }

    public List<FieldTable> fieldTables() {
      return views.stream()
          .filter(v -> v.tableId() != null)
          .map(v -> new FieldTable(v.tableId(), v.tableName()))
          .distinct()
          .toList();
    }
  }

  /**
   * 결과 컬럼 하나가 사용 화면에서 보일 모습 - 쿼리 화면에서 필드 설정과의 연결을 보인다.
   *
   * @param header 화면 제목(필드 라벨 → 별칭 → DB 주석 → 컬럼 이름)
   * @param alias 쿼리 별칭(없으면 null)
   * @param tableId 필드가 있는 테이블(필드 관리 화면 링크, 등록된 필드가 없으면 null - 조회를 저장하면 등록된다)
   * @param tableName 그 테이블 이름(조인해 테이블이 여럿일 때 단추를 나눠 보인다)
   * @param labelOverAlias 별칭이 있지만 필드 라벨이 제목이 된다(안내용)
   */
  public record ColumnView(
      String label,
      String header,
      String alias,
      InputType inputType,
      boolean sortable,
      UUID tableId,
      String tableName,
      boolean labelOverAlias) {}

  /** 결과 컬럼 표 위 "필드 관리" 단추: 결과 컬럼이 쓰는 등록된 테이블(나온 순서, 보통 메뉴 테이블 하나) */
  public record FieldTable(UUID id, String name) {}

  /** 조회 쿼리 미리 보기 행 수 */
  static final int PREVIEW_ROWS = 5;

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

  /**
   * 쿼리 화면 "실행 계획" 결과.
   *
   * @param lines 계획 줄(오류면 빈 목록)
   * @param approximate 파라미터를 NULL 로 넣어 본 계획
   * @param error 보일 수 없을 때 사유
   */
  public record ExplainResult(List<String> lines, boolean approximate, String error) {}

  /** 실행 계획(실행하지 않는다). 쿼리 형식 검사는 검사 단추와 같다 */
  public ExplainResult explain(Menu menu, QueryKind kind, String queryText) {
    var adapter = resolver.resolve(fieldService.get(menu.getDbTableId()).getConnectionName());
    try {
      adapter.validate(queryText, kind);
      var explain = adapter.explain(queryText);
      return new ExplainResult(explain.lines(), explain.approximate(), null);
    } catch (BlueskyException e) {
      return new ExplainResult(List.of(), false, QueryErrorCode.messageOf(e));
    } catch (UnsupportedOperationException e) {
      return new ExplainResult(List.of(), false, Msg.t("inspect.explain.unsupported", adapter.databaseType()));
    } catch (DataAccessException e) {
      return new ExplainResult(List.of(), false, Msg.t("inspect.explain.failed", firstLine(e)));
    }
  }

  /** 쿼리 화면 검사(조회는 앞 PREVIEW_ROWS 행 미리 보기) */
  public QueryInspection inspect(
      Menu menu, QueryKind kind, String queryText, Map<String, ParamSetting> submitted) {
    return inspect(menu, kind, queryText, submitted, false);
  }

  /**
   * @param forSave 저장 전 검사(미리 보기 없이 사용 화면과 같은 한 행 실행)
   */
  private QueryInspection inspect(
      Menu menu,
      QueryKind kind,
      String queryText,
      Map<String, ParamSetting> submitted,
      boolean forSave) {
    var base = fieldService.get(menu.getDbTableId());
    var adapter = resolver.resolve(base.getConnectionName());
    try {
      adapter.validate(queryText, kind);
    } catch (BlueskyException e) {
      return new QueryInspection(
          QueryErrorCode.messageOf(e), List.of(), List.of(), List.of(), List.of(), null, null);
    }
    var meta = adapter.readTable(refOf(base));
    var warnings = new ArrayList<String>();
    var analysis = kind == QueryKind.READ ? adapter.analyzeRead(queryText) : null;
    if (analysis != null && !analysis.parsed()) {
      warnings.add(Msg.t("inspect.unparsed", analysis.parseError()));
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
    // DB 에서 확인: 조회는 앞 몇 행 시험 실행(미리 보기로 보인다), 입력 · 수정 · 삭제는 실행하지 않고 검사(EXPLAIN 등). 문법 · 없는 테이블 ·
    // 컬럼이면 오류
    var tried =
        analysis != null
            ? tryRun(adapter, queryText, params, warnings, forSave)
            : new TryRun(checkModify(adapter, queryText, warnings), null);
    String passed = null;
    if (tried.error() == null) {
      passed = analysis != null ? Msg.t("inspect.passed.read") : Msg.t("inspect.passed.modify");
    }
    var columns = analysis == null ? List.<ResultColumn>of() : analysis.columns();
    return new QueryInspection(
        tried.error(),
        params,
        columns,
        forSave ? List.of() : viewsOf(base, analysis, columns),
        warnings,
        passed,
        tried.preview());
  }

  /** 결과 컬럼마다 사용 화면 모습(MenuPageService 목록 열과 같은 규칙: FieldService.headerOf · 정렬은 해석된 실제 컬럼만) */
  private List<ColumnView> viewsOf(
      DbTable base, ReadAnalysis analysis, List<ResultColumn> columns) {
    var fields = fieldService.lookup(base);
    var views = new ArrayList<ColumnView>();
    for (var column : columns) {
      if (column.kind() == ResultColumn.Kind.ALL || column.kind() == ResultColumn.Kind.ALL_TABLE) {
        continue;
      }
      var field = fields.fieldOf(column);
      var table = field == null ? null : fields.tableOf(column).orElse(null);
      var hasLabel = field != null && field.getLabel() != null && !field.getLabel().isBlank();
      views.add(
          new ColumnView(
              column.label(),
              FieldService.headerOf(field, column.alias(), column.label()),
              column.alias(),
              field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType(),
              analysis.parsed()
                  && field != null
                  && field.isSortable()
                  && !field.isVirtual()
                  && column.kind() == ResultColumn.Kind.COLUMN,
              table == null ? null : table.getId(),
              table == null ? null : table.getTableName(),
              hasLabel && column.alias() != null && !column.alias().equals(field.getLabel())));
    }
    return views;
  }

  private record TryRun(String error, DatabaseAdapter.ReadResult preview) {}

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
    var inspection = inspect(menu, kind, queryText, submitted, true);
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
    // 설정 변경 이력: 새 쿼리면 만들기, 있던 쿼리(조회 포함)면 바뀐 항목만
    var before = query.getId() == null ? null : SettingLogService.snapshot(query);
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
    var saved = menuRepository.save(menu);
    // 저장 전 콜백이 새 쿼리 id 를 채우므로 저장된 메뉴에서 다시 찾는다(이름 · 종류 · 있던 id)
    var edited = query;
    var savedQuery =
        saved.getQueries().stream()
            .filter(q -> q.getName().equals(edited.getName()) && q.getKind() == edited.getKind())
            .filter(q -> edited.getId() == null || q.getId().equals(edited.getId()))
            .findFirst()
            .orElse(edited);
    settingLog.record(
        Target.QUERY,
        before == null ? Action.CREATE : Action.UPDATE,
        savedQuery.getId(),
        savedQuery.getName(),
        null,
        menuId,
        before,
        SettingLogService.snapshot(savedQuery));
    return saved;
  }

  public Menu delete(UUID menuId, UUID queryId) {
    var menu = menuRepository.findById(menuId).orElseThrow();
    var query =
        menu.getQueries().stream().filter(q -> q.getId().equals(queryId)).findFirst().orElseThrow();
    if (query.getKind() == QueryKind.READ) {
      throw new BlueskyException(QueryErrorCode.READ_QUERY_REQUIRED);
    }
    menu.getQueries().remove(query);
    var saved = menuRepository.save(menu);
    settingLog.record(
        Target.QUERY,
        Action.DELETE,
        queryId,
        query.getName(),
        null,
        menuId,
        SettingLogService.snapshot(query),
        null);
    return saved;
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
      case READ -> Msg.t("query.kind.READ");
      case INSERT -> Msg.t("query.kind.INSERT");
      case UPDATE -> Msg.t("query.kind.UPDATE");
      case DELETE -> Msg.t("query.kind.DELETE");
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
      warnings.add(Msg.t("inspect.leadingWildcard", name, expression));
      return;
    }
    var isColumn = meta.columns().stream().anyMatch(c -> c.name().equals(name));
    if (isColumn && !meta.indexLeadingColumns().contains(name)) {
      warnings.add(Msg.t("inspect.noIndex", name));
    }
  }

  /**
   * 조회 쿼리를 앞 몇 행만 실제로 실행해 본다(선택 파라미터는 조건을 빼고, 필수는 NULL). 문법 · 없는 테이블 · 컬럼(BadSqlGrammarException)은
   * 오류로 돌려주고(저장 거부), 그 밖의 실패(값 형식 등 - 파라미터를 NULL 로 넣어 생길 수 있다)는 경고로만 남긴다.
   *
   * @return 오류 문구(없으면 null)와 미리 보기(실행하지 못했으면 null)
   */
  private static TryRun tryRun(
      DatabaseAdapter adapter,
      String queryText,
      List<ParamRow> params,
      List<String> warnings,
      boolean forSave) {
    var bind = new HashMap<String, Object>();
    var empty = new java.util.HashSet<String>();
    for (var row : params) {
      bind.put(row.name(), null);
      if (!row.setting().required()) {
        empty.add(row.name());
      }
    }
    try {
      if (forSave) {
        // 저장: 사용 화면과 똑같이(건수 쿼리 포함) 한 행 실행 - 건수 쿼리로 감쌀 때만 나는 오류(MySQL 결과 이름 중복 등)도 저장 전에 막는다
        adapter.read(queryText, bind, empty, null, 0, 1);
        return new TryRun(null, null);
      }
      // 테스트 단추: 자주 누르므로 건수를 세지 않고 앞 몇 행만(큰 테이블 COUNT 를 피한다)
      return new TryRun(null, adapter.sample(queryText, bind, empty, PREVIEW_ROWS));
    } catch (BadSqlGrammarException e) {
      return new TryRun(Msg.t("inspect.rejected", firstLine(e)), null);
    } catch (DataAccessException e) {
      warnings.add(Msg.t("inspect.tryFailed", firstLine(e)));
    } catch (IllegalArgumentException e) {
      warnings.add(Msg.t("inspect.tryError", e.getMessage()));
    }
    return new TryRun(null, null);
  }

  /** 입력 · 수정 · 삭제 쿼리를 실행하지 않고 DB 에서 검사한다(tryEmptyRun 과 같은 오류 · 경고 규칙). */
  private static String checkModify(
      DatabaseAdapter adapter, String queryText, List<String> warnings) {
    try {
      adapter.check(queryText);
    } catch (BadSqlGrammarException e) {
      return Msg.t("inspect.rejected", firstLine(e));
    } catch (DataAccessException e) {
      warnings.add(Msg.t("inspect.checkError", firstLine(e)));
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
