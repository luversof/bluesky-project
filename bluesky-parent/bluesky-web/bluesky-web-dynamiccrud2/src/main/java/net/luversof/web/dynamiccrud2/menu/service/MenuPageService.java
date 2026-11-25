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
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.adapter.jdbc.ReadQueryRewriter;
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService.ProjectNode;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.config.Msg;
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

  /**
   * 주소의 code 로 메뉴를 찾는다. 없거나, 숨긴 프로젝트 · 메인 메뉴 아래면 404(2026-11-24 사용자 요청: 숨기면 아래 메뉴도 숨긴 것으로 본다). 메뉴
   * 자체의 "메뉴 목록에 표시" 는 목록에서만 빼는 값이라 주소로는 열린다(복사한 메뉴는 숨김으로 만들어져 보이기 전에 주소로 확인)
   */
  public MenuPath resolve(String projectCode, String mainMenuCode, String menuCode) {
    var project =
        projectRepository
            .findByCode(projectCode)
            .filter(Project::isEnableDisplay)
            .orElseThrow(MenuPageService::notFound);
    var mainMenu =
        mainMenuRepository
            .findByProjectIdAndCode(project.getId(), mainMenuCode)
            .filter(MainMenu::isEnableDisplay)
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

  /**
   * @param inputType 필드 입력 형식 - 목록 칸 표시 형식(_components/cellValue)에 쓴다
   * @param options 필드 값 목록(저장값 → 표시, {@link DbField#valueOptionMap()}). 비면 원래 값 그대로
   * @param dateFormat 날짜 · 일시 표시 형식({@link DbField#getDateFormat()}). null 이면 기본 모양
   * @param json json · jsonb 컬럼 - 목록 칸은 한 줄로 줄이고 올리면 보기 좋게 펼친 전체(_components/cellValue)
   */
  public record Column(
      String label,
      String header,
      boolean sortable,
      InputType inputType,
      Map<String, String> options,
      String dateFormat,
      boolean json) {}

  public record SearchField(
      String name,
      String header,
      InputType inputType,
      boolean required,
      String value,
      Map<String, String> options) {}

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

  /**
   * 메뉴 검색 설정으로 만든 검색칸(범위면 value · valueTo 두 칸).
   *
   * @param options 셀렉트 목록(저장값 → 표시). 비면 입력칸
   * @param tooMany "DB 값 전체" 인데 값이 너무 많아 입력칸으로 보인다
   * @param required 채워야 목록을 보인다(검색 설정의 필수)
   */
  public record FilterField(
      String label,
      String header,
      InputType inputType,
      SearchOperator operator,
      String value,
      String valueTo,
      Map<String, String> options,
      boolean tooMany,
      boolean required) {}

  /** 검색 셀렉트 "DB 값 전체" 의 최대 개수 - 넘으면 입력칸으로 */
  static final int DISTINCT_LIMIT = 200;

  /** 같은 컬럼 값을 사용 화면을 열 때마다 읽지 않게(1 분) */
  private static final long DISTINCT_TTL_MILLIS = 60_000;

  private record DistinctValues(long readAt, List<String> values) {}

  // shortcut: 캐시가 pod 메모리에만 있어 replica 마다 따로 읽는다(1 분), 값이 자주 바뀌는 큰 테이블이 생기면 TTL 을 설정으로 뺄 것
  private final Map<String, DistinctValues> distinctCache =
      new java.util.concurrent.ConcurrentHashMap<>();

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
      boolean readonly,
      Map<String, String> options,
      boolean json) {}

  private static Map<String, String> optionsOf(DbField field) {
    return field == null ? Map.of() : field.valueOptionMap();
  }

  public record ActionForm(Menu menu, MenuQuery query, List<FormField> fields, List<String> rows) {}

  /**
   * JSON 호출 설명 한 줄(메뉴 관리 화면 "JSON 호출"). path 는 사용 화면 주소 뒤에 붙는다.
   *
   * @param rows 작업에 대상 행(목록 JSON 의 row 값)이 필요한가 - 쿼리에 행 키 파라미터가 있으면
   */
  public record ApiCall(
      QueryKind kind,
      String name,
      String method,
      String path,
      List<ApiParam> params,
      boolean rows) {}

  /** JSON 호출 파라미터. 조회는 주소의 ?이름=값, 작업은 본문 inputs 의 키 */
  public record ApiParam(String name, boolean required, String description) {}

  /**
   * 이 메뉴의 JSON 호출(조회 + 입력 · 수정 · 삭제) 설명. 사용 화면과 같은 규칙(검색칸 · 메뉴 검색 설정 · 파라미터 역할)에서 만들어 쿼리를 고치면 같이
   * 바뀐다. 쿼리를 실행하지 않는다
   */
  public List<ApiCall> apiDoc(UUID menuId) {
    var ctx = context(menuId);
    var bound = bindSearch(ctx, Map.of());
    var filtered = bindFilters(ctx, SearchInput.EMPTY, bound.params());
    var read = new ArrayList<ApiParam>();
    bound.fields().forEach(f -> read.add(new ApiParam("s." + f.name(), f.required(), f.header())));
    filtered
        .fields()
        .forEach(f -> {
          // 셀렉트 검색칸은 고를 수 있는 저장값을 앞 몇 개만 보인다
          var choices =
              f.options().isEmpty()
                  ? ""
                  : " · "
                      + Msg.t(
                          "menu.api.param.choices",
                          String.join(", ", f.options().keySet().stream().limit(10).toList())
                              + (f.options().size() > 10 ? ", …" : ""));
          read.add(
              new ApiParam(
                  "f." + f.label(),
                  f.required(),
                  f.header() + " (" + f.operator().name() + ")" + choices));
          if (f.operator() == SearchOperator.RANGE) {
            read.add(new ApiParam("f." + f.label() + ".to", false, f.header()));
          }
        });
    if (filtered.keyword() != null) {
      read.add(new ApiParam("q", false, String.join(", ", filtered.keyword().headers())));
    }
    read.add(new ApiParam("page", false, Msg.t("menu.api.param.page")));
    read.add(new ApiParam("sort", false, Msg.t("menu.api.param.sort")));
    read.add(new ApiParam("desc", false, Msg.t("menu.api.param.desc")));
    var calls = new ArrayList<ApiCall>();
    calls.add(new ApiCall(QueryKind.READ, ctx.read().getName(), "GET", "/api", read, false));
    ctx.menu().getQueries().stream()
        .filter(q -> q.getKind() != QueryKind.READ)
        .sorted(
            java.util.Comparator.comparing(MenuQuery::getKind)
                .thenComparing(MenuQuery::getDisplayOrder)
                .thenComparing(MenuQuery::getName))
        .forEach(query -> {
          var settings = QueryService.settingsOf(query);
          var inputs = new ArrayList<ApiParam>();
          var needsRow = false;
          for (var name : paramNames(ctx, query)) {
            var setting = settingOf(settings, name);
            if (setting.role() == Role.ROW_KEY) {
              needsRow = true;
            } else {
              inputs.add(
                  new ApiParam(
                      name, setting.required(), headerOf(ctx.baseFields().get(name), name)));
            }
          }
          calls.add(
              new ApiCall(
                  query.getKind(),
                  query.getName(),
                  "POST",
                  "/api/action/" + query.getId(),
                  inputs,
                  needsRow));
        });
    return calls;
  }

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
        message = Msg.t("menu.page.readFailed", firstLine(e.getMostSpecificCause().getMessage()));
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

  /**
   * 엑셀: 화면과 같은 검색 · 정렬로 전체(설정 한도까지)를 읽는 대로 sink 에 넘긴다(labels 는 화면 열 제목). 필수 검색 조건이 비면 아무것도 넘기지 않는다
   */
  public void export(
      UUID menuId,
      SearchInput search,
      String sort,
      boolean descending,
      DatabaseAdapter.RowSink sink) {
    var ctx = context(menuId);
    authorizer.checkRead(ctx.menu());
    if (!ctx.menu().isEnableExcel()) {
      throw new org.springframework.security.access.AccessDeniedException("excel disabled");
    }
    var bound = bindSearch(ctx, search.params());
    var filtered = bindFilters(ctx, search, bound.params());
    if (bound.message() != null || filtered.message() != null) {
      return;
    }
    ctx.adapter()
        .export(
            ctx.read().getQueryText(),
            bound.params(),
            bound.emptyOptional(),
            filtered.filters(),
            sortOf(ctx, sort, descending),
            new DatabaseAdapter.RowSink() {
              @Override
              public void labels(List<String> labels) {
                sink.labels(columnsOf(ctx, labels).stream().map(Column::header).toList());
              }

              @Override
              public void row(List<Object> values) {
                sink.row(values);
              }
            });
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
              readonly,
              optionsOf(field),
              field != null && ValueConverter.isJson(field.getDataType())));
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
      throw new IllegalArgumentException(Msg.t("menu.action.selectRows"));
    }
    if (!needsRow) {
      rows.clear();
      rows.add(Map.of());
    }
    var binds = new ArrayList<Map<String, Object>>();
    var logged = new ArrayList<Map<String, String>>();
    // 변경 이력의 전후 비교: 고른 행(서명 확인한 목록 값 = 사용자가 보고 바꾼 값)에서 파라미터별 바뀌기 전 값과 행 전체를 남긴다
    var before = new ArrayList<Map<String, String>>();
    var beforeRows = new ArrayList<Map<String, String>>();
    for (var row : rows) {
      if (needsRow) {
        var previous = new LinkedHashMap<String, String>();
        names.forEach(name -> previous.put(name, row.get(labelOfParam(ctx, name))));
        before.add(previous);
        beforeRows.add(new LinkedHashMap<>(row));
      }
      var bind = new HashMap<String, Object>();
      var log = new LinkedHashMap<String, String>();
      for (var name : names) {
        var setting = settingOf(settings, name);
        var field = ctx.baseFields().get(name);
        String raw;
        if (setting.role() == Role.ROW_KEY) {
          raw = row.get(labelOfParam(ctx, name));
          if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException(Msg.t("menu.action.rowValueMissing", name));
          }
        } else if (isLocked(query, field)) {
          // 수정 불가 컬럼은 받은 값을 쓰지 않고 선택한 행의 원래 값을 그대로 넣는다(화면 조작으로 바꾸지 못하게)
          raw = row.get(labelOfParam(ctx, name));
          if (raw == null) {
            throw new IllegalArgumentException(
                Msg.t("menu.action.lockedMissing", headerOf(field, name)));
          }
        } else {
          raw = inputValue(setting, field, name, inputs);
        }
        bind.put(name, ValueConverter.toBindValue(raw, field));
        log.put(name, raw);
      }
      binds.add(bind);
      logged.add(log);
    }
    if (needsRow) {
      checkUnchanged(ctx, settings, names, rows);
    }
    var counts = ctx.adapter().modify(query.getQueryText(), binds);
    var affected = java.util.Arrays.stream(counts).sum();
    var changeLog = new ChangeLog();
    changeLog.setMenuId(ctx.menu().getId());
    changeLog.setMenuQueryId(query.getId());
    changeLog.setUsername(currentUsername());
    // rows = 넣은 값(파라미터별), before = 그 파라미터의 바뀌기 전 값, beforeRows = 고른 행 전체(목록 제목별). 입력은 고른 행이 없어 rows
    // 만
    changeLog.setBindValues(
        needsRow
            ? Map.of("rows", logged, "before", before, "beforeRows", beforeRows)
            : Map.of("rows", logged));
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

  /** 입력칸 값: 비었으면 기본값, 그래도 비었고 필수면 거부 */
  private static String inputValue(
      ParamSetting setting, DbField field, String name, Map<String, String> inputs) {
    var raw = inputs == null ? null : inputs.get(name);
    if ((raw == null || raw.isEmpty()) && setting.defaultValue() != null) {
      raw = setting.defaultValue();
    }
    if ((raw == null || raw.isEmpty()) && setting.required()) {
      throw new IllegalArgumentException(Msg.t("menu.action.required", headerOf(field, name)));
    }
    return raw;
  }

  /** 엑셀로 여러 행 넣기: 한 번에 넣는 최대 행 수 */
  // shortcut: 1000 행 고정, 더 큰 적재가 필요해지면 설정(Dynamiccrud2Properties)으로 뺄 것
  public static final int BULK_MAX_ROWS = 1000;

  /**
   * 엑셀로 여러 행 넣기 폼.
   *
   * @param names 파라미터 이름(쿼리 순서)
   * @param headers 엑셀 첫 줄 칸 이름(화면 제목 - 작업 폼과 같은 이름)
   */
  public record BulkForm(Menu menu, MenuQuery query, List<String> names, List<String> headers) {}

  /** 엑셀 행 검사에서 막힌 행들(행 번호 + 사유). 하나라도 있으면 아무것도 넣지 않는다 */
  public static class BulkInputException extends IllegalArgumentException {
    private final transient List<String> errors;

    public BulkInputException(List<String> errors) {
      super(String.join("\n", errors));
      this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
      return errors;
    }
  }

  /** 엑셀로 여러 행 넣기: 입력 작업이고 행을 고르지 않는(행 키 없는) 작업만 */
  public BulkForm bulkForm(UUID menuId, UUID queryId) {
    var ctx = context(menuId);
    var query = queryOf(ctx.menu(), queryId);
    authorizer.checkRun(ctx.menu(), query);
    var settings = QueryService.settingsOf(query);
    var names = paramNames(ctx, query);
    if (query.getKind() != QueryKind.INSERT
        || names.stream().anyMatch(n -> settingOf(settings, n).role() == Role.ROW_KEY)) {
      throw new IllegalArgumentException(Msg.t("menu.bulk.notInsert"));
    }
    return new BulkForm(
        ctx.menu(),
        query,
        names,
        names.stream().map(n -> headerOf(ctx.baseFields().get(n), n)).toList());
  }

  /**
   * 엑셀로 여러 행 넣기(사용자 결정 11-26). 행마다 칸 이름 → 값(칸 이름은 화면 제목이나 파라미터 이름). 먼저 모든 행을 검사해(필수 · 기본값 · 형식) 틀린
   * 행이 하나라도 있으면 아무것도 넣지 않고 행 번호와 사유를 모아 돌려준다. 다 맞으면 한 트랜잭션으로 넣고 이력 한 건.
   *
   * @param rows 엑셀 둘째 줄부터(빈 줄은 부른 쪽이 뺀다). 행 번호는 엑셀 줄 번호(첫 줄 = 칸 이름이라 2 부터)
   * @return 넣은 행 수
   * @throws BulkInputException 막힌 행들
   */
  public int runBulk(UUID menuId, UUID queryId, List<Map<String, String>> rows) {
    var form = bulkForm(menuId, queryId);
    var ctx = context(menuId);
    var query = form.query();
    if (rows.isEmpty()) {
      throw new IllegalArgumentException(Msg.t("menu.bulk.empty"));
    }
    if (rows.size() > BULK_MAX_ROWS) {
      throw new IllegalArgumentException(Msg.t("menu.bulk.tooMany", BULK_MAX_ROWS));
    }
    var settings = QueryService.settingsOf(query);
    var binds = new ArrayList<Map<String, Object>>();
    var logged = new ArrayList<Map<String, String>>();
    var errors = new ArrayList<String>();
    for (int i = 0; i < rows.size(); i++) {
      var row = rows.get(i);
      var inputs = new HashMap<String, String>();
      for (int p = 0; p < form.names().size(); p++) {
        var name = form.names().get(p);
        var value = row.containsKey(form.headers().get(p)) ? row.get(form.headers().get(p)) : row.get(name);
        inputs.put(name, value == null ? null : value.strip());
      }
      try {
        var bind = new HashMap<String, Object>();
        var log = new LinkedHashMap<String, String>();
        for (var name : form.names()) {
          var field = ctx.baseFields().get(name);
          var raw = inputValue(settingOf(settings, name), field, name, inputs);
          bind.put(name, ValueConverter.toBindValue(raw, field));
          log.put(name, raw);
        }
        binds.add(bind);
        logged.add(log);
      } catch (IllegalArgumentException e) {
        errors.add(Msg.t("menu.bulk.rowError", i + 2, e.getMessage()));
      }
    }
    if (!errors.isEmpty()) {
      throw new BulkInputException(errors);
    }
    var affected = java.util.Arrays.stream(ctx.adapter().modify(query.getQueryText(), binds)).sum();
    var changeLog = new ChangeLog();
    changeLog.setMenuId(menuId);
    changeLog.setMenuQueryId(query.getId());
    changeLog.setUsername(currentUsername());
    changeLog.setBindValues(Map.of("rows", logged, "bulk", true));
    changeLog.setAffectedRows(affected);
    try {
      changeLogRepository.save(changeLog);
    } catch (RuntimeException e) {
      // 대상 DB 는 이미 커밋됐다(run 과 같은 이유로 성공으로 둔다)
      LOG.error("[CHANGE_LOG_FAILED] menu={} query={} bulk={} affected={}", menuId, query.getId(), rows.size(), affected, e);
    }
    return affected;
  }

  /**
   * 데이터 변경 되돌리기(메뉴 관리 → 변경 이력 → 데이터 변경 한 줄, 사용자 결정 11-26). 실행한 사람이 아니라 지금 그 작업을 실행할 수 있는 사람이 되돌린다.
   *
   * <ul>
   *   <li>수정: 같은 수정 쿼리를 바뀌기 전 값(before)으로 다시 실행. 그 사이 누가 그 행을 바꿨으면(지금 값 ≠ 그때 넣은 값) 막는다.
   *   <li>삭제: 지운 행(beforeRows, 메뉴 테이블 컬럼만)을 그 테이블에 다시 넣는다. 같은 키 행이 이미 있으면 막는다.
   *   <li>입력: 되돌리지 않는다 - DB 가 만든 키(자동 증가 등)를 이력이 모를 수 있다.
   * </ul>
   *
   * 되돌린 것도 데이터 변경 이력에 남고(bindValues.revertOf = 되돌린 이력 id), 한 이력은 한 번만 되돌린다.
   *
   * @return 되돌린 행 수
   * @throws IllegalArgumentException 되돌릴 수 없을 때(메시지는 화면에 보인다)
   */
  public int revert(UUID menuId, UUID logId) {
    var ctx = context(menuId);
    var log =
        changeLogRepository
            .findById(logId)
            .filter(found -> found.getMenuId().equals(menuId))
            .orElseThrow(() -> new IllegalArgumentException(Msg.t("menu.revert.notFound")));
    var values = log.getBindValues() == null ? Map.<String, Object>of() : log.getBindValues();
    if (values.containsKey("revertOf")) {
      throw new IllegalArgumentException(Msg.t("menu.revert.notRevertible"));
    }
    if (changeLogRepository.findRevertedIds(menuId).contains(logId.toString())) {
      throw new IllegalArgumentException(Msg.t("menu.revert.already"));
    }
    var query =
        ctx.menu().getQueries().stream()
            .filter(q -> q.getId().equals(log.getMenuQueryId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(Msg.t("menu.revert.queryGone")));
    authorizer.checkRun(ctx.menu(), query);
    var table =
        new TableRef(
            ctx.base().getCatalogName(), ctx.base().getSchemaName(), ctx.base().getTableName());
    var after = textMaps(values.get("rows"));
    var before = textMaps(values.get("before"));
    var keyNames =
        QueryService.settingsOf(query).entrySet().stream()
            .filter(e -> e.getValue().role() == Role.ROW_KEY)
            .map(Map.Entry::getKey)
            .filter(ctx.baseFields()::containsKey)
            .toList();
    if (keyNames.isEmpty() || before.isEmpty()) {
      // 행 키를 모르거나 전후 값이 없는 옛 이력(2026-11-19 전)
      throw new IllegalArgumentException(Msg.t("menu.revert.notRevertible"));
    }
    var changeLog = new ChangeLog();
    changeLog.setMenuId(menuId);
    changeLog.setMenuQueryId(query.getId());
    changeLog.setUsername(currentUsername());
    int affected;
    switch (query.getKind()) {
      case UPDATE -> {
        var binds = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < before.size(); i++) {
          var previous = before.get(i);
          var done = i < after.size() ? after.get(i) : Map.<String, String>of();
          checkStillAsChanged(ctx, table, keyNames, previous, done);
          var bind = new HashMap<String, Object>();
          previous.forEach(
              (name, raw) -> bind.put(name, ValueConverter.toBindValue(raw, ctx.baseFields().get(name))));
          binds.add(bind);
        }
        affected = java.util.Arrays.stream(ctx.adapter().modify(query.getQueryText(), binds)).sum();
        // 되돌린 실행도 수정 이력과 같은 모양: rows = 넣은 값(= 그때의 전), before = 그때 넣은 값
        changeLog.setBindValues(Map.of("rows", before, "before", after, "revertOf", logId.toString()));
      }
      case DELETE -> {
        var labels = baseColumnLabels(ctx);
        var inserts = new ArrayList<Map<String, Object>>();
        var restored = new ArrayList<Map<String, String>>();
        for (int i = 0; i < before.size(); i++) {
          var keys = new LinkedHashMap<String, Object>();
          for (var key : keyNames) {
            keys.put(key, ValueConverter.toBindValue(before.get(i).get(key), ctx.baseFields().get(key)));
          }
          if (ctx.adapter().currentRow(table, List.copyOf(keyNames), keys) != null) {
            throw new IllegalArgumentException(Msg.t("menu.revert.rowExists"));
          }
        }
        for (var row : textMaps(values.get("beforeRows"))) {
          var insert = new LinkedHashMap<String, Object>();
          var shown = new LinkedHashMap<String, String>();
          labels.forEach(
              (column, label) -> {
                if (row.containsKey(label)) {
                  insert.put(column, ValueConverter.toBindValue(row.get(label), ctx.baseFields().get(column)));
                  shown.put(column, row.get(label));
                }
              });
          inserts.add(insert);
          restored.add(shown);
        }
        if (inserts.isEmpty()) {
          throw new IllegalArgumentException(Msg.t("menu.revert.notRevertible"));
        }
        affected = java.util.Arrays.stream(ctx.adapter().insertRows(table, inserts)).sum();
        // 되돌린 삭제는 다시 넣은 행(컬럼별)으로 - 이력 화면에서 입력처럼 보인다
        changeLog.setBindValues(Map.of("rows", restored, "revertOf", logId.toString()));
      }
      default -> throw new IllegalArgumentException(Msg.t("menu.revert.notRevertible"));
    }
    changeLog.setAffectedRows(affected);
    changeLogRepository.save(changeLog);
    return affected;
  }

  /**
   * 수정 되돌리기 전 확인: 그 행의 지금 값이 그때 넣은 값 그대로인가(메뉴 테이블 컬럼인 파라미터만). 다르면 그 사이 다른 변경이 있어 덮어쓰지 않는다. 값 비교는
   * 바인딩 값으로(숫자 1 = 1.0, 일시 초 생략 같은 모양 차이를 넘는다), JSON 은 내용으로.
   */
  private void checkStillAsChanged(
      Context ctx,
      TableRef table,
      List<String> keyNames,
      Map<String, String> previous,
      Map<String, String> done) {
    var keys = new LinkedHashMap<String, Object>();
    for (var key : keyNames) {
      keys.put(key, ValueConverter.toBindValue(previous.get(key), ctx.baseFields().get(key)));
    }
    var columns =
        done.keySet().stream()
            .filter(ctx.baseFields()::containsKey)
            .filter(name -> !keyNames.contains(name))
            .toList();
    if (columns.isEmpty()) {
      return;
    }
    var current = ctx.adapter().currentRow(table, columns, keys);
    if (current == null) {
      throw new IllegalArgumentException(Msg.t("menu.action.rowGone"));
    }
    for (int i = 0; i < columns.size(); i++) {
      var field = ctx.baseFields().get(columns.get(i));
      if (!sameValue(done.get(columns.get(i)), ValueConverter.toText(current.get(i)), field)) {
        throw new IllegalArgumentException(Msg.t("menu.revert.changedSince"));
      }
    }
  }

  private boolean sameValue(String a, String b, DbField field) {
    var left = a == null ? "" : a;
    var right = b == null ? "" : b;
    if (left.equals(right)) {
      return true;
    }
    if (left.isEmpty() || right.isEmpty()) {
      return false;
    }
    if (field != null && ValueConverter.isJson(field.getDataType())) {
      try {
        return objectMapper.readTree(left).equals(objectMapper.readTree(right));
      } catch (RuntimeException e) {
        return false;
      }
    }
    try {
      var x = ValueConverter.toBindValue(left, field);
      var y = ValueConverter.toBindValue(right, field);
      if (x instanceof java.math.BigDecimal m && y instanceof java.math.BigDecimal n) {
        return m.compareTo(n) == 0;
      }
      return java.util.Objects.equals(x, y);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** 이력 JSON 배열 → [이름: 글자 값] 목록(null 은 null 그대로 - 빈 글자와 구분해 다시 넣는다) */
  private static List<Map<String, String>> textMaps(Object value) {
    var result = new ArrayList<Map<String, String>>();
    if (value instanceof List<?> list) {
      for (var item : list) {
        if (item instanceof Map<?, ?> map) {
          var row = new LinkedHashMap<String, String>();
          map.forEach((k, v) -> row.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
          result.add(row);
        }
      }
    }
    return result;
  }

  // ---- 내부 ----

  /**
   * 동시 수정 확인: 고른 행(목록을 열 때 값)이 지금 DB 값과 같은지 본다. 다르면 그 사이 누가 바꾼 것이라 덮어쓰지 않고 목록을 다시 열게 한다. 비교는 목록에
   * 그대로 실린 메뉴 테이블 컬럼만(계산식 · 다른 테이블 컬럼은 값 모양이 달라 뺀다), 행 키가 메뉴 테이블 컬럼이 아니면 확인하지 않는다.
   */
  // shortcut: 확인과 실행이 한 트랜잭션이 아니라 그 사이 몇 ms 의 변경은 못 잡는다, 필요하면 modify 안에서 SELECT ... FOR UPDATE 로 옮길
  // 것
  private void checkUnchanged(
      Context ctx,
      Map<String, ParamSetting> settings,
      List<String> names,
      List<Map<String, String>> rows) {
    var keyColumns =
        names.stream().filter(n -> settingOf(settings, n).role() == Role.ROW_KEY).toList();
    var labels = baseColumnLabels(ctx);
    if (labels.isEmpty() || !ctx.baseFields().keySet().containsAll(keyColumns)) {
      return;
    }
    var columns = List.copyOf(labels.keySet());
    var table =
        new TableRef(
            ctx.base().getCatalogName(), ctx.base().getSchemaName(), ctx.base().getTableName());
    for (var row : rows) {
      var keys = new LinkedHashMap<String, Object>();
      for (var key : keyColumns) {
        keys.put(
            key,
            ValueConverter.toBindValue(row.get(labelOfParam(ctx, key)), ctx.baseFields().get(key)));
      }
      var current = ctx.adapter().currentRow(table, columns, keys);
      if (current == null) {
        throw new IllegalArgumentException(Msg.t("menu.action.rowGone"));
      }
      for (int i = 0; i < columns.size(); i++) {
        var label = labels.get(columns.get(i));
        if (row.containsKey(label)
            && !java.util.Objects.equals(row.get(label), ValueConverter.toText(current.get(i)))) {
          throw new IllegalArgumentException(Msg.t("menu.action.rowChanged"));
        }
      }
    }
  }

  /** 목록에 그대로 실린 메뉴 테이블 컬럼 → 결과 이름. * 는 메뉴 테이블 * 일 때만 컬럼 이름 그대로 */
  private static Map<String, String> baseColumnLabels(Context ctx) {
    var columns = ctx.analysis().columns();
    var baseTable = ctx.base().getTableName();
    var star =
        columns.stream()
            .anyMatch(c ->
                c.kind() == ResultColumn.Kind.ALL
                    || (c.kind() == ResultColumn.Kind.ALL_TABLE
                        && baseTable.equalsIgnoreCase(c.tableName())));
    var labels = new HashSet<String>();
    columns.forEach(c -> labels.add(c.label()));
    var result = new LinkedHashMap<String, String>();
    for (var field : ctx.base().getFields()) {
      if (field.isVirtual()) {
        continue;
      }
      var name = field.getColumnName();
      var explicit =
          columns.stream()
              .filter(c -> c.kind() == ResultColumn.Kind.COLUMN)
              .filter(c -> c.tableName() == null || c.tableName().equalsIgnoreCase(baseTable))
              .filter(c -> name.equals(c.columnName()))
              .map(ResultColumn::label)
              .findFirst();
      if (explicit.isPresent()) {
        result.put(name, explicit.get());
      } else if (star && !labels.contains(name)) {
        result.put(name, name);
      }
    }
    return result;
  }

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
      FieldService.Lookup fields) {}

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
        fieldService.lookup(base));
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
              raw == null ? "" : raw,
              optionsOf(field)));
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
      message = Msg.t("menu.page.requiredSearch", String.join(", ", missing));
    }
    return new Bound(params, empty, fields, message);
  }

  /** 검색 결과 컬럼이 가리키는 테이블 컬럼의 서로 다른 값(글자로). 읽지 못하면 빈 목록 - 셀렉트 대신 입력칸이 된다 */
  private List<String> distinctValues(Context ctx, ResultColumn column) {
    var base = ctx.base();
    var table =
        new TableRef(
            base.getCatalogName(),
            base.schemaOf(column.tableSchema()),
            column.tableName() == null ? base.getTableName() : column.tableName());
    var key =
        base.getConnectionName()
            + "|"
            + table.catalog()
            + "|"
            + table.schema()
            + "|"
            + table.name()
            + "|"
            + column.columnName();
    var cached = distinctCache.get(key);
    var now = System.currentTimeMillis();
    if (cached != null && now - cached.readAt() < DISTINCT_TTL_MILLIS) {
      return cached.values();
    }
    List<String> values;
    try {
      values =
          ctx.adapter().distinctValues(table, column.columnName(), DISTINCT_LIMIT).stream()
              .map(ValueConverter::toText)
              .toList();
    } catch (DataAccessException e) {
      return List.of();
    }
    distinctCache.put(key, new DistinctValues(now, values));
    return values;
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
    var missing = new ArrayList<String>();
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
      var pick =
          SearchSettings.Pick.resolve(
              item.pick(), field != null && !field.valueOptionMap().isEmpty());
      var tooMany = false;
      Map<String, String> options =
          switch (pick) {
            case INPUT -> Map.of();
            case LIST -> optionsOf(field);
            case CUSTOM -> item.customOptions();
            case DISTINCT -> {
              var values = distinctValues(ctx, column);
              if (values.size() > DISTINCT_LIMIT) {
                tooMany = true;
                yield Map.of();
              }
              // 값 목록이 있으면 표시 글자로, 없으면 값 그대로
              var labels = optionsOf(field);
              var map = new java.util.LinkedHashMap<String, String>();
              values.forEach(v -> map.put(v, labels.getOrDefault(v, v)));
              yield map;
            }
          };
      if (!options.isEmpty() && operator != SearchOperator.RANGE) {
        // 값 목록에서 고른 저장값은 일부가 아닌 그 값 자체로 찾는다(1 을 고르면 10 · 21 이 나오지 않게)
        operator = SearchOperator.EQUALS;
      }
      var value = blankToNull(search.filters().get(item.label()));
      var valueTo =
          operator == SearchOperator.RANGE
              ? blankToNull(search.filters().get(item.label() + ".to"))
              : null;
      var header = FieldService.headerOf(field, column.alias(), item.label());
      if (item.required() && value == null && valueTo == null) {
        missing.add(header);
      }
      fields.add(
          new FilterField(
              item.label(),
              header,
              inputType,
              operator,
              value,
              valueTo,
              options,
              tooMany,
              item.required()));
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
      keywordHeaders.add(FieldService.headerOf(field, column.alias(), label));
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
    // 필수 검색칸이 비면 조회하지 않고 안내만(사용 화면 · 엑셀 · JSON 모두 - message 가 있으면 읽지 않는다)
    if (message == null && !missing.isEmpty()) {
      message = Msg.t("menu.page.requiredSearch", String.join(", ", missing));
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
      var header = FieldService.headerOf(field, column == null ? null : column.alias(), label);
      var sortable =
          ctx.analysis().parsed()
              && field != null
              && field.isSortable()
              && !field.isVirtual()
              && (column == null || column.kind() == ResultColumn.Kind.COLUMN);
      columns.add(
          new Column(
              label,
              header,
              sortable,
              field == null || field.getInputType() == null ? InputType.TEXT : field.getInputType(),
              optionsOf(field),
              field == null ? null : field.getDateFormat(),
              field != null && ValueConverter.isJson(field.getDataType())));
    }
    return columns;
  }

  /** 결과 컬럼의 필드: 메뉴 테이블이면 그 필드, 조인한 다른 테이블이면 그 테이블 필드, 계산 결과면 가상 필드 */
  private static DbField fieldOf(Context ctx, ResultColumn column) {
    return ctx.fields().fieldOf(column);
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
    return FieldService.headerOf(field, null, fallback);
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
      throw new IllegalArgumentException(Msg.t("menu.action.rowUnreadable"), e);
    }
  }

  private static MenuQuery queryOf(Menu menu, UUID queryId) {
    var query =
        menu.getQueries().stream()
            .filter(q -> q.getId().equals(queryId))
            .findFirst()
            .orElseThrow(MenuPageService::notFound);
    if (query.getKind() == QueryKind.READ) {
      throw new BlueskyException(
          QueryErrorCode.STATEMENT_KIND_MISMATCH, Msg.t("query.kind.modify"), QueryKind.READ);
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
