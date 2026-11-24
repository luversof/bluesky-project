package net.luversof.web.dynamiccrud2.admin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.domain.MainMenu;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.Project;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Action;
import net.luversof.web.dynamiccrud2.domain.SettingLog.Target;
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
  private final SettingLogService settingLog;

  public MenuService(
      ProjectRepository projectRepository,
      MainMenuRepository mainMenuRepository,
      MenuRepository menuRepository,
      FieldService fieldService,
      DatabaseAdapterResolver resolver,
      SettingLogService settingLog) {
    this.projectRepository = projectRepository;
    this.mainMenuRepository = mainMenuRepository;
    this.menuRepository = menuRepository;
    this.fieldService = fieldService;
    this.resolver = resolver;
    this.settingLog = settingLog;
  }

  /** 설정 첫 화면과 사용 화면 메뉴 목록이 쓰는 트리 */
  public record MainMenuNode(MainMenu mainMenu, List<Menu> menus) {}

  public record ProjectNode(Project project, List<MainMenuNode> mainMenus) {}

  public List<ProjectNode> tree() {
    // 프로젝트 · 메인 메뉴마다 묻지 않고 세 번에 읽어 메모리에서 묶는다(사용 화면이 요청마다 부른다)
    var menus =
        menuRepository.findAll().stream()
            .sorted(
                java.util.Comparator.comparingInt(Menu::getDisplayOrder)
                    .thenComparing(Menu::getName))
            .collect(java.util.stream.Collectors.groupingBy(Menu::getMainMenuId));
    var mainMenus =
        mainMenuRepository.findAll().stream()
            .sorted(
                java.util.Comparator.comparingInt(MainMenu::getDisplayOrder)
                    .thenComparing(MainMenu::getName))
            .collect(java.util.stream.Collectors.groupingBy(MainMenu::getProjectId));
    return projectRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
        .map(project ->
            new ProjectNode(
                project,
                mainMenus.getOrDefault(project.getId(), List.of()).stream()
                    .map(mainMenu ->
                        new MainMenuNode(mainMenu, menus.getOrDefault(mainMenu.getId(), List.of())))
                    .toList()))
        .toList();
  }

  /** 새 프로젝트는 맨 끝에 붙는다. 순서는 {@link #reorderProjects} 로 바꾼다 */
  public Project saveProject(UUID id, String code, String name) {
    var project = id == null ? new Project() : projectRepository.findById(id).orElseThrow();
    var before = id == null ? null : SettingLogService.snapshot(project);
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
    var saved = projectRepository.save(project);
    settingLog.record(
        Target.PROJECT,
        id == null ? Action.CREATE : Action.UPDATE,
        saved.getId(),
        saved.getName(),
        saved.getId(),
        null,
        before,
        SettingLogService.snapshot(saved));
    return saved;
  }

  /** 드래그 앤 드롭 결과: 받은 순서대로 0, 1, 2 … 를 매긴다. 목록에 없는 id 는 무시 */
  public void reorderProjects(List<UUID> orderedIds) {
    // 이력에 "이전 순서 → 새 순서" 로 보이게 바꾸기 전 순서를 떠 둔다
    var before =
        SettingLogService.order(
            projectRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(Project::getName)
                .toList());
    var projects = projectRepository.findAllById(orderedIds);
    for (var project : projects) {
      project.setDisplayOrder(orderedIds.indexOf(project.getId()));
    }
    projectRepository.saveAll(projects);
    settingLog.record(
        Target.PROJECT,
        Action.REORDER,
        null,
        null,
        null,
        null,
        before,
        SettingLogService.order(
            projects.stream()
                .sorted(Comparator.comparingInt(Project::getDisplayOrder))
                .map(Project::getName)
                .toList()));
  }

  public void deleteProject(UUID id) {
    if (!mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    var project = projectRepository.findById(id).orElseThrow();
    projectRepository.deleteById(id);
    settingLog.record(
        Target.PROJECT,
        Action.DELETE,
        id,
        project.getName(),
        id,
        null,
        SettingLogService.snapshot(project),
        null);
  }

  /** 새 메인 메뉴는 그 프로젝트의 맨 끝에 붙는다. 순서는 {@link #reorderMainMenus} 로 바꾼다 */
  public MainMenu saveMainMenu(UUID id, UUID projectId, String code, String name) {
    var mainMenu = id == null ? new MainMenu() : mainMenuRepository.findById(id).orElseThrow();
    var before = id == null ? null : SettingLogService.snapshot(mainMenu);
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
    var saved = mainMenuRepository.save(mainMenu);
    settingLog.record(
        Target.MAIN_MENU,
        id == null ? Action.CREATE : Action.UPDATE,
        saved.getId(),
        saved.getName(),
        saved.getProjectId(),
        null,
        before,
        SettingLogService.snapshot(saved));
    return saved;
  }

  /** 한 프로젝트 안 메인 메뉴 순서. 다른 프로젝트의 id 가 섞여 오면 무시한다 */
  public void reorderMainMenus(UUID projectId, List<UUID> orderedIds) {
    var mainMenus = mainMenuRepository.findByProjectIdOrderByDisplayOrderAscNameAsc(projectId);
    var before = SettingLogService.order(mainMenus.stream().map(MainMenu::getName).toList());
    for (var mainMenu : mainMenus) {
      var index = orderedIds.indexOf(mainMenu.getId());
      if (index >= 0) {
        mainMenu.setDisplayOrder(index);
      }
    }
    mainMenuRepository.saveAll(mainMenus);
    settingLog.record(
        Target.MAIN_MENU,
        Action.REORDER,
        null,
        null,
        projectId,
        null,
        before,
        SettingLogService.order(
            mainMenus.stream()
                .sorted(Comparator.comparingInt(MainMenu::getDisplayOrder))
                .map(MainMenu::getName)
                .toList()));
  }

  public void deleteMainMenu(UUID id) {
    if (!menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(id).isEmpty()) {
      throw new BlueskyException(QueryErrorCode.HAS_CHILDREN);
    }
    var mainMenu = getMainMenu(id);
    mainMenuRepository.deleteById(id);
    settingLog.record(
        Target.MAIN_MENU,
        Action.DELETE,
        id,
        mainMenu.getName(),
        mainMenu.getProjectId(),
        null,
        SettingLogService.snapshot(mainMenu),
        null);
  }

  /** 사용 화면 메뉴 목록에 보일지(관리 화면 표시 · 숨김 단추) */
  public void setProjectDisplay(UUID id, boolean display) {
    var project = projectRepository.findById(id).orElseThrow();
    var before = SettingLogService.snapshot(project);
    project.setEnableDisplay(display);
    projectRepository.save(project);
    settingLog.record(
        Target.PROJECT,
        Action.UPDATE,
        id,
        project.getName(),
        id,
        null,
        before,
        SettingLogService.snapshot(project));
  }

  public void setMainMenuDisplay(UUID id, boolean display) {
    var mainMenu = getMainMenu(id);
    var before = SettingLogService.snapshot(mainMenu);
    mainMenu.setEnableDisplay(display);
    mainMenuRepository.save(mainMenu);
    settingLog.record(
        Target.MAIN_MENU,
        Action.UPDATE,
        id,
        mainMenu.getName(),
        mainMenu.getProjectId(),
        null,
        before,
        SettingLogService.snapshot(mainMenu));
  }

  public Menu setMenuDisplay(UUID id, boolean display) {
    var menu = getMenu(id);
    var before = SettingLogService.snapshot(menu);
    menu.setEnableDisplay(display);
    var saved = menuRepository.save(menu);
    settingLog.record(
        Target.MENU,
        Action.UPDATE,
        id,
        saved.getName(),
        null,
        id,
        before,
        SettingLogService.snapshot(saved));
    return saved;
  }

  public Project getProject(UUID id) {
    return projectRepository.findById(id).orElseThrow();
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
    var mainMenu = mainMenuRepository.findById(mainMenuId).orElseThrow();
    var checked = checkMenuCode(mainMenuId, null, code);
    var adapter = resolver.resolve(connectionName);
    var meta = adapter.readTable(ref);
    var dbTable = fieldService.registerOrGet(connectionName, meta);

    var read = new MenuQuery();
    read.setKind(QueryKind.READ);
    read.setName(Msg.t("query.kind.READ"));
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
    var saved = menuRepository.save(menu);
    var after = SettingLogService.snapshot(saved);
    after.put("table", connectionName + " / " + ref.name());
    settingLog.record(
        Target.MENU,
        Action.CREATE,
        saved.getId(),
        saved.getName(),
        mainMenu.getProjectId(),
        saved.getId(),
        null,
        after);
    return saved;
  }

  /**
   * 메뉴 복사: 같은 메인 메뉴 맨 끝에 같은 테이블 · 쿼리(조회 · 입력 · 수정 · 삭제) · 검색 설정 · 권한 · 페이지 크기 · 엑셀을 그대로 둔 새 메뉴. 다
   * 고치기 전에 사용자 화면에 나오지 않게 숨김으로 만든다. 쿼리 id 는 새로 매긴다(데이터 변경 이력은 원래 메뉴에 남는다).
   */
  /**
   * 이름 · 주소 이름을 묻지 않는 복사(프로젝트 관리 화면의 복사 아이콘, 사용자 결정 11-24): 이름은 "원래 이름 복사본", 주소 이름은 같은 메인 메뉴에서 겹치지
   * 않게 원래-copy, 원래-copy2 … (50자 안으로 원래 이름을 자른다). 숨김으로 만들어지니 설정 화면에서 고친 뒤 켠다.
   */
  public Menu copyMenu(UUID id) {
    var source = getMenu(id);
    var base = source.getCode().substring(0, Math.min(source.getCode().length(), 42)) + "-copy";
    var code = base;
    for (int n = 2;
        menuRepository.findByMainMenuIdAndCode(source.getMainMenuId(), code).isPresent();
        n++) {
      code = base + n;
    }
    var name = Msg.t("admin.menu.copy.name", source.getName());
    return copyMenu(id, code, name.substring(0, Math.min(name.length(), 100)));
  }

  public Menu copyMenu(UUID id, String code, String name) {
    var source = getMenu(id);
    var copy = new Menu();
    copy.setMainMenuId(source.getMainMenuId());
    copy.setCode(checkMenuCode(source.getMainMenuId(), null, code));
    copy.setName(name.strip());
    copy.setDbTableId(source.getDbTableId());
    copy.setAuthority(source.getAuthority());
    copy.setPageSize(source.getPageSize());
    copy.setEnableExcel(source.isEnableExcel());
    copy.setEnableDisplay(false);
    copy.setSearchSettings(
        source.getSearchSettings() == null
            ? null
            : new java.util.LinkedHashMap<>(source.getSearchSettings()));
    copy.setDisplayOrder(
        menuRepository
                .findByMainMenuIdOrderByDisplayOrderAscNameAsc(source.getMainMenuId())
                .stream()
                .mapToInt(Menu::getDisplayOrder)
                .max()
                .orElse(-1)
            + 1);
    for (var query : source.getQueries()) {
      var copied = new MenuQuery();
      copied.setKind(query.getKind());
      copied.setName(query.getName());
      copied.setQueryText(query.getQueryText());
      copied.setParamSettings(
          query.getParamSettings() == null
              ? null
              : new java.util.LinkedHashMap<>(query.getParamSettings()));
      copied.setAuthority(query.getAuthority());
      copied.setDisplayOrder(query.getDisplayOrder());
      copy.getQueries().add(copied);
    }
    var saved = menuRepository.save(copy);
    var after = SettingLogService.snapshot(saved);
    after.put("copiedFrom", source.getName());
    settingLog.record(
        Target.MENU,
        Action.CREATE,
        saved.getId(),
        saved.getName(),
        getMainMenu(saved.getMainMenuId()).getProjectId(),
        saved.getId(),
        null,
        after);
    return saved;
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
    var before = SettingLogService.snapshot(menu);
    menu.setCode(checkMenuCode(menu.getMainMenuId(), id, code));
    menu.setName(name.strip());
    menu.setAuthority(authority == null || authority.isBlank() ? null : authority.strip());
    menu.setPageSize(Math.max(1, pageSize));
    menu.setEnableExcel(enableExcel);
    menu.setEnableDisplay(enableDisplay);
    var saved = menuRepository.save(menu);
    settingLog.record(
        Target.MENU,
        Action.UPDATE,
        id,
        saved.getName(),
        null,
        id,
        before,
        SettingLogService.snapshot(saved));
    return saved;
  }

  /** 관리 화면에서 그 자리 수정: 이름 · 주소 이름만(나머지 설정은 메뉴 설정 화면) */
  public Menu renameMenu(UUID id, String code, String name) {
    var menu = getMenu(id);
    var before = SettingLogService.snapshot(menu);
    menu.setCode(checkMenuCode(menu.getMainMenuId(), id, code));
    menu.setName(name.strip());
    var saved = menuRepository.save(menu);
    settingLog.record(
        Target.MENU,
        Action.UPDATE,
        id,
        saved.getName(),
        null,
        id,
        before,
        SettingLogService.snapshot(saved));
    return saved;
  }

  /**
   * 메인 메뉴 안 메뉴 순서(드래그 앤 드롭). 목록에 없는 id 는 순서를 그대로 둔다. 다른 메인 메뉴의 메뉴 id 가 오면(끌어 옮김, 사용자 결정 11-24) 같은
   * 프로젝트일 때만 이 메인 메뉴로 옮긴다 - 주소 이름은 메인 메뉴 안에서 겹치면 안 되므로 겹치면 막는다(DUPLICATE_CODE). 다른 프로젝트 메뉴는 무시(화면은
   * 한 프로젝트만 보여 오지 않는다, 테이블 · 연결이 맞는지 모름).
   */
  public void reorderMenus(UUID mainMenuId, List<UUID> orderedIds) {
    var mainMenu = getMainMenu(mainMenuId);
    var menus =
        new java.util.ArrayList<>(
            menuRepository.findByMainMenuIdOrderByDisplayOrderAscNameAsc(mainMenuId));
    var before = SettingLogService.order(menus.stream().map(Menu::getName).toList());
    var moves = new java.util.ArrayList<Menu>();
    var movedBefore = new java.util.HashMap<UUID, java.util.Map<String, String>>();
    for (var id : orderedIds) {
      if (menus.stream().anyMatch(menu -> menu.getId().equals(id))) {
        continue;
      }
      var menu = getMenu(id);
      var from = getMainMenu(menu.getMainMenuId());
      if (!from.getProjectId().equals(mainMenu.getProjectId())) {
        continue;
      }
      var snapshot = SettingLogService.snapshot(menu);
      snapshot.put("mainMenu", from.getName());
      movedBefore.put(id, snapshot);
      menu.setCode(checkMenuCode(mainMenuId, id, menu.getCode()));
      menu.setMainMenuId(mainMenuId);
      menus.add(menu);
      moves.add(menu);
    }
    for (var menu : menus) {
      var index = orderedIds.indexOf(menu.getId());
      if (index >= 0) {
        menu.setDisplayOrder(index);
      }
    }
    menuRepository.saveAll(menus);
    for (var menu : moves) {
      var after = SettingLogService.snapshot(menu);
      after.put("mainMenu", mainMenu.getName());
      settingLog.record(
          Target.MENU,
          Action.UPDATE,
          menu.getId(),
          menu.getName(),
          mainMenu.getProjectId(),
          menu.getId(),
          movedBefore.get(menu.getId()),
          after);
    }
    settingLog.record(
        Target.MENU,
        Action.REORDER,
        mainMenuId,
        mainMenu.getName(),
        mainMenu.getProjectId(),
        null,
        before,
        SettingLogService.order(
            menus.stream()
                .sorted(Comparator.comparingInt(Menu::getDisplayOrder))
                .map(Menu::getName)
                .toList()));
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
    var menu = menuRepository.findById(id).orElse(null);
    menuRepository.deleteById(id);
    if (menu != null) {
      // 메뉴가 지워져 메뉴 범위로는 못 보므로 프로젝트 범위로 남긴다
      settingLog.record(
          Target.MENU,
          Action.DELETE,
          id,
          menu.getName(),
          mainMenuRepository
              .findById(menu.getMainMenuId())
              .map(MainMenu::getProjectId)
              .orElse(null),
          null,
          SettingLogService.snapshot(menu),
          null);
    }
  }
}
