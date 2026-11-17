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
import net.luversof.web.dynamiccrud2.connection.ConnectionNameService;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

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

  public AdminController(
      MenuService menuService,
      QueryService queryService,
      FieldService fieldService,
      ConnectionNameService connectionNameService,
      DatabaseAdapterResolver resolver) {
    this.menuService = menuService;
    this.queryService = queryService;
    this.fieldService = fieldService;
    this.connectionNameService = connectionNameService;
    this.resolver = resolver;
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

  @PostMapping("/mainMenu/{id}/delete")
  public String deleteMainMenu(@PathVariable UUID id, RedirectAttributes redirect) {
    var projectId = menuService.getMainMenu(id).getProjectId();
    return run(redirect, redirectToProject(projectId), () -> menuService.deleteMainMenu(id));
  }

  // ---- 메뉴 등록 ----

  @GetMapping("/mainMenu/{mainMenuId}/menu/new")
  public String newMenu(@PathVariable UUID mainMenuId, Model model) {
    model.addAttribute("mainMenuId", mainMenuId);
    model.addAttribute("projectId", menuService.getMainMenu(mainMenuId).getProjectId());
    return "admin/menuNew";
  }

  /** connection 자동완성 후보(htmx, datalist 의 option) */
  @GetMapping("/connection/search")
  public String connectionOptions(@RequestParam(defaultValue = "") String keyword, Model model) {
    model.addAttribute("names", connectionNameService.search(keyword));
    return "admin/connectionOptions";
  }

  /** connection 을 고르면 테이블 select 를 채운다(htmx). 값은 catalog|schema|name. 없는 이름이면 안내 option 하나 */
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
    var parts = table.split("\\|", -1);
    var ref = new TableRef(emptyToNull(parts[0]), emptyToNull(parts[1]), parts[2]);
    try {
      var menu = menuService.createMenu(mainMenuId, code, name, connectionName, ref);
      return "redirect:/admin/menu/" + menu.getId();
    } catch (BlueskyException e) {
      redirect.addFlashAttribute("error", messageOf(e));
      return "redirect:/admin/mainMenu/" + mainMenuId + "/menu/new";
    }
  }

  // ---- 메뉴 ----

  @GetMapping("/menu/{id}")
  public String menu(@PathVariable UUID id, Model model) {
    var menu = menuService.getMenu(id);
    model.addAttribute("menu", menu);
    model.addAttribute("projectId", menuService.getMainMenu(menu.getMainMenuId()).getProjectId());
    model.addAttribute("dbTable", fieldService.get(menu.getDbTableId()));
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

  @PostMapping("/menu/{id}")
  public String updateMenu(
      @PathVariable UUID id,
      @RequestParam String code,
      @RequestParam String name,
      @RequestParam(required = false) String authority,
      @RequestParam(defaultValue = "20") int pageSize,
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
    model.addAttribute("menuId", menu);
    model.addAttribute(
        "fields",
        dbTable.getFields().stream()
            .sorted(
                java.util.Comparator.comparing(
                        net.luversof.web.dynamiccrud2.domain.DbField::isVirtual)
                    .thenComparing(net.luversof.web.dynamiccrud2.domain.DbField::getDisplayOrder))
            .toList());
    model.addAttribute("inputTypes", InputType.values());
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
      @RequestParam(required = false) UUID menu) {
    fieldService.updateField(id, columnName, label, inputType, required, maxLength, sortable);
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
    model.addAttribute("name", name == null ? "" : name);
    model.addAttribute("queryText", queryText == null ? "" : queryText);
    model.addAttribute("authority", authority == null ? "" : authority);
    model.addAttribute("error", error);
    model.addAttribute(
        "inspection",
        queryText == null || queryText.isBlank()
            ? new QueryService.QueryInspection(null, List.of(), List.of(), List.of())
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
