package net.luversof.web.dynamiccrud2.setting.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.setting.domain.MainMenu;
import net.luversof.web.dynamiccrud2.setting.domain.Menu;
import net.luversof.web.dynamiccrud2.setting.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.setting.domain.Project;
import net.luversof.web.dynamiccrud2.setting.domain.QueryKind;
import net.luversof.web.dynamiccrud2.setting.repository.MainMenuRepository;
import net.luversof.web.dynamiccrud2.setting.repository.MenuRepository;
import net.luversof.web.dynamiccrud2.setting.repository.ProjectRepository;

/** Project · MainMenu · Menu 관리와 메뉴 등록(테이블 필드 등록 + 조회 쿼리 자동 생성). */
@Service
public class MenuService {

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

  public Project saveProject(UUID id, String name, int displayOrder) {
    var project = id == null ? new Project() : projectRepository.findById(id).orElseThrow();
    project.setName(name.strip());
    project.setDisplayOrder(displayOrder);
    return projectRepository.save(project);
  }

  public void deleteProject(UUID id) {
    if (!mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    projectRepository.deleteById(id);
  }

  public MainMenu saveMainMenu(UUID id, UUID projectId, String name, int displayOrder) {
    var mainMenu = id == null ? new MainMenu() : mainMenuRepository.findById(id).orElseThrow();
    if (id == null) {
      mainMenu.setProjectId(projectRepository.findById(projectId).orElseThrow().getId());
    }
    mainMenu.setName(name.strip());
    mainMenu.setDisplayOrder(displayOrder);
    return mainMenuRepository.save(mainMenu);
  }

  public void deleteMainMenu(UUID id) {
    if (!menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    mainMenuRepository.deleteById(id);
  }

  public Menu getMenu(UUID id) {
    return menuRepository.findById(id).orElseThrow();
  }

  /** 1차 등록: connection · 테이블을 고르면 필드를 테이블 단위로 등록하고 조회 쿼리만 자동 생성한다. */
  public Menu createMenu(UUID mainMenuId, String name, String connectionName, TableRef ref) {
    mainMenuRepository.findById(mainMenuId).orElseThrow();
    var adapter = resolver.resolve(connectionName);
    var meta = adapter.readTable(ref);
    var dbTable = fieldService.registerOrGet(connectionName, meta);

    var read = new MenuQuery();
    read.setKind(QueryKind.READ);
    read.setName("조회");
    read.setQueryText(adapter.generate(QueryKind.READ, meta));

    var menu = new Menu();
    menu.setMainMenuId(mainMenuId);
    menu.setDbTableId(dbTable.getId());
    menu.setName(name.strip());
    menu.getQueries().add(read);
    return menuRepository.save(menu);
  }

  public Menu updateMenu(
      UUID id,
      String name,
      String authority,
      int pageSize,
      boolean enableExcel,
      boolean enableDisplay,
      int displayOrder) {
    var menu = getMenu(id);
    menu.setName(name.strip());
    menu.setAuthority(authority == null || authority.isBlank() ? null : authority.strip());
    menu.setPageSize(Math.max(1, pageSize));
    menu.setEnableExcel(enableExcel);
    menu.setEnableDisplay(enableDisplay);
    menu.setDisplayOrder(displayOrder);
    return menuRepository.save(menu);
  }

  /** 메뉴 삭제. 공유 필드(DbTable)는 다른 메뉴가 쓸 수 있어 남긴다. */
  public void deleteMenu(UUID id) {
    menuRepository.deleteById(id);
  }
}
