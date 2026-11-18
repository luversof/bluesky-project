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
import net.luversof.web.dynamiccrud2.adapter.Filter;
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
import net.luversof.web.dynamiccrud2.domain.SearchOperator;
import net.luversof.web.dynamiccrud2.domain.SearchSettings;
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

  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(MenuPageService.class);

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
  private final RowSigner rowSigner;

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
      MenuRepository menuRepository,
      RowSigner rowSigner) {
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
    this.rowSigner = rowSigner;
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

  /**
   * 사용 화면 검색 입력.
   *
   * @param params 쿼리 :파라미터 검색칸(s.이름)
   * @param filters 메뉴 검색 설정 칸(f.결과이름, 범위의 까지는 f.결과이름.to)
   * @param keyword 통합 검색어(q)
   */
  public record SearchInput(
      Map<String, String> params, Map<String, String> filters, String keyword) {

    public static final SearchInput EMPTY = new SearchInput(Map.of(), Map.of(), null);
  }

  /** 메뉴 검색 설정으로 만든 검색칸(범위면 value · valueTo 두 칸) */
  public record FilterField(
      String label,
      String header,
      InputType inputType,
      SearchOperator operator,
      String value,
      String valueTo) {}

  /** 통합 검색칸. headers 는 함께 찾는 컬럼 제목(안내용) */
  public record KeywordBox(List<String> headers, String value) {}

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
      List<FilterField> filterFields,
      KeywordBox keyword,
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
      String value,
      boolean readonly) {}

  public record ActionForm(Menu menu, MenuQuery query, List<FormField> fields, List<String> rows) {}

  public record Export(List<String> headers, List<List<Object>> rows) {}

  /**
   * 로그인한 사용자가 볼 수 있는 메뉴(표시 켬 + 메뉴 권한)만 남긴 트리. 숨긴 프로젝트 · 메인 메뉴는 아래까지 통째로 빼고, 볼 메뉴가 없는 메인 메뉴 · 프로젝트도
   * 뺀다
   */
  public List<ProjectNode> visibleTree() {
    return menuService.tree().stream()
        .filter(project -> project.project().isEnableDisplay())
        .map(project ->
            new ProjectNode(
                project.project(),
                project.mainMenus().stream()
                    .filter(mainMenu -> mainMenu.mainMenu().isEnableDisplay())
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

  public UsePage page(UUID menuId, SearchInput search, int page, String sort, boolean descending) {
    var ctx = context(menuId);
    authorizer.checkRead(ctx.menu());
    var bound = bindSearch(ctx, search.params());
    var filtered = bindFilters(ctx, search, bound.params());
    var columns = new ArrayList<Column>();
    var rows = new ArrayList<Row>();
    long total = 0;
    var message = bound.message() != null ? bound.message() : filtered.message();
    var effectiveSort = sortOf(ctx, sort, descending);
    if (effectiveSort == null) {
      sort = null;
    }
    if (message == null) {
      try {
        var result = read(ctx, bound, filtered.filters(), effectiveSort, Math.max(page, 0));
        columns.addAll(columnsOf(ctx, result.labels()));
        total = result.total();
        for (var values : result.rows()) {
          rows.add(rowOf(ctx.menu().getId(), result.labels(), values));
        }
      } catch (DataAccessException e) {
        message = "조회 실패: " + firstLine(e.getMostSpecificCause().getMessage());
      } catch (IllegalArgumentException e) {
        // 검색 조건을 붙일 수 없는 쿼리 등(ReadQueryRewriter)
        message = e.getMessage();
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
        filtered.fields(),
        filtered.keyword(),
        message,
        queries.stream().filter(q -> q.getKind() == QueryKind.INSERT).toList(),
        queries.stream().filter(q -> q.getKind() == QueryKind.UPDATE).toList(),
        queries.stream().filter(q -> q.getKind() == QueryKind.DELETE).toList());
  }

  /** 엑셀: 화면과 같은 검색 · 정렬로 전체(설정 한도까지). 필수 검색 조건이 비면 빈 결과 */
  public Export export(UUID menuId, SearchInput search, String sort, boolean descending) {
    var ctx = context(menuId);
    authorizer.checkRead(ctx.menu());
    if (!ctx.menu().isEnableExcel()) {
      throw new org.springframework.security.access.AccessDeniedException("excel disabled");
    }
    var bound = bindSearch(ctx, search.params());
    var filtered = bindFilters(ctx, search, bound.params());
    if (bound.message() != null || filtered.message() != null) {
      return new Export(List.of(), List.of());
    }
    var result =
        ctx.adapter()
            .export(
                ctx.read().getQueryText(),
                bound.params(),
                bound.emptyOptional(),
                filtered.filters(),
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
    // 여러 행이면 값은 안 쓰지만 서명은 모두 확인한다(고친 행이 폼을 거쳐 실행으로 넘어가지 않게)
    rows.forEach(json -> parseRow(ctx.menu().getId(), json));
    var firstRow =
        rows.size() == 1 ? parseRow(ctx.menu().getId(), rows.get(0)) : Map.<String, String>of();
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
      var readonly = isLocked(query, field);
      fields.add(
          new FormField(
              name,
              headerOf(field, name),
              field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType(),
              setting.required(),
              field == null ? null : field.getMaxLength(),
              readonly && rows.size() > 1 ? null : value,
              readonly));
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
      rowJsons.forEach(json -> rows.add(parseRow(ctx.menu().getId(), json)));
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
        } else if (isLocked(query, field)) {
          // 수정 불가 컬럼은 받은 값을 쓰지 않고 선택한 행의 원래 값을 그대로 넣는다(화면 조작으로 바꾸지 못하게)
          raw = row.get(labelOfParam(ctx, name));
          if (raw == null) {
            throw new IllegalArgumentException(
                headerOf(field, name) + " 은(는) 수정할 수 없는 값인데 조회 결과에 없어 원래 값을 알 수 없습니다.");
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
    try {
      changeLogRepository.save(changeLog);
    } catch (RuntimeException e) {
      // 대상 DB 는 이미 커밋됐다. 실패로 보이면 사용자가 다시 실행해 입력이 중복되므로 성공으로 두고, 이력은 로그로 남긴다
      LOG.error(
          "[CHANGE_LOG_FAILED] menu={} query={} user={} affected={} binds={}",
          changeLog.getMenuId(),
          changeLog.getMenuQueryId(),
          changeLog.getUsername(),
          affected,
          logged,
          e);
    }
    return affected;
  }

  // ---- 내부 ----

  /** 수정 작업에서 바꿀 수 없는 필드인가(입력 작업은 새 행이라 모든 값을 받는다) */
  private static boolean isLocked(MenuQuery query, DbField field) {
    return query.getKind() == QueryKind.UPDATE && field != null && !field.isUpdatable();
  }

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
    if (base.getFields().stream().anyMatch(f -> !f.isVirtual() && f.getUpdatable() == null)) {
      // "수정 가능" 이 생기기 전에 등록된 테이블 - 한 번 다시 읽어 PK · unique 를 수정 불가로 채운다
      base = fieldService.resync(base.getId());
    }
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

  private record Filtered(
      List<Filter> filters, List<FilterField> fields, KeywordBox keyword, String message) {}

  /**
   * 메뉴 검색 설정(관리 화면에서 고른 칸)과 통합 검색을 조회 조건으로 바꾼다. 값은 params 에 _f0 · _f0_to · _q 로 넣는다. 설정한 컬럼이 조회
   * 쿼리에서 사라졌으면 그 칸은 조용히 뺀다(쿼리를 고친 뒤 검색 설정을 다시 저장하기 전).
   */
  private Filtered bindFilters(Context ctx, SearchInput search, Map<String, Object> params) {
    var settings = SearchSettings.of(ctx.menu().getSearchSettings());
    var byLabel = new HashMap<String, ResultColumn>();
    ctx.analysis().columns().stream()
        .filter(c -> c.kind() == ResultColumn.Kind.COLUMN)
        .forEach(c -> byLabel.putIfAbsent(c.label(), c));
    var filters = new ArrayList<Filter>();
    var fields = new ArrayList<FilterField>();
    String message = null;
    var index = 0;
    for (var item : settings.fields()) {
      var column = byLabel.get(item.label());
      if (column == null) {
        continue;
      }
      var field = fieldOf(ctx, column);
      var inputType =
          field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType();
      var allowed = SearchOperator.allowedFor(inputType);
      var operator = allowed.contains(item.operator()) ? item.operator() : allowed.get(0);
      var value = blankToNull(search.filters().get(item.label()));
      var valueTo =
          operator == SearchOperator.RANGE
              ? blankToNull(search.filters().get(item.label() + ".to"))
              : null;
      fields.add(
          new FilterField(
              item.label(),
              column.alias() != null ? column.alias() : headerOf(field, item.label()),
              inputType,
              operator,
              value,
              valueTo));
      var param = "_f" + index;
      var paramTo = "_f" + index + "_to";
      index++;
      try {
        switch (operator) {
          case CONTAINS, STARTS_WITH -> {
            if (value != null) {
              var like = Filter.escapeLike(value) + "%";
              params.put(param, operator == SearchOperator.CONTAINS ? "%" + like : like);
              filters.add(new Filter(List.of(item.label()), operator, param, null));
            }
          }
          case EQUALS -> {
            if (value != null) {
              params.put(param, ValueConverter.toBindValue(value, field));
              filters.add(new Filter(List.of(item.label()), operator, param, null));
            }
          }
          case RANGE -> {
            if (value != null) {
              params.put(param, ValueConverter.toBindValue(value, field));
            }
            if (valueTo != null) {
              params.put(paramTo, ValueConverter.toBindValue(valueTo, field));
            }
            if (value != null || valueTo != null) {
              filters.add(
                  new Filter(
                      List.of(item.label()),
                      operator,
                      value == null ? null : param,
                      valueTo == null ? null : paramTo));
            }
          }
        }
      } catch (IllegalArgumentException e) {
        message = e.getMessage();
      }
    }
    // 통합 검색: 고른 글자 컬럼 중 하나라도 검색어를 포함(대소문자 무시)
    var keywordLabels = new ArrayList<String>();
    var keywordHeaders = new ArrayList<String>();
    for (var label : settings.keyword()) {
      var column = byLabel.get(label);
      if (column == null) {
        continue;
      }
      var field = fieldOf(ctx, column);
      if (!SearchOperator.isText(field == null ? null : field.getInputType())) {
        continue;
      }
      keywordLabels.add(label);
      keywordHeaders.add(column.alias() != null ? column.alias() : headerOf(field, label));
    }
    KeywordBox keyword = null;
    if (!keywordLabels.isEmpty()) {
      var value = blankToNull(search.keyword());
      keyword = new KeywordBox(keywordHeaders, value);
      if (value != null) {
        params.put("_q", "%" + Filter.escapeLike(value) + "%");
        filters.add(new Filter(keywordLabels, SearchOperator.CONTAINS, "_q", null));
      }
    }
    return new Filtered(filters, fields, keyword, message);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private ReadResult read(Context ctx, Bound bound, List<Filter> filters, Sort sort, int page) {
    return ctx.adapter()
        .read(
            ctx.read().getQueryText(),
            bound.params(),
            bound.emptyOptional(),
            filters,
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
                          ctx.base().schemaOf(column.tableSchema()),
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

  private Row rowOf(UUID menuId, List<String> labels, List<Object> values) {
    var texts = new ArrayList<String>(values.size());
    var map = new LinkedHashMap<String, String>();
    for (int i = 0; i < values.size(); i++) {
      var text = ValueConverter.toText(values.get(i));
      texts.add(text);
      map.putIfAbsent(labels.get(i), text);
    }
    return new Row(texts, rowSigner.sign(menuId, objectMapper.writeValueAsString(map)));
  }

  /** 서명을 확인한 행 JSON 만 읽는다({@link RowSigner}) */
  private Map<String, String> parseRow(UUID menuId, String signed) {
    var json = rowSigner.verify(menuId, signed);
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
      throw new BlueskyException(
          QueryErrorCode.STATEMENT_KIND_MISMATCH, "입력 · 수정 · 삭제", QueryKind.READ);
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
