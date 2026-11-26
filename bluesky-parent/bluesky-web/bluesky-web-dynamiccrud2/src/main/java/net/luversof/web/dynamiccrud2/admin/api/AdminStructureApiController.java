package net.luversof.web.dynamiccrud2.admin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ApiError;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.CodeName;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.Display;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MainMenuTree;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MainMenuView;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuCopy;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuCreate;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuSummary;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuUpdate;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuView;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.Order;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ProjectTree;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ProjectView;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.connection.ConnectionNameService;

/**
 * 관리 JSON 호출 - 구조(프로젝트 · 메인 메뉴 · 메뉴). 관리 화면(AdminController)과 같은 서비스 · 검사 · 설정 변경 이력을 거친다. /admin/** 라
 * 설정 관리 권한(AdminAccessInterceptor)이 막는다.
 */
@RestController
@RequestMapping("/admin/api")
@Tag(name = "관리 - 구조", description = "프로젝트 · 메인 메뉴 · 메뉴 만들기 · 바꾸기 · 지우기 · 순서 · 보이기")
@ApiResponse(
    responseCode = "400",
    description = "주소 이름 형식 · 겹침, 하위가 있어 못 지움 등",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "401", description = "로그인 없음")
@ApiResponse(responseCode = "403", description = "설정 관리 권한 없음")
@ApiResponse(
    responseCode = "404",
    description = "없는 id",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
public class AdminStructureApiController {

  private final MenuService menuService;
  private final ConnectionNameService connectionNameService;
  private final DatabaseAdapterResolver resolver;

  public AdminStructureApiController(
      MenuService menuService,
      ConnectionNameService connectionNameService,
      DatabaseAdapterResolver resolver) {
    this.menuService = menuService;
    this.connectionNameService = connectionNameService;
    this.resolver = resolver;
  }

  // ---- 프로젝트 ----

  @Operation(summary = "전체 구조", description = "프로젝트 → 메인 메뉴 → 메뉴(순서대로, 숨긴 것 포함)")
  @GetMapping("/projects")
  public List<ProjectTree> tree() {
    return menuService.tree().stream()
        .map(p ->
            new ProjectTree(
                ProjectView.of(p.project()),
                p.mainMenus().stream()
                    .map(m ->
                        new MainMenuTree(
                            MainMenuView.of(m.mainMenu()),
                            m.menus().stream().map(MenuSummary::of).toList()))
                    .toList()))
        .toList();
  }

  @Operation(summary = "프로젝트 만들기")
  @PostMapping("/projects")
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectView createProject(@RequestBody CodeName body) {
    return ProjectView.of(menuService.saveProject(null, body.code(), body.name()));
  }

  @Operation(summary = "프로젝트 이름 · 주소 이름 바꾸기")
  @PutMapping("/projects/{id}")
  public ProjectView updateProject(@PathVariable UUID id, @RequestBody CodeName body) {
    menuService.getProject(id);
    return ProjectView.of(menuService.saveProject(id, body.code(), body.name()));
  }

  @Operation(summary = "프로젝트 지우기", description = "메인 메뉴가 있으면 지우지 않는다(400)")
  @DeleteMapping("/projects/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProject(@PathVariable UUID id) {
    menuService.deleteProject(id);
  }

  @Operation(summary = "프로젝트 보이기 · 숨기기", description = "숨기면 그 아래 메뉴는 사용 화면 목록 · 주소 모두 막힌다")
  @PutMapping("/projects/{id}/display")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void projectDisplay(@PathVariable UUID id, @RequestBody Display body) {
    menuService.setProjectDisplay(id, body.display());
  }

  @Operation(summary = "프로젝트 순서")
  @PutMapping("/projects/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void projectOrder(@RequestBody Order body) {
    menuService.reorderProjects(body.ids());
  }

  // ---- 메인 메뉴 ----

  @Operation(summary = "메인 메뉴 만들기")
  @PostMapping("/projects/{projectId}/main-menus")
  @ResponseStatus(HttpStatus.CREATED)
  public MainMenuView createMainMenu(@PathVariable UUID projectId, @RequestBody CodeName body) {
    menuService.getProject(projectId);
    return MainMenuView.of(menuService.saveMainMenu(null, projectId, body.code(), body.name()));
  }

  @Operation(summary = "메인 메뉴 이름 · 주소 이름 바꾸기")
  @PutMapping("/main-menus/{id}")
  public MainMenuView updateMainMenu(@PathVariable UUID id, @RequestBody CodeName body) {
    var projectId = menuService.getMainMenu(id).getProjectId();
    return MainMenuView.of(menuService.saveMainMenu(id, projectId, body.code(), body.name()));
  }

  @Operation(summary = "메인 메뉴 지우기", description = "메뉴가 있으면 지우지 않는다(400)")
  @DeleteMapping("/main-menus/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMainMenu(@PathVariable UUID id) {
    menuService.deleteMainMenu(id);
  }

  @Operation(summary = "메인 메뉴 보이기 · 숨기기")
  @PutMapping("/main-menus/{id}/display")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void mainMenuDisplay(@PathVariable UUID id, @RequestBody Display body) {
    menuService.setMainMenuDisplay(id, body.display());
  }

  @Operation(summary = "메인 메뉴 순서(프로젝트 안)")
  @PutMapping("/projects/{projectId}/main-menus/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void mainMenuOrder(@PathVariable UUID projectId, @RequestBody Order body) {
    menuService.reorderMainMenus(projectId, body.ids());
  }

  // ---- 메뉴 ----

  @Operation(summary = "연결 이름 찾기", description = "메뉴 만들기의 connectionName 후보")
  @GetMapping("/connections")
  public List<String> connections(@RequestParam(defaultValue = "") String keyword) {
    return connectionNameService.search(keyword);
  }

  @Operation(summary = "연결의 테이블 목록", description = "메뉴 만들기의 catalog · schema · table 후보")
  @GetMapping("/connections/{connectionName}/tables")
  public List<TableRef> tables(@PathVariable String connectionName) {
    try {
      return resolver.resolve(connectionName).listTables();
    } catch (org.springframework.dao.DataAccessException e) {
      throw connectionFailed(connectionName, e);
    }
  }

  @Operation(summary = "메뉴 만들기", description = "필드를 테이블 단위로 등록하고 조회 쿼리를 만든다. 만든 메뉴는 보이는 상태")
  @PostMapping("/main-menus/{mainMenuId}/menus")
  @ResponseStatus(HttpStatus.CREATED)
  public MenuView createMenu(@PathVariable UUID mainMenuId, @RequestBody MenuCreate body) {
    if (body.table() == null || body.table().isBlank()) {
      throw new IllegalArgumentException(
          net.luversof.web.dynamiccrud2.config.Msg.t("admin.menuCreate.tableFromList"));
    }
    var ref = new TableRef(blankToNull(body.catalog()), blankToNull(body.schema()), body.table());
    try {
      return MenuView.of(
          menuService.createMenu(mainMenuId, body.code(), body.name(), body.connectionName(), ref));
    } catch (org.springframework.dao.DataAccessResourceFailureException e) {
      // 대상 연결 실패만(설정 DB 오류는 그대로 - AdminApiAdvice)
      throw connectionFailed(body.connectionName(), e);
    }
  }

  @Operation(summary = "메뉴 설정", description = "작업(쿼리) · 검색 설정 포함")
  @GetMapping("/menus/{id}")
  public MenuView menu(@PathVariable UUID id) {
    return MenuView.of(menuService.getMenu(id));
  }

  @Operation(summary = "메뉴 설정 바꾸기", description = "이름 · 주소 이름 · 권한 · 페이지 크기 · 엑셀 · 보이기")
  @PutMapping("/menus/{id}")
  public MenuView updateMenu(@PathVariable UUID id, @RequestBody MenuUpdate body) {
    return MenuView.of(
        menuService.updateMenu(
            id,
            body.code(),
            body.name(),
            body.authority(),
            body.pageSize(),
            body.enableExcel(),
            body.enableDisplay()));
  }

  @Operation(summary = "메뉴 보이기 · 숨기기")
  @PutMapping("/menus/{id}/display")
  public MenuView menuDisplay(@PathVariable UUID id, @RequestBody Display body) {
    return MenuView.of(menuService.setMenuDisplay(id, body.display()));
  }

  @Operation(summary = "메뉴 복사", description = "같은 메인 메뉴 맨 끝에 쿼리 · 검색 설정 · 권한까지 그대로, 숨김으로")
  @PostMapping("/menus/{id}/copy")
  @ResponseStatus(HttpStatus.CREATED)
  public MenuView copyMenu(@PathVariable UUID id, @RequestBody(required = false) MenuCopy body) {
    var blank = body == null || (isBlank(body.code()) && isBlank(body.name()));
    return MenuView.of(
        blank
            ? menuService.copyMenu(id)
            : menuService.copyMenu(id, body.code(), body.name() == null ? "" : body.name()));
  }

  @Operation(summary = "메뉴 지우기", description = "공유 필드(테이블)는 남는다")
  @DeleteMapping("/menus/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMenu(@PathVariable UUID id) {
    menuService.getMenu(id);
    menuService.deleteMenu(id);
  }

  @Operation(
      summary = "메뉴 순서 · 옮기기",
      description = "메인 메뉴 안 순서. 다른 메인 메뉴(같은 프로젝트)의 메뉴 id 를 넣으면 이 메인 메뉴로 옮긴다 - 주소 이름이 겹치면 400")
  @PutMapping("/main-menus/{mainMenuId}/menus/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void menuOrder(@PathVariable UUID mainMenuId, @RequestBody Order body) {
    menuService.reorderMenus(mainMenuId, body.ids());
  }

  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(AdminStructureApiController.class);

  /** 연결 실패(서버에 닿지 않음 · 계정 거부): 400 + 관리 화면과 같은 안내, 원인은 로그에 */
  private static IllegalArgumentException connectionFailed(String connectionName, Exception e) {
    var name = connectionName == null ? "" : connectionName.strip();
    LOG.warn("connection {} unavailable: {}", name, e.getMessage());
    return new IllegalArgumentException(
        net.luversof.web.dynamiccrud2.config.Msg.t("admin.menuCreate.connFailed", name), e);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String blankToNull(String value) {
    return isBlank(value) ? null : value;
  }
}
