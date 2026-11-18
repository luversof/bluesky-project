package net.luversof.web.dynamiccrud2.admin.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.MainMenu;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.Project;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;
import net.luversof.web.dynamiccrud2.repository.ProjectRepository;

/** Project · MainMenu · Menu 관리와 메뉴 등록(테이블 필드 등록 + 조회 쿼리 자동 생성). */
@Service
public class MenuService {

  /** 주소 이름(code): 영문 소문자 · 숫자 · -, 1~50자 */
  private static final Pattern CODE = Pattern.compile("^[a-z0-9][a-z0-9-]{0,49}$");

  /** 프로젝트 code 는 주소 첫 단계라 앱이 쓰는 경로와 겹치면 안 된다 */
  public static final Set<String> RESERVED_PROJECT_CODES =
      Set.of("admin", "error", "actuator", "login", "logout", "oauth2", "js", "webjars");

  private final ProjectRepository projectRepository;
  private final MainMenuRepository mainMenuRepository;
  private final MenuRepository menuRepository;
  private final FieldService fieldService;
  private final DatabaseAdapterResolver resolver;

  public MenuService(
      ProjectRepository projectRepository,
      MainMenuRepository mainMenuRepository,
      MenuRepository menuRepository,
      FieldService fieldService,
      DatabaseAdapterResolver resolver) {
    this.projectRepository = projectRepository;
    this.mainMenuRepository = mainMenuRepository;
    this.menuRepository = menuRepository;
    this.fieldService = fieldService;
    this.resolver = resolver;
  }

  /** 설정 첫 화면과 사용 화면 메뉴 목록이 쓰는 트리 */
  public record MainMenuNode(MainMenu mainMenu, List<Menu> menus) {}

  public record ProjectNode(Project project, List<MainMenuNode> mainMenus) {}

  public List<ProjectNode> tree() {
    return projectRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
        .map(project ->
            new ProjectNode(
                project,
                mainMenuRepository
                    .findByProjectIdOrderByDisplayOrderAscNameAsc(project.getId())
                    .stream()
                    .map(mainMenu ->
                        new MainMenuNode(
                            mainMenu,
                            menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(
                                mainMenu.getId())))
                    .toList()))
        .toList();
  }

  /** 새 프로젝트는 맨 끝에 붙는다. 순서는 {@link #reorderProjects} 로 바꾼다 */
  public Project saveProject(UUID id, String code, String name) {
    var project = id == null ? new Project() : projectRepository.findById(id).orElseThrow();
    var checked = checkCode(code);
    if (RESERVED_PROJECT_CODES.contains(checked)) {
      throw new BlueskyException(QueryErrorCode.RESERVED_CODE, checked);
    }
    projectRepository
        .findByCode(checked)
        .filter(other -> !other.getId().equals(id))
        .ifPresent(other -> duplicate(checked));
    project.setCode(checked);
    if (id == null) {
      project.setDisplayOrder(
          projectRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                  .mapToInt(Project::getDisplayOrder)
                  .max()
                  .orElse(-1)
              + 1);
    }
    project.setName(name.strip());
    return projectRepository.save(project);
  }

  /** 드래그 앤 드롭 결과: 받은 순서대로 0, 1, 2 … 를 매긴다. 목록에 없는 id 는 무시 */
  public void reorderProjects(List<UUID> orderedIds) {
    var projects = projectRepository.findAllById(orderedIds);
    for (var project : projects) {
      project.setDisplayOrder(orderedIds.indexOf(project.getId()));
    }
    projectRepository.saveAll(projects);
  }

  public void deleteProject(UUID id) {
    if (!mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    projectRepository.deleteById(id);
  }

  /** 새 메인 메뉴는 그 프로젝트의 맨 끝에 붙는다. 순서는 {@link #reorderMainMenus} 로 바꾼다 */
  public MainMenu saveMainMenu(UUID id, UUID projectId, String code, String name) {
    var mainMenu = id == null ? new MainMenu() : mainMenuRepository.findById(id).orElseThrow();
    var checked = checkCode(code);
    var parentId = id == null ? projectId : mainMenu.getProjectId();
    mainMenuRepository
        .findByProjectIdAndCode(parentId, checked)
        .filter(other -> !other.getId().equals(id))
        .ifPresent(other -> duplicate(checked));
    mainMenu.setCode(checked);
    if (id == null) {
      mainMenu.setProjectId(projectRepository.findById(projectId).orElseThrow().getId());
      mainMenu.setDisplayOrder(
          mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(projectId).stream()
                  .mapToInt(MainMenu::getDisplayOrder)
                  .max()
                  .orElse(-1)
              + 1);
    }
    mainMenu.setName(name.strip());
    return mainMenuRepository.save(mainMenu);
  }

  /** 한 프로젝트 안 메인 메뉴 순서. 다른 프로젝트의 id 가 섞여 오면 무시한다 */
  public void reorderMainMenus(UUID projectId, List<UUID> orderedIds) {
    var mainMenus = mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(projectId);
    for (var mainMenu : mainMenus) {
      var index = orderedIds.indexOf(mainMenu.getId());
      if (index >= 0) {
        mainMenu.setDisplayOrder(index);
      }
    }
    mainMenuRepository.saveAll(mainMenus);
  }

  public void deleteMainMenu(UUID id) {
    if (!menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    mainMenuRepository.deleteById(id);
  }

  /** 사용 화면 메뉴 목록에 보일지(관리 화면 표시 · 숨김 단추) */
  public void setProjectDisplay(UUID id, boolean display) {
    var project = projectRepository.findById(id).orElseThrow();
    project.setEnableDisplay(display);
    projectRepository.save(project);
  }

  public void setMainMenuDisplay(UUID id, boolean display) {
    var mainMenu = getMainMenu(id);
    mainMenu.setEnableDisplay(display);
    mainMenuRepository.save(mainMenu);
  }

  public MainMenu getMainMenu(UUID id) {
    return mainMenuRepository.findById(id).orElseThrow();
  }

  public Menu getMenu(UUID id) {
    return menuRepository.findById(id).orElseThrow();
  }

  /** 1차 등록: connection · 테이블을 고르면 필드를 테이블 단위로 등록하고 조회 쿼리만 자동 생성한다. */
  public Menu createMenu(
      UUID mainMenuId, String code, String name, String connectionName, TableRef ref) {
    mainMenuRepository.findById(mainMenuId).orElseThrow();
    var checked = checkMenuCode(mainMenuId, null, code);
    var adapter = resolver.resolve(connectionName);
    var meta = adapter.readTable(ref);
    var dbTable = fieldService.registerOrGet(connectionName, meta);

    var read = new MenuQuery();
    read.setKind(QueryKind.READ);
    read.setName("조회");
    read.setQueryText(adapter.generate(QueryKind.READ, meta));

    var menu = new Menu();
    menu.setMainMenuId(mainMenuId);
    // 새 메뉴는 맨 끝(프로젝트 · 메인 메뉴와 같다)
    menu.setDisplayOrder(
        menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(mainMenuId).stream()
                .mapToInt(Menu::getDisplayOrder)
                .max()
                .orElse(-1)
            + 1);
    menu.setDbTableId(dbTable.getId());
    menu.setCode(checked);
    menu.setName(name.strip());
    menu.getQueries().add(read);
    return menuRepository.save(menu);
  }

  public Menu updateMenu(
      UUID id,
      String code,
      String name,
      String authority,
      int pageSize,
      boolean enableExcel,
      boolean enableDisplay) {
    var menu = getMenu(id);
    menu.setCode(checkMenuCode(menu.getMainMenuId(), id, code));
    menu.setName(name.strip());
    menu.setAuthority(authority == null || authority.isBlank() ? null : authority.strip());
    menu.setPageSize(Math.max(1, pageSize));
    menu.setEnableExcel(enableExcel);
    menu.setEnableDisplay(enableDisplay);
    return menuRepository.save(menu);
  }

  /** 관리 화면에서 그 자리 수정: 이름 · 주소 이름만(나머지 설정은 메뉴 설정 화면) */
  public Menu renameMenu(UUID id, String code, String name) {
    var menu = getMenu(id);
    menu.setCode(checkMenuCode(menu.getMainMenuId(), id, code));
    menu.setName(name.strip());
    return menuRepository.save(menu);
  }

  /** 메인 메뉴 안 메뉴 순서(드래그 앤 드롭). 목록에 없는 id 는 순서를 그대로 둔다 */
  public void reorderMenus(UUID mainMenuId, List<UUID> orderedIds) {
    var menus = menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(mainMenuId);
    for (var menu : menus) {
      var index = orderedIds.indexOf(menu.getId());
      if (index >= 0) {
        menu.setDisplayOrder(index);
      }
    }
    menuRepository.saveAll(menus);
  }

  private String checkMenuCode(UUID mainMenuId, UUID menuId, String code) {
    var checked = checkCode(code);
    menuRepository
        .findByMainMenuIdAndCode(mainMenuId, checked)
        .filter(other -> !other.getId().equals(menuId))
        .ifPresent(other -> duplicate(checked));
    return checked;
  }

  private static String checkCode(String code) {
    var checked = code == null ? "" : code.strip();
    if (!CODE.matcher(checked).matches()) {
      throw new BlueskyException(QueryErrorCode.INVALID_CODE, checked);
    }
    return checked;
  }

  private static void duplicate(String code) {
    throw new BlueskyException(QueryErrorCode.DUPLICATE_CODE, code);
  }

  /** 메뉴 삭제. 공유 필드(DbTable)는 다른 메뉴가 쓸 수 있어 남긴다. */
  public void deleteMenu(UUID id) {
    menuRepository.deleteById(id);
  }
}
