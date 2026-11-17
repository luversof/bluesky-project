package net.luversof.web.dynamiccrud2.setting.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.connection.ConnectionNameService;
import net.luversof.web.dynamiccrud2.setting.domain.InputType;
import net.luversof.web.dynamiccrud2.setting.domain.Menu;
import net.luversof.web.dynamiccrud2.setting.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.setting.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.setting.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.setting.domain.QueryKind;
import net.luversof.web.dynamiccrud2.setting.service.FieldService;
import net.luversof.web.dynamiccrud2.setting.service.MenuService;
import net.luversof.web.dynamiccrud2.setting.service.QueryService;

/** 설정 화면. 경로 전체가 ROLE_ADMIN 전용(SettingAccessInterceptor). */
@Controller
@RequestMapping("/setting")
public class SettingController {

  private static final String REDIRECT_INDEX = "redirect:/setting";

  private final MenuService menuService;
  private final QueryService queryService;
  private final FieldService fieldService;
  private final ConnectionNameService connectionNameService;
  private final DatabaseAdapterResolver resolver;

  public SettingController(
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

  @GetMapping
  public String index(Model model) {
    model.addAttribute("tree", menuService.tree());
    return "setting/index";
  }

  @PostMapping("/project")
  public String saveProject(
      @RequestParam(required = false) UUID id,
      @RequestParam String name,
      @RequestParam(defaultValue = "0") int displayOrder) {
    menuService.saveProject(id, name, displayOrder);
    return REDIRECT_INDEX;
  }

  @PostMapping("/project/{id}/delete")
  public String deleteProject(@PathVariable UUID id, RedirectAttributes redirect) {
    return run(redirect, REDIRECT_INDEX, () -> menuService.deleteProject(id));
  }

  @PostMapping("/project/{projectId}/mainMenu")
  public String saveMainMenu(
      @PathVariable UUID projectId,
      @RequestParam(required = false) UUID id,
      @RequestParam String name,
      @RequestParam(defaultValue = "0") int displayOrder) {
    menuService.saveMainMenu(id, projectId, name, displayOrder);
    return REDIRECT_INDEX;
  }

  @PostMapping("/mainMenu/{id}/delete")
  public String deleteMainMenu(@PathVariable UUID id, RedirectAttributes redirect) {
    return run(redirect, REDIRECT_INDEX, () -> menuService.deleteMainMenu(id));
  }

  // ---- 메뉴 등록 ----

  @GetMapping("/mainMenu/{mainMenuId}/menu/new")
  public String newMenu(@PathVariable UUID mainMenuId, Model model) {
    model.addAttribute("mainMenuId", mainMenuId);
    model.addAttribute("connectionNames", connectionNameService.connectionNames());
    return "setting/menuNew";
  }

  /** connection 을 고르면 테이블 select 를 채운다(htmx). 값은 catalog|schema|name */
  @GetMapping("/connection/tables")
  public String tableOptions(@RequestParam String connectionName, Model model) {
    model.addAttribute("tables", resolver.resolve(connectionName).listTables());
    return "setting/tableOptions";
  }

  @PostMapping("/mainMenu/{mainMenuId}/menu")
  public String createMenu(
      @PathVariable UUID mainMenuId,
      @RequestParam String name,
      @RequestParam String connectionName,
      @RequestParam String table) {
    var parts = table.split("\\|", -1);
    var ref = new TableRef(emptyToNull(parts[0]), emptyToNull(parts[1]), parts[2]);
    var menu = menuService.createMenu(mainMenuId, name, connectionName, ref);
    return "redirect:/setting/menu/" + menu.getId();
  }

  // ---- 메뉴 ----

  @GetMapping("/menu/{id}")
  public String menu(@PathVariable UUID id, Model model) {
    var menu = menuService.getMenu(id);
    model.addAttribute("menu", menu);
    model.addAttribute("dbTable", fieldService.get(menu.getDbTableId()));
    model.addAttribute(
        "queries",
        menu.getQueries().stream()
            .sorted(
                java.util.Comparator.comparing(MenuQuery::getKind)
                    .thenComparing(MenuQuery::getDisplayOrder)
                    .thenComparing(MenuQuery::getName))
            .toList());
    return "setting/menu";
  }

  @PostMapping("/menu/{id}")
  public String updateMenu(
      @PathVariable UUID id,
      @RequestParam String name,
      @RequestParam(required = false) String authority,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "false") boolean enableExcel,
      @RequestParam(defaultValue = "false") boolean enableDisplay,
      @RequestParam(defaultValue = "0") int displayOrder) {
    menuService.updateMenu(id, name, authority, pageSize, enableExcel, enableDisplay, displayOrder);
    return "redirect:/setting/menu/" + id;
  }

  @PostMapping("/menu/{id}/delete")
  public String deleteMenu(@PathVariable UUID id) {
    menuService.deleteMenu(id);
    return REDIRECT_INDEX;
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
    return "setting/queryInspection";
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
    return "redirect:/setting/menu/" + menuId;
  }

  @PostMapping("/menu/{menuId}/query/{queryId}/delete")
  public String deleteQuery(
      @PathVariable UUID menuId, @PathVariable UUID queryId, RedirectAttributes redirect) {
    return run(
        redirect, "redirect:/setting/menu/" + menuId, () -> queryService.delete(menuId, queryId));
  }

  // ---- 필드 ----

  @GetMapping("/table/{id}")
  public String table(@PathVariable UUID id, Model model) {
    var dbTable = fieldService.get(id);
    model.addAttribute("dbTable", dbTable);
    model.addAttribute(
        "fields",
        dbTable.getFields().stream()
            .sorted(
                java.util.Comparator.comparing(
                        net.luversof.web.dynamiccrud2.setting.domain.DbField::isVirtual)
                    .thenComparing(
                        net.luversof.web.dynamiccrud2.setting.domain.DbField::getDisplayOrder))
            .toList());
    model.addAttribute("inputTypes", InputType.values());
    return "setting/table";
  }

  @PostMapping("/table/{id}/field")
  public String updateField(
      @PathVariable UUID id,
      @RequestParam String columnName,
      @RequestParam(required = false) String label,
      @RequestParam InputType inputType,
      @RequestParam(defaultValue = "false") boolean required,
      @RequestParam(required = false) Integer maxLength,
      @RequestParam(defaultValue = "false") boolean sortable) {
    fieldService.updateField(id, columnName, label, inputType, required, maxLength, sortable);
    return "redirect:/setting/table/" + id;
  }

  @PostMapping("/table/{id}/resync")
  public String resync(@PathVariable UUID id) {
    fieldService.resync(id);
    return "redirect:/setting/table/" + id;
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
    return "setting/query";
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
