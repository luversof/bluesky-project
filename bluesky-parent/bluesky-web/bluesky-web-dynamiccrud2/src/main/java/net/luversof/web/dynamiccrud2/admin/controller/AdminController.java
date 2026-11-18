package net.luversof.web.dynamiccrud2.admin.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.admin.service.SearchService;
import net.luversof.web.dynamiccrud2.connection.ConnectionNameService;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;

/** 설정 화면. 경로 전체가 ROLE_ADMIN 전용(AdminAccessInterceptor). */
@Controller
@RequestMapping("/admin")
public class AdminController {

  private static final String REDIRECT_INDEX = "redirect:/admin";

  private final MenuService menuService;
  private final QueryService queryService;
  private final FieldService fieldService;
  private final ConnectionNameService connectionNameService;
  private final DatabaseAdapterResolver resolver;
  private final SearchService searchService;

  public AdminController(
      MenuService menuService,
      QueryService queryService,
      FieldService fieldService,
      ConnectionNameService connectionNameService,
      DatabaseAdapterResolver resolver,
      SearchService searchService) {
    this.menuService = menuService;
    this.queryService = queryService;
    this.fieldService = fieldService;
    this.connectionNameService = connectionNameService;
    this.resolver = resolver;
    this.searchService = searchService;
  }

  // ---- Project · MainMenu ----

  /** 왼쪽 프로젝트 목록 + 오른쪽 고른 프로젝트 상세. 고르지 않았거나 없는 id 면 첫 프로젝트 */
  @GetMapping
  public String index(@RequestParam(required = false) UUID project, Model model) {
    var tree = menuService.tree();
    model.addAttribute("tree", tree);
    model.addAttribute(
        "selected",
        tree.stream()
            .filter(node -> node.project().getId().equals(project))
            .findFirst()
            .orElse(tree.isEmpty() ? null : tree.getFirst()));
    return "admin/index";
  }

  @PostMapping("/project")
  public String saveProject(
      @RequestParam(required = false) UUID id,
      @RequestParam String code,
      @RequestParam String name,
      RedirectAttributes redirect) {
    try {
      return redirectToProject(menuService.saveProject(id, code, name).getId());
    } catch (BlueskyException e) {
      redirect.addFlashAttribute("error", messageOf(e));
      return id == null ? REDIRECT_INDEX : redirectToProject(id);
    }
  }

  /** 드래그 앤 드롭 · 키보드로 바꾼 순서(fetch). 응답 본문 없음 */
  @PostMapping("/project/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderProjects(@RequestParam List<UUID> ids) {
    menuService.reorderProjects(ids);
  }

  @PostMapping("/project/{projectId}/mainMenu/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderMainMenus(@PathVariable UUID projectId, @RequestParam List<UUID> ids) {
    menuService.reorderMainMenus(projectId, ids);
  }

  @PostMapping("/mainMenu/{mainMenuId}/menu/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderMenus(@PathVariable UUID mainMenuId, @RequestParam List<UUID> ids) {
    menuService.reorderMenus(mainMenuId, ids);
  }

  @PostMapping("/project/{id}/delete")
  public String deleteProject(@PathVariable UUID id, RedirectAttributes redirect) {
    return run(redirect, REDIRECT_INDEX, () -> menuService.deleteProject(id));
  }

  @PostMapping("/project/{projectId}/mainMenu")
  public String saveMainMenu(
      @PathVariable UUID projectId,
      @RequestParam(required = false) UUID id,
      @RequestParam String code,
      @RequestParam String name,
      RedirectAttributes redirect) {
    return run(
        redirect,
        redirectToProject(projectId),
        () -> menuService.saveMainMenu(id, projectId, code, name));
  }

  @PostMapping("/project/{id}/display")
  public String projectDisplay(@PathVariable UUID id, @RequestParam boolean display) {
    menuService.setProjectDisplay(id, display);
    return redirectToProject(id);
  }

  @PostMapping("/mainMenu/{id}/display")
  public String mainMenuDisplay(@PathVariable UUID id, @RequestParam boolean display) {
    menuService.setMainMenuDisplay(id, display);
    return redirectToProject(menuService.getMainMenu(id).getProjectId());
  }

  @PostMapping("/mainMenu/{id}/delete")
  public String deleteMainMenu(@PathVariable UUID id, RedirectAttributes redirect) {
    var projectId = menuService.getMainMenu(id).getProjectId();
    return run(redirect, redirectToProject(projectId), () -> menuService.deleteMainMenu(id));
  }

  // ---- 메뉴 등록 ----

  /** connection 검색 후보(htmx, combobox 목록의 li) */
  @GetMapping("/connection/search")
  public String connectionOptions(@RequestParam(defaultValue = "") String keyword, Model model) {
    model.addAttribute("names", connectionNameService.search(keyword));
    return "admin/connectionOptions";
  }

  /** connection 을 고르면 테이블 후보를 채운다(htmx, combobox 목록의 li). 값은 catalog|schema|name. 없는 이름이면 안내 한 줄 */
  @GetMapping("/connection/tables")
  public String tableOptions(@RequestParam(defaultValue = "") String connectionName, Model model) {
    String error = null;
    var tables = java.util.List.<TableRef>of();
    if (connectionName.isBlank()) {
      error = "connection 을 먼저 고르세요";
    } else {
      try {
        tables = resolver.resolve(connectionName.strip()).listTables();
      } catch (BlueskyException e) {
        error = messageOf(e);
      }
    }
    model.addAttribute("tables", tables);
    model.addAttribute("error", error);
    return "admin/tableOptions";
  }

  @PostMapping("/mainMenu/{mainMenuId}/menu")
  public String createMenu(
      @PathVariable UUID mainMenuId,
      @RequestParam String code,
      @RequestParam String name,
      @RequestParam String connectionName,
      @RequestParam String table,
      RedirectAttributes redirect) {
    // 관리 화면 메인 메뉴 아래 폼에서 온다. 성공 · 실패 모두 그 프로젝트 관리 화면으로(실패면 사유)
    var projectPage = redirectToProject(menuService.getMainMenu(mainMenuId).getProjectId());
    var parts = table.split("\\|", -1);
    if (parts.length != 3 || parts[2].isBlank()) {
      redirect.addFlashAttribute("error", "테이블을 목록에서 고르세요.");
      return projectPage;
    }
    var ref = new TableRef(emptyToNull(parts[0]), emptyToNull(parts[1]), parts[2]);
    try {
      // 만들면 관리 화면에 머문다(만든 메뉴가 목록에 보인다). 검색 · 작업 설정은 메뉴 이름을 눌러 메뉴 설정 화면에서
      menuService.createMenu(mainMenuId, code, name, connectionName, ref);
      return projectPage;
    } catch (BlueskyException e) {
      redirect.addFlashAttribute("error", messageOf(e));
      return projectPage;
    }
  }

  // ---- 메뉴 ----

  @GetMapping("/menu/{id}")
  public String menu(@PathVariable UUID id, Model model) {
    var menu = menuService.getMenu(id);
    model.addAttribute("menu", menu);
    model.addAttribute("projectId", menuService.getMainMenu(menu.getMainMenuId()).getProjectId());
    var dbTable = fieldService.get(menu.getDbTableId());
    model.addAttribute("dbTable", dbTable);
    addSource(model, dbTable);
    var crumbs = pathTo(menu, false);
    model.addAttribute("breadcrumb", crumbs);
    model.addAttribute("search", searchService.setup(menu));
    model.addAttribute(
        "queries",
        menu.getQueries().stream()
            .sorted(
                java.util.Comparator.comparing(MenuQuery::getKind)
                    .thenComparing(MenuQuery::getDisplayOrder)
                    .thenComparing(MenuQuery::getName))
            .toList());
    return "admin/menu";
  }

  /** 데이터 출처 줄의 DB 종류와 대상 이름(테이블 · 컬렉션). connection 을 못 열면(설정에서 빠짐 등) 화면은 그대로 보이게 사유만, 대상 이름은 테이블 */
  private void addSource(Model model, net.luversof.web.dynamiccrud2.domain.DbTable dbTable) {
    String databaseType;
    String targetNoun;
    try {
      var adapter = resolver.resolve(dbTable.getConnectionName());
      databaseType = adapter.databaseType();
      targetNoun = adapter.targetNoun();
    } catch (RuntimeException e) {
      databaseType = "알 수 없음(연결 실패)";
      targetNoun = "테이블";
    }
    model.addAttribute("databaseType", databaseType);
    model.addAttribute("targetNoun", targetNoun);
  }

  /**
   * 위치 표시는 화면 단계(프로젝트 관리 › 메뉴 관리 › 테이블 필드 관리 · 쿼리 추가)로 보인다(사용자 결정 - 프로젝트 · 메인 메뉴 이름 단계는 화면이 아니라
   * 헷갈렸다). 프로젝트 관리 링크는 그 메뉴의 프로젝트를 고른 상태로, withMenuLink 면 메뉴 관리도 링크
   */
  private List<Crumb> pathTo(Menu menu, boolean withMenuLink) {
    var projectId = menuService.getMainMenu(menu.getMainMenuId()).getProjectId();
    var crumbs = new java.util.ArrayList<Crumb>();
    crumbs.add(new Crumb("프로젝트 관리", "/admin?project=" + projectId));
    crumbs.add(new Crumb("메뉴 관리", withMenuLink ? "/admin/menu/" + menu.getId() : null));
    return crumbs;
  }

  @PostMapping("/menu/{id}")
  public String updateMenu(
      @PathVariable UUID id,
      @RequestParam String code,
      @RequestParam String name,
      @RequestParam(required = false) String authority,
      @RequestParam(defaultValue = "15") int pageSize,
      @RequestParam(defaultValue = "false") boolean enableExcel,
      @RequestParam(defaultValue = "false") boolean enableDisplay,
      RedirectAttributes redirect) {
    return run(
        redirect,
        "redirect:/admin/menu/" + id,
        () ->
            menuService.updateMenu(
                id, code, name, authority, pageSize, enableExcel, enableDisplay));
  }

  /** 검색 설정: on = 검색칸으로 고른 결과 이름들, op.결과이름 = 방식, kw = 통합 검색에 넣은 결과 이름들 */
  @PostMapping("/menu/{id}/search")
  public String saveSearch(
      @PathVariable UUID id, @RequestParam MultiValueMap<String, String> form) {
    var operators = new LinkedHashMap<String, SearchOperator>();
    var on = form.get("on") == null ? List.<String>of() : form.get("on");
    for (var label : on) {
      // 모르는 방식이면 null - 서비스가 그 컬럼의 기본 방식으로 바꾼다
      var operator = form.getFirst("op." + label);
      operators.put(
          label,
          java.util.Arrays.stream(SearchOperator.values())
              .filter(o -> o.name().equals(operator))
              .findFirst()
              .orElse(null));
    }
    searchService.save(id, operators, form.get("kw") == null ? List.of() : form.get("kw"));
    return "redirect:/admin/menu/" + id;
  }

  /** 관리 화면 메뉴 줄의 그 자리 수정(이름 · 주소 이름) - 끝나면 그 프로젝트로 돌아온다 */
  @PostMapping("/menu/{id}/rename")
  public String renameMenu(
      @PathVariable UUID id,
      @RequestParam String code,
      @RequestParam String name,
      RedirectAttributes redirect) {
    var projectId = menuService.getMainMenu(menuService.getMenu(id).getMainMenuId()).getProjectId();
    return run(
        redirect, redirectToProject(projectId), () -> menuService.renameMenu(id, code, name));
  }

  @PostMapping("/menu/{id}/delete")
  public String deleteMenu(@PathVariable UUID id) {
    var projectId = menuService.getMainMenu(menuService.getMenu(id).getMainMenuId()).getProjectId();
    menuService.deleteMenu(id);
    return redirectToProject(projectId);
  }

  // ---- 작업 쿼리 ----

  @GetMapping("/menu/{menuId}/query/new")
  public String newQuery(@PathVariable UUID menuId, @RequestParam QueryKind kind, Model model) {
    var menu = menuService.getMenu(menuId);
    var draft = queryService.draft(menu, kind);
    return editor(model, menu, null, kind, null, draft.queryText(), null, null, draft.error());
  }

  @GetMapping("/menu/{menuId}/query/{queryId}")
  public String editQuery(@PathVariable UUID menuId, @PathVariable UUID queryId, Model model) {
    var menu = menuService.getMenu(menuId);
    var query =
        menu.getQueries().stream().filter(q -> q.getId().equals(queryId)).findFirst().orElseThrow();
    return editor(
        model,
        menu,
        queryId,
        query.getKind(),
        query.getName(),
        query.getQueryText(),
        query.getAuthority(),
        QueryService.settingsOf(query),
        null);
  }

  /** 쿼리 아래 표(파라미터 · 결과 컬럼 · 경고)를 다시 그린다(htmx) */
  @PostMapping("/menu/{menuId}/query/inspect")
  public String inspect(
      @PathVariable UUID menuId,
      @RequestParam QueryKind kind,
      @RequestParam String queryText,
      @RequestParam MultiValueMap<String, String> form,
      Model model) {
    var menu = menuService.getMenu(menuId);
    model.addAttribute("kind", kind);
    model.addAttribute("inspection", queryService.inspect(menu, kind, queryText, params(form)));
    return "admin/queryInspection";
  }

  @PostMapping("/menu/{menuId}/query")
  public String saveQuery(
      @PathVariable UUID menuId,
      @RequestParam(required = false) UUID queryId,
      @RequestParam QueryKind kind,
      @RequestParam(required = false) String name,
      @RequestParam String queryText,
      @RequestParam(required = false) String authority,
      @RequestParam MultiValueMap<String, String> form,
      Model model) {
    var settings = params(form);
    try {
      queryService.save(menuId, queryId, kind, name, queryText, settings, authority);
    } catch (BlueskyException e) {
      var menu = menuService.getMenu(menuId);
      return editor(model, menu, queryId, kind, name, queryText, authority, settings, messageOf(e));
    }
    return "redirect:/admin/menu/" + menuId;
  }

  @PostMapping("/menu/{menuId}/query/{queryId}/delete")
  public String deleteQuery(
      @PathVariable UUID menuId, @PathVariable UUID queryId, RedirectAttributes redirect) {
    return run(
        redirect, "redirect:/admin/menu/" + menuId, () -> queryService.delete(menuId, queryId));
  }

  // ---- 필드 ----

  @GetMapping("/table/{id}")
  public String table(
      @PathVariable UUID id, @RequestParam(required = false) UUID menu, Model model) {
    var dbTable = fieldService.get(id);
    model.addAttribute("dbTable", dbTable);
    addSource(model, dbTable);
    model.addAttribute("menuId", menu);
    // 메뉴에서 왔으면 그 메뉴 아래로, 아니면 프로젝트 관리 바로 아래로(테이블은 여러 메뉴가 공유한다)
    var crumbs = new java.util.ArrayList<Crumb>();
    if (menu != null) {
      var owner = menuService.getMenu(menu);
      crumbs.addAll(pathTo(owner, true));
    } else {
      crumbs.add(new Crumb("프로젝트 관리", "/admin"));
    }
    crumbs.add(new Crumb(model.getAttribute("targetNoun") + " 필드 관리", null));
    model.addAttribute("breadcrumb", crumbs);
    model.addAttribute(
        "fields",
        dbTable.getFields().stream()
            .sorted(
                java.util.Comparator.comparing(
                        net.luversof.web.dynamiccrud2.domain.DbField::isVirtual)
                    .thenComparing(net.luversof.web.dynamiccrud2.domain.DbField::getDisplayOrder))
            .toList());
    model.addAttribute("inputTypes", InputType.values());
    // 인덱스 표와 정렬 안내: 정렬은 어떤 인덱스의 첫 컬럼일 때 인덱스를 쓴다(못 읽으면 표에 사유, 안내는 생략)
    var title =
        (dbTable.getSchemaName() == null ? "" : dbTable.getSchemaName() + ".")
            + dbTable.getTableName();
    SearchService.TableIndexes indexes;
    java.util.Set<String> leading;
    try {
      var meta = fieldService.readMeta(dbTable);
      indexes = new SearchService.TableIndexes(title, meta.indexes(), null);
      leading = meta.indexLeadingColumns();
    } catch (RuntimeException e) {
      indexes = new SearchService.TableIndexes(title, List.of(), "인덱스 정보를 읽지 못했습니다");
      leading = null;
    }
    model.addAttribute("indexes", indexes);
    model.addAttribute("indexLeading", leading);
    return "admin/table";
  }

  @PostMapping("/table/{id}/field")
  public String updateField(
      @PathVariable UUID id,
      @RequestParam String columnName,
      @RequestParam(required = false) String label,
      @RequestParam InputType inputType,
      @RequestParam(defaultValue = "false") boolean required,
      @RequestParam(required = false) Integer maxLength,
      @RequestParam(defaultValue = "false") boolean sortable,
      @RequestParam(defaultValue = "false") boolean updatable,
      @RequestParam(required = false) UUID menu) {
    fieldService.updateField(
        id, columnName, label, inputType, required, maxLength, sortable, updatable);
    return redirectToTable(id, menu);
  }

  @PostMapping("/table/{id}/resync")
  public String resync(@PathVariable UUID id, @RequestParam(required = false) UUID menu) {
    fieldService.resync(id);
    return redirectToTable(id, menu);
  }

  /** 필드 화면은 여러 메뉴가 공유한다 - 들어온 메뉴(menu)를 들고 다녀 "← 메뉴" 로 돌아가게 한다 */
  private static String redirectToTable(UUID id, UUID menu) {
    return "redirect:/admin/table/" + id + (menu == null ? "" : "?menu=" + menu);
  }

  // ---- 공통 ----

  private String editor(
      Model model,
      Menu menu,
      UUID queryId,
      QueryKind kind,
      String name,
      String queryText,
      String authority,
      Map<String, ParamSetting> settings,
      String error) {
    model.addAttribute("menu", menu);
    model.addAttribute("queryId", queryId);
    model.addAttribute("kind", kind);
    var crumbs = pathTo(menu, true);
    crumbs.add(new Crumb((queryId == null ? "쿼리 추가: " : "쿼리 편집: ") + kind.name(), null));
    model.addAttribute("breadcrumb", crumbs);
    model.addAttribute("name", name == null ? "" : name);
    model.addAttribute("queryText", queryText == null ? "" : queryText);
    model.addAttribute("authority", authority == null ? "" : authority);
    model.addAttribute("error", error);
    model.addAttribute(
        "inspection",
        queryText == null || queryText.isBlank()
            ? QueryService.QueryInspection.EMPTY
            : queryService.inspect(menu, kind, queryText, settings));
    return "admin/query";
  }

  /** 폼의 p.{이름}.role · p.{이름}.required · p.{이름}.defaultValue 를 모은다. required 가 없으면 선택. */
  static Map<String, ParamSetting> params(MultiValueMap<String, String> form) {
    var result = new LinkedHashMap<String, ParamSetting>();
    form.forEach((key, values) -> {
      if (key.startsWith("p.") && key.endsWith(".role")) {
        var name = key.substring(2, key.length() - ".role".length());
        result.put(
            name,
            new ParamSetting(
                Role.valueOf(values.get(0)),
                form.containsKey("p." + name + ".required"),
                emptyToNull(form.getFirst("p." + name + ".defaultValue"))));
      }
    });
    return result;
  }

  private static String redirectToProject(UUID projectId) {
    return REDIRECT_INDEX + "?project=" + projectId;
  }

  private static String run(RedirectAttributes redirect, String view, Runnable action) {
    try {
      action.run();
    } catch (BlueskyException e) {
      redirect.addFlashAttribute("error", messageOf(e));
    }
    return view;
  }

  private static String messageOf(BlueskyException e) {
    var message = QueryErrorCode.messageOf(e);
    return message == null ? e.getErrorCode() : message;
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
