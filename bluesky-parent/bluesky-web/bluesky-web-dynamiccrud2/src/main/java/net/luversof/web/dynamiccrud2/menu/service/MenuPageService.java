package net.luversof.web.dynamiccrud2.menu.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.ReadResult;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter.Sort;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis;
import net.luversof.web.dynamiccrud2.adapter.ReadAnalysis.ResultColumn;
import net.luversof.web.dynamiccrud2.adapter.jdbc.ReadQueryRewriter;
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.domain.ChangeLog;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.MainMenu;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.Project;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.ChangeLogRepository;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;
import net.luversof.web.dynamiccrud2.repository.ProjectRepository;
import net.luversof.web.dynamiccrud2.security.MenuAuthorizer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 사용 화면: 메뉴 목록 · 조회(검색 · 페이징 · 정렬) · 작업 폼 · 실행 · 엑셀. 권한은 진입점마다 확인한다. 화면 제목은 쿼리 별칭 → 필드 라벨 → DB 주석 →
 * 컬럼 이름 순서.
 */
@Service
public class MenuPageService {

  private static final Set<String> RESERVED_PARAMS =
      Set.of(ReadQueryRewriter.LIMIT_PARAM, ReadQueryRewriter.OFFSET_PARAM);

  private final MenuService menuService;
  private final FieldService fieldService;
  private final DbTableRepository dbTableRepository;
  private final ChangeLogRepository changeLogRepository;
  private final DatabaseAdapterResolver resolver;
  private final MenuAuthorizer authorizer;
  private final ObjectMapper objectMapper;
  private final ProjectRepository projectRepository;
  private final MainMenuRepository mainMenuRepository;
  private final MenuRepository menuRepository;

  public MenuPageService(
      MenuService menuService,
      FieldService fieldService,
      DbTableRepository dbTableRepository,
      ChangeLogRepository changeLogRepository,
      DatabaseAdapterResolver resolver,
      MenuAuthorizer authorizer,
      ObjectMapper objectMapper,
      ProjectRepository projectRepository,
      MainMenuRepository mainMenuRepository,
      MenuRepository menuRepository) {
    this.menuService = menuService;
    this.fieldService = fieldService;
    this.dbTableRepository = dbTableRepository;
    this.changeLogRepository = changeLogRepository;
    this.resolver = resolver;
    this.authorizer = authorizer;
    this.objectMapper = objectMapper;
    this.projectRepository = projectRepository;
    this.mainMenuRepository = mainMenuRepository;
    this.menuRepository = menuRepository;
  }

  // ---- 주소(/{프로젝트}/{메인 메뉴}/{메뉴}) ----

  /** 사용 화면 주소의 세 단계 */
  public record MenuPath(Project project, MainMenu mainMenu, Menu menu) {
    public String url() {
      return url(project, mainMenu, menu);
    }

    public static String url(Project project, MainMenu mainMenu, Menu menu) {
      return "/" + project.getCode() + "/" + mainMenu.getCode() + "/" + menu.getCode();
    }
  }

  /** 주소의 code 로 메뉴를 찾는다. 없으면 404 */
  public MenuPath resolve(String projectCode, String mainMenuCode, String menuCode) {
    var project = projectRepository.findByCode(projectCode).orElseThrow(MenuPageService::notFound);
    var mainMenu =
        mainMenuRepository
            .findByProjectIdAndCode(project.getId(), mainMenuCode)
            .orElseThrow(MenuPageService::notFound);
    var menu =
        menuRepository
            .findByMainMenuIdAndCode(mainMenu.getId(), menuCode)
            .orElseThrow(MenuPageService::notFound);
    return new MenuPath(project, mainMenu, menu);
  }

  /** 사이드 메뉴: 그 프로젝트에서 이 사용자가 볼 수 있는 메인 메뉴 · 메뉴(없는 code · 볼 메뉴가 없으면 빈 값) */
  public java.util.Optional<ProjectNode> visibleProject(String projectCode) {
    return visibleTree().stream()
        .filter(node -> node.project().getCode().equals(projectCode))
        .findFirst();
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  // ---- 화면 모델 ----

  public record Column(String label, String header, boolean sortable) {}

  public record SearchField(
      String name, String header, InputType inputType, boolean required, String value) {}

  /** 행: 화면에 보일 값들 + 작업으로 넘길 값(결과 이름 → 문자열 JSON) */
  public record Row(List<String> values, String json) {}

  public record UsePage(
      Menu menu,
      List<Column> columns,
      List<Row> rows,
      long total,
      int page,
      int pageSize,
      String sort,
      boolean descending,
      List<SearchField> searchFields,
      String message,
      List<MenuQuery> insertActions,
      List<MenuQuery> updateActions,
      List<MenuQuery> deleteActions) {}

  public record FormField(
      String name,
      String header,
      InputType inputType,
      boolean required,
      Integer maxLength,
      String value) {}

  public record ActionForm(Menu menu, MenuQuery query, List<FormField> fields, List<String> rows) {}

  public record Export(List<String> headers, List<List<Object>> rows) {}

  /** 로그인한 사용자가 볼 수 있는 메뉴(표시 켬 + 메뉴 권한)만 남긴 트리. 빈 메인 메뉴 · 프로젝트는 뺀다 */
  public List<ProjectNode> visibleTree() {
    return menuService.tree().stream()
        .map(project ->
            new ProjectNode(
                project.project(),
                project.mainMenus().stream()
                    .map(mainMenu ->
                        new MenuService.MainMenuNode(
                            mainMenu.mainMenu(),
                            mainMenu.menus().stream()
                                .filter(m -> m.isEnableDisplay() && authorizer.canRead(m))
                                .toList()))
                    .filter(mainMenu -> !mainMenu.menus().isEmpty())
                    .toList()))
        .filter(project -> !project.mainMenus().isEmpty())
        .toList();
  }

  // ---- 조회 ----

  public UsePage page(
      UUID menuId, Map<String, String> search, int page, String sort, boolean descending) {
    var ctx = context(menuId);
    authorizer.checkRead(ctx.menu());
    var bound = bindSearch(ctx, search);
    var columns = new ArrayList<Column>();
    var rows = new ArrayList<Row>();
    long total = 0;
    var message = bound.message();
    var effectiveSort = sortOf(ctx, sort, descending);
    if (effectiveSort == null) {
      sort = null;
    }
    if (message == null) {
      try {
        var result = read(ctx, bound, effectiveSort, Math.max(page, 0));
        columns.addAll(columnsOf(ctx, result.labels()));
        total = result.total();
        for (var values : result.rows()) {
          rows.add(rowOf(result.labels(), values));
        }
      } catch (DataAccessException e) {
        message = "조회 실패: " + firstLine(e.getMostSpecificCause().getMessage());
      }
    }
    if (columns.isEmpty()) {
      columns.addAll(columnsOf(ctx, analysisLabels(ctx.analysis())));
    }
    var queries =
        ctx.menu().getQueries().stream()
            .filter(q -> q.getKind() != QueryKind.READ && authorizer.canRun(ctx.menu(), q))
            .sorted(
                java.util.Comparator.comparing(MenuQuery::getDisplayOrder)
                    .thenComparing(MenuQuery::getName))
            .toList();
    return new UsePage(
        ctx.menu(),
        columns,
        rows,
        total,
        Math.max(page, 0),
        ctx.menu().getPageSize(),
        sort,
        descending,
        bound.fields(),
        message,
        queries.stream().filter(q -> q.getKind() == QueryKind.INSERT).toList(),
        queries.stream().filter(q -> q.getKind() == QueryKind.UPDATE).toList(),
        queries.stream().filter(q -> q.getKind() == QueryKind.DELETE).toList());
  }

  /** 엑셀: 화면과 같은 검색 · 정렬로 전체(설정 한도까지). 필수 검색 조건이 비면 빈 결과 */
  public Export export(UUID menuId, Map<String, String> search, String sort, boolean descending) {
    var ctx = context(menuId);
    authorizer.checkRead(ctx.menu());
    if (!ctx.menu().isEnableExcel()) {
      throw new org.springframework.security.access.AccessDeniedException("excel disabled");
    }
    var bound = bindSearch(ctx, search);
    if (bound.message() != null) {
      return new Export(List.of(), List.of());
    }
    var result =
        ctx.adapter()
            .export(
                ctx.read().getQueryText(),
                bound.params(),
                bound.emptyOptional(),
                sortOf(ctx, sort, descending));
    return new Export(
        columnsOf(ctx, result.labels()).stream().map(Column::header).toList(), result.rows());
  }

  // ---- 작업 ----

  public ActionForm form(UUID menuId, UUID queryId, List<String> rowJsons) {
    var ctx = context(menuId);
    var query = queryOf(ctx.menu(), queryId);
    authorizer.checkRun(ctx.menu(), query);
    var rows = rowJsons == null ? List.<String>of() : rowJsons;
    var firstRow = rows.size() == 1 ? parseRow(rows.get(0)) : Map.<String, String>of();
    var fields = new ArrayList<FormField>();
    var settings = QueryService.settingsOf(query);
    for (var name : paramNames(ctx, query)) {
      var setting = settings.getOrDefault(name, new ParamSetting(Role.INPUT, true, null));
      if (setting.role() == Role.ROW_KEY) {
        continue;
      }
      var field = ctx.baseFields().get(name);
      var value = firstRow.get(labelOfParam(ctx, name));
      if (value == null) {
        value = setting.defaultValue();
      }
      fields.add(
          new FormField(
              name,
              headerOf(field, name),
              field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType(),
              setting.required(),
              field == null ? null : field.getMaxLength(),
              value));
    }
    return new ActionForm(ctx.menu(), query, fields, rows);
  }

  /**
   * 작업 실행. 선택한 행마다(입력은 한 번) 실행하고 한 트랜잭션으로 묶는다. 변경 이력을 남긴다.
   *
   * @return 영향 행 수 합
   * @throws IllegalArgumentException 입력 · 행 선택 문제(메시지는 화면에 보인다)
   */
  public int run(UUID menuId, UUID queryId, Map<String, String> inputs, List<String> rowJsons) {
    var ctx = context(menuId);
    var query = queryOf(ctx.menu(), queryId);
    authorizer.checkRun(ctx.menu(), query);
    // 저장 검사가 강해지기 전에 저장된 쿼리(WHERE 없는 UPDATE 등)도 실행 때 막는다
    ctx.adapter().validate(query.getQueryText(), query.getKind());
    var settings = QueryService.settingsOf(query);
    var names = paramNames(ctx, query);
    var needsRow = names.stream().anyMatch(n -> settingOf(settings, n).role() == Role.ROW_KEY);
    var rows = new ArrayList<Map<String, String>>();
    if (rowJsons != null) {
      rowJsons.forEach(json -> rows.add(parseRow(json)));
    }
    if (needsRow && rows.isEmpty()) {
      throw new IllegalArgumentException("작업할 행을 선택하세요.");
    }
    if (!needsRow) {
      rows.clear();
      rows.add(Map.of());
    }
    var binds = new ArrayList<Map<String, Object>>();
    var logged = new ArrayList<Map<String, String>>();
    for (var row : rows) {
      var bind = new HashMap<String, Object>();
      var log = new LinkedHashMap<String, String>();
      for (var name : names) {
        var setting = settingOf(settings, name);
        var field = ctx.baseFields().get(name);
        String raw;
        if (setting.role() == Role.ROW_KEY) {
          raw = row.get(labelOfParam(ctx, name));
          if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("선택한 행에서 " + name + " 값을 찾지 못했습니다.");
          }
        } else {
          raw = inputs == null ? null : inputs.get(name);
          if ((raw == null || raw.isEmpty()) && setting.defaultValue() != null) {
            raw = setting.defaultValue();
          }
          if ((raw == null || raw.isEmpty()) && setting.required()) {
            throw new IllegalArgumentException(headerOf(field, name) + " 값을 입력하세요.");
          }
        }
        bind.put(name, ValueConverter.toBindValue(raw, field));
        log.put(name, raw);
      }
      binds.add(bind);
      logged.add(log);
    }
    var counts = ctx.adapter().modify(query.getQueryText(), binds);
    var affected = java.util.Arrays.stream(counts).sum();
    var changeLog = new ChangeLog();
    changeLog.setMenuId(ctx.menu().getId());
    changeLog.setMenuQueryId(query.getId());
    changeLog.setUsername(currentUsername());
    changeLog.setBindValues(Map.of("rows", logged));
    changeLog.setAffectedRows(affected);
    changeLogRepository.save(changeLog);
    return affected;
  }

  // ---- 내부 ----

  private record Context(
      Menu menu,
      MenuQuery read,
      DbTable base,
      DatabaseAdapter adapter,
      ReadAnalysis analysis,
      Map<String, DbField> baseFields,
      Map<String, Map<String, DbField>> otherFields) {}

  private record Bound(
      Map<String, Object> params,
      Set<String> emptyOptional,
      List<SearchField> fields,
      String message) {}

  private Context context(UUID menuId) {
    var menu = menuService.getMenu(menuId);
    var read =
        menu.getQueries().stream()
            .filter(q -> q.getKind() == QueryKind.READ)
            .findFirst()
            .orElseThrow();
    var base = fieldService.get(menu.getDbTableId());
    var adapter = resolver.resolve(base.getConnectionName());
    var baseFields = new HashMap<String, DbField>();
    base.getFields().forEach(f -> baseFields.put(f.getColumnName(), f));
    return new Context(
        menu,
        read,
        base,
        adapter,
        adapter.analyzeRead(read.getQueryText()),
        baseFields,
        new HashMap<>());
  }

  private Bound bindSearch(Context ctx, Map<String, String> search) {
    var settings = QueryService.settingsOf(ctx.read());
    var params = new HashMap<String, Object>();
    var empty = new HashSet<String>();
    var fields = new ArrayList<SearchField>();
    var missing = new ArrayList<String>();
    String message = null;
    for (var name : paramNames(ctx, ctx.read())) {
      var setting = settings.getOrDefault(name, new ParamSetting(Role.SEARCH, true, null));
      var field = ctx.baseFields().get(name);
      var header = headerOf(field, name);
      var raw = search == null ? null : search.get(name);
      fields.add(
          new SearchField(
              name,
              header,
              field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType(),
              setting.required(),
              raw == null ? "" : raw));
      if ((raw == null || raw.isEmpty()) && setting.defaultValue() != null) {
        raw = setting.defaultValue();
      }
      if (raw == null || raw.isEmpty()) {
        if (setting.required()) {
          missing.add(header);
        } else {
          empty.add(name);
        }
        params.put(name, null);
        continue;
      }
      try {
        params.put(name, ValueConverter.toBindValue(raw, field));
      } catch (IllegalArgumentException e) {
        message = e.getMessage();
      }
    }
    if (!missing.isEmpty()) {
      message = "필수 검색 조건을 입력하세요: " + String.join(", ", missing);
    }
    return new Bound(params, empty, fields, message);
  }

  private ReadResult read(Context ctx, Bound bound, Sort sort, int page) {
    return ctx.adapter()
        .read(
            ctx.read().getQueryText(),
            bound.params(),
            bound.emptyOptional(),
            sort,
            page,
            ctx.menu().getPageSize());
  }

  /**
   * 요청한 정렬이 허용된 컬럼이면 Sort, 아니면 null(정렬 값은 화면에서 오므로 여기서 거른다). 결과 열은 쿼리 분석으로 알고, * 로 펼친 열은 메뉴 테이블 필드
   * 이름으로 본다.
   */
  private Sort sortOf(Context ctx, String sort, boolean descending) {
    if (sort == null || sort.isBlank() || !ctx.analysis().parsed()) {
      return null;
    }
    var labels = new ArrayList<>(analysisLabels(ctx.analysis()));
    if (ctx.analysis().columns().stream()
        .anyMatch(
            c -> c.kind() == ResultColumn.Kind.ALL || c.kind() == ResultColumn.Kind.ALL_TABLE)) {
      labels.addAll(ctx.baseFields().keySet());
    }
    var allowed =
        columnsOf(ctx, labels).stream().filter(Column::sortable).map(Column::label).toList();
    return allowed.contains(sort) ? new Sort(sort, descending) : null;
  }

  private List<String> paramNames(Context ctx, MenuQuery query) {
    return ctx.adapter().parameterNames(query.getQueryText()).stream()
        .filter(n -> !RESERVED_PARAMS.contains(n))
        .toList();
  }

  private static ParamSetting settingOf(Map<String, ParamSetting> settings, String name) {
    return settings.getOrDefault(name, new ParamSetting(Role.INPUT, true, null));
  }

  private static List<String> analysisLabels(ReadAnalysis analysis) {
    return analysis.columns().stream()
        .filter(
            c -> c.kind() == ResultColumn.Kind.COLUMN || c.kind() == ResultColumn.Kind.EXPRESSION)
        .map(ResultColumn::label)
        .toList();
  }

  private List<Column> columnsOf(Context ctx, List<String> labels) {
    var byLabel = new HashMap<String, ResultColumn>();
    ctx.analysis().columns().forEach(c -> byLabel.putIfAbsent(c.label(), c));
    var columns = new ArrayList<Column>();
    for (var label : labels) {
      var column = byLabel.get(label);
      var field = column == null ? ctx.baseFields().get(label) : fieldOf(ctx, column);
      var header =
          column != null && column.alias() != null ? column.alias() : headerOf(field, label);
      var sortable =
          ctx.analysis().parsed()
              && field != null
              && field.isSortable()
              && !field.isVirtual()
              && (column == null || column.kind() == ResultColumn.Kind.COLUMN);
      columns.add(new Column(label, header, sortable));
    }
    return columns;
  }

  /** 결과 컬럼의 필드: 메뉴 테이블이면 그 필드, 조인한 다른 테이블이면 그 테이블 필드, 계산 결과면 가상 필드 */
  private DbField fieldOf(Context ctx, ResultColumn column) {
    if (column.kind() == ResultColumn.Kind.EXPRESSION) {
      return ctx.baseFields().get(column.label());
    }
    if (column.tableName() == null
        || column.tableName().equalsIgnoreCase(ctx.base().getTableName())) {
      return ctx.baseFields().get(column.columnName());
    }
    var fields =
        ctx.otherFields()
            .computeIfAbsent(
                column.tableName(),
                table -> {
                  var map = new HashMap<String, DbField>();
                  dbTableRepository
                      .findIdByLocation(
                          ctx.base().getConnectionName(),
                          ctx.base().getCatalogName(),
                          column.tableSchema() == null
                              ? ctx.base().getSchemaName()
                              : column.tableSchema(),
                          table)
                      .flatMap(dbTableRepository::findById)
                      .ifPresent(t -> t.getFields().forEach(f -> map.put(f.getColumnName(), f)));
                  return map;
                });
    return fields.get(column.columnName());
  }

  /** 파라미터 이름 → 조회 결과의 이름(별칭을 썼으면 별칭). 메뉴 테이블의 같은 이름 컬럼을 찾는다 */
  private static String labelOfParam(Context ctx, String name) {
    return ctx.analysis().columns().stream()
        .filter(c -> c.kind() == ResultColumn.Kind.COLUMN)
        .filter(
            c -> c.tableName() == null || c.tableName().equalsIgnoreCase(ctx.base().getTableName()))
        .filter(c -> name.equals(c.columnName()))
        .map(ResultColumn::label)
        .findFirst()
        .orElse(name);
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

  private Row rowOf(List<String> labels, List<Object> values) {
    var texts = new ArrayList<String>(values.size());
    var map = new LinkedHashMap<String, String>();
    for (int i = 0; i < values.size(); i++) {
      var text = ValueConverter.toText(values.get(i));
      texts.add(text);
      map.putIfAbsent(labels.get(i), text);
    }
    return new Row(texts, objectMapper.writeValueAsString(map));
  }

  private Map<String, String> parseRow(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("선택한 행 값을 읽지 못했습니다.", e);
    }
  }

  private static MenuQuery queryOf(Menu menu, UUID queryId) {
    var query =
        menu.getQueries().stream().filter(q -> q.getId().equals(queryId)).findFirst().orElseThrow();
    if (query.getKind() == QueryKind.READ) {
      throw new BlueskyException(QueryErrorCode.STATEMENT_KIND_MISMATCH, "작업", QueryKind.READ);
    }
    return query;
  }

  private static String currentUsername() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null ? "unknown" : authentication.getName();
  }

  private static String firstLine(String message) {
    return message == null ? "" : message.lines().findFirst().orElse(message);
  }
}
