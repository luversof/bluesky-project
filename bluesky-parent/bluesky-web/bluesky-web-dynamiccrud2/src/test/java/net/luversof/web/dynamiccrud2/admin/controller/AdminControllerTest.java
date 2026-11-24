package net.luversof.web.dynamiccrud2.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.MenuRepository;

/**
 * 설정 화면을 실제 컨트롤러 · JTE · 보안 필터로 렌더한다(브라우저 없이). 설정 catalog(dynamiccrud2) 자체 테이블을 대상으로 쓰고 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class AdminControllerTest implements GeneralWebTest {

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;
  @Autowired MenuRepository menuRepository;
  @Autowired net.luversof.web.dynamiccrud2.repository.SettingLogRepository settingLogRepository;

  MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private static RequestPostProcessor admin() {
    return user("admin").roles("ADMIN");
  }

  @Test
  void accessByRole() throws Exception {
    var anonymous = mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andReturn();
    assertThat(anonymous.getResponse().getRedirectedUrl())
        .startsWith("https://user.web.bluesky.local/login/redirect?redirectUrl=")
        .endsWith("/admin");
    mvc.perform(get("/admin").header("HX-Request", "true")).andExpect(status().isUnauthorized());
    mvc.perform(get("/admin").with(user("u").roles("USER"))).andExpect(status().isForbidden());
    mvc.perform(get("/admin").with(user("master-user").roles("MASTER")))
        .andExpect(status().isOk())
        // 상단 메뉴: 로그인 사용자 이름 + 로그아웃(POST /logout)
        .andExpect(content().string(Matchers.containsString("master-user")))
        .andExpect(content().string(Matchers.containsString("action=\"/logout\"")));
    // 없는 id 는 500 이 아니라 404
    mvc.perform(get("/admin/menu/" + java.util.UUID.randomUUID()).with(admin()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/admin/table/" + java.util.UUID.randomUUID()).with(admin()))
        .andExpect(status().isNotFound());
  }

  /** 다국어: 브라우저 언어(Accept-Language)를 따르고, 상단 언어 전환(POST /locale)이 쿠키로 바꾼 뒤 보던 화면으로 돌아간다 */
  @Test
  void localeFollowsBrowserAndSwitch() throws Exception {
    mvc.perform(get("/admin").with(admin()))
        .andExpect(content().string(Matchers.containsString("<title>프로젝트 관리")))
        .andExpect(content().string(Matchers.containsString("lang=\"ko\"")));
    mvc.perform(get("/admin").header("Accept-Language", "en-US,en;q=0.9").with(admin()))
        .andExpect(content().string(Matchers.containsString("<title>Projects")))
        .andExpect(content().string(Matchers.containsString("lang=\"en\"")));
    var switched =
        mvc.perform(
                post("/locale")
                    .param("lang", "en")
                    .header("Referer", "https://dynamiccrud2.web.bluesky.local/admin?project=x")
                    .with(admin()))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    assertThat(switched.getResponse().getRedirectedUrl()).isEqualTo("/admin?project=x");
    var cookies = switched.getResponse().getCookies();
    assertThat(cookies).isNotEmpty();
    mvc.perform(get("/admin").cookie(cookies).with(admin()))
        .andExpect(content().string(Matchers.containsString("<title>Projects")));
  }

  /** 설정 변경 이력: 프로젝트 이름을 바꾸면 프로젝트 설정 이력에 이전 → 새 이름, 메뉴 설정을 바꾸면 그 메뉴의 "설정 변경" 탭에 남는다 */
  @Test
  void settingChangesAreLogged() throws Exception {
    var code = "t" + UUID.randomUUID().toString().substring(0, 8);
    var project = menuService.saveProject(null, code, "set-log-a");
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    var ref =
        resolver.resolve("dynamiccrud2").listTables().stream()
            .filter(t -> t.name().equals("Project"))
            .findFirst()
            .orElseThrow();
    var preexisting =
        dbTableRepository.findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
    UUID menuId = null;
    try {
      mvc.perform(
              post("/admin/project")
                  .param("id", project.getId().toString())
                  .param("code", code)
                  .param("name", "set-log-b")
                  .with(admin()))
          .andExpect(status().is3xxRedirection());
      mvc.perform(get("/admin/project/" + project.getId() + "/history").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("set-log-a</del>")))
          .andExpect(content().string(Matchers.containsString("set-log-b</ins>")));
      menuId = menuService.createMenu(mainMenu.getId(), "list", "m1", "dynamiccrud2", ref).getId();
      menuService.updateMenu(menuId, "list", "m1", null, 30, false, true);
      // 메뉴 변경 이력의 기본 탭은 메뉴 설정 변경
      mvc.perform(get("/admin/menu/" + menuId + "/history").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("페이지 크기")))
          .andExpect(content().string(Matchers.containsString("30</ins>")));
      // 바뀐 게 없는 저장은 남기지 않는다
      var before = settingLogRepository.countSearch(menuId, null, null, null, null, null, null);
      menuService.updateMenu(menuId, "list", "m1", null, 30, false, true);
      assertThat(settingLogRepository.countSearch(menuId, null, null, null, null, null, null))
          .isEqualTo(before);

      // 프로젝트 관리 트리의 메뉴 눈 아이콘: 숨기면 이력이 남고 프로젝트 화면으로 돌아온다
      mvc.perform(
              post("/admin/menu/" + menuId + "/display").param("display", "false").with(admin()))
          .andExpect(redirectedUrl("/admin?project=" + project.getId()));
      assertThat(menuService.getMenu(menuId).isEnableDisplay()).isFalse();
      assertThat(settingLogRepository.countSearch(menuId, null, null, null, null, null, null))
          .isEqualTo(before + 1);
      // 왼쪽 프로젝트 목록의 눈 아이콘: back 이 있으면 보던 프로젝트로 돌아온다
      var other = UUID.randomUUID();
      mvc.perform(
              post("/admin/project/" + project.getId() + "/display")
                  .param("display", "false")
                  .param("back", other.toString())
                  .with(admin()))
          .andExpect(redirectedUrl("/admin?project=" + other));
      assertThat(menuService.getProject(project.getId()).isEnableDisplay()).isFalse();

      // 순서 바꾸기는 이전 순서 → 새 순서로 남고, 같은 자리에 놓으면 남기지 않는다. 전체 설정 변경 이력 화면에서 보인다
      var second = menuService.saveMainMenu(null, project.getId(), "main2", "main2");
      try {
        menuService.reorderMainMenus(project.getId(), List.of(second.getId(), mainMenu.getId()));
        var count =
            settingLogRepository.countSearch(null, null, project.getId(), null, null, null, null);
        menuService.reorderMainMenus(project.getId(), List.of(second.getId(), mainMenu.getId()));
        assertThat(
                settingLogRepository.countSearch(
                    null, null, project.getId(), null, null, null, null))
            .isEqualTo(count);
        mvc.perform(get("/admin/history").param("target", "MAIN_MENU").with(admin()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("main, main2</del>")))
            .andExpect(content().string(Matchers.containsString("main2, main</ins>")));
      } finally {
        menuService.deleteMainMenu(second.getId());
      }
    } finally {
      if (menuId != null) {
        var dbTableId = menuService.getMenu(menuId).getDbTableId();
        menuService.deleteMenu(menuId);
        if (preexisting.isEmpty()) {
          dbTableRepository.deleteById(dbTableId);
        }
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
      settingLogRepository.deleteAll(
          settingLogRepository.search(
              null, null, project.getId(), null, null, null, null, 1000, 0));
      if (menuId != null) {
        settingLogRepository.deleteAll(
            settingLogRepository.search(menuId, null, null, null, null, null, null, 1000, 0));
      }
    }
  }

  @Test
  void newItemsGoLastAndReorderSaves() throws Exception {
    var startedAt = java.time.Instant.now();
    var first =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "order-a-" + UUID.randomUUID());
    var second =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "order-b-" + UUID.randomUUID());
    var m1 = menuService.saveMainMenu(null, first.getId(), "m1", "m1");
    var m2 = menuService.saveMainMenu(null, first.getId(), "m2", "m2");
    try {
      assertThat(second.getDisplayOrder()).isGreaterThan(first.getDisplayOrder());
      assertThat(m2.getDisplayOrder()).isGreaterThan(m1.getDisplayOrder());

      mvc.perform(
              post("/admin/project/order")
                  .param("ids", second.getId().toString(), first.getId().toString())
                  .with(admin()))
          .andExpect(status().isNoContent());
      mvc.perform(
              post("/admin/project/" + first.getId() + "/mainMenu/order")
                  .param("ids", m2.getId().toString(), m1.getId().toString())
                  .with(admin()))
          .andExpect(status().isNoContent());

      var names = menuService.tree().stream().map(node -> node.project().getId()).toList();
      assertThat(names.indexOf(second.getId())).isLessThan(names.indexOf(first.getId()));
      var mainMenus =
          menuService.tree().stream()
              .filter(node -> node.project().getId().equals(first.getId()))
              .findFirst()
              .orElseThrow()
              .mainMenus()
              .stream()
              .map(node -> node.mainMenu().getId())
              .toList();
      assertThat(mainMenus).containsExactly(m2.getId(), m1.getId());

      // 메뉴도 새 것은 맨 끝, 끌어서 바꾼 순서를 저장한다
      var ref =
          resolver.resolve("dynamiccrud2").listTables().stream()
              .filter(t -> t.name().equals("Project"))
              .findFirst()
              .orElseThrow();
      var n1 = menuService.createMenu(m1.getId(), "n1", "n1", "dynamiccrud2", ref);
      var n2 = menuService.createMenu(m1.getId(), "n2", "n2", "dynamiccrud2", ref);
      try {
        assertThat(n2.getDisplayOrder()).isGreaterThan(n1.getDisplayOrder());
        mvc.perform(
                post("/admin/mainMenu/" + m1.getId() + "/menu/order")
                    .param("ids", n2.getId().toString(), n1.getId().toString())
                    .with(admin()))
            .andExpect(status().isNoContent());
        assertThat(menuService.getMenu(n2.getId()).getDisplayOrder())
            .isLessThan(menuService.getMenu(n1.getId()).getDisplayOrder());
      } finally {
        menuService.deleteMenu(n1.getId());
        menuService.deleteMenu(n2.getId());
      }
      // 관리 권한이 없으면 순서도 못 바꾼다
      mvc.perform(
              post("/admin/project/order")
                  .param("ids", first.getId().toString())
                  .with(user("u").roles("USER")))
          .andExpect(status().isForbidden());
    } finally {
      menuService.deleteMainMenu(m1.getId());
      menuService.deleteMainMenu(m2.getId());
      menuService.deleteProject(first.getId());
      menuService.deleteProject(second.getId());
      net.luversof.web.dynamiccrud2.TestSettingLogs.purge(settingLogRepository, first.getId());
      net.luversof.web.dynamiccrud2.TestSettingLogs.purge(settingLogRepository, second.getId());
      // 프로젝트 순서 변경은 어느 프로젝트에도 속하지 않는다 - 이 시험이 시작한 뒤의 것만
      settingLogRepository.deleteAll(
          settingLogRepository.search(null, null, null, null, "PROJECT", startedAt, null, 100, 0));
    }
  }

  @Test
  void rootShowsProjectListAndCodesAreValidated() throws Exception {
    mvc.perform(get("/").with(user("u").roles("USER"))).andExpect(status().isOk());
    // 앱 경로와 겹치는 code · 형식 틀린 code 는 저장할 수 없다
    assertThatThrownBy(() -> menuService.saveProject(null, "admin", "x"))
        .extracting("errorCode")
        .isEqualTo("QueryErrorCode.RESERVED_CODE");
    assertThatThrownBy(() -> menuService.saveProject(null, "Bad Code", "x"))
        .extracting("errorCode")
        .isEqualTo("QueryErrorCode.INVALID_CODE");
    var code = "t" + UUID.randomUUID().toString().substring(0, 8);
    var project = menuService.saveProject(null, code, "dup");
    try {
      assertThatThrownBy(() -> menuService.saveProject(null, code, "dup2"))
          .extracting("errorCode")
          .isEqualTo("QueryErrorCode.DUPLICATE_CODE");
      // 같은 프로젝트를 같은 code 로 다시 저장(이름만 바꾸기)은 된다
      assertThat(menuService.saveProject(project.getId(), code, "renamed").getName())
          .isEqualTo("renamed");
    } finally {
      menuService.deleteProject(project.getId());
      net.luversof.web.dynamiccrud2.TestSettingLogs.purge(settingLogRepository, project.getId());
    }
  }

  @Test
  void settingScreensRender() throws Exception {
    var ref =
        resolver.resolve("dynamiccrud2").listTables().stream()
            .filter(t -> t.name().equals("MainMenu"))
            .findFirst()
            .orElseThrow();
    var preexisting =
        dbTableRepository.findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
    var project =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "ctl-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    var created = new ArrayList<UUID>();
    try {
      mvc.perform(get("/admin").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString(project.getName())));
      // 메뉴 추가는 관리 화면 메인 메뉴 아래 펼침 폼(connection · 테이블은 검색해서 고르는 칸)
      mvc.perform(get("/admin").param("project", project.getId().toString()).with(admin()))
          .andExpect(status().isOk())
          .andExpect(
              content().string(Matchers.containsString("id=\"add-menu-" + mainMenu.getId() + "\"")))
          .andExpect(content().string(Matchers.containsString("data-combobox")));
      // 자동완성: 이름 일부로 찾는다
      mvc.perform(
              get("/admin/connection/search")
                  .param("keyword", "CRUD2")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("data-value=\"dynamiccrud2\"")));
      // 없는 이름을 적으면 테이블 칸에 안내가 나온다(오류 화면이 아니라)
      mvc.perform(
              get("/admin/connection/tables")
                  .param("connectionName", "no-such-connection-zzz")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("connection 을 찾을 수 없습니다")));
      mvc.perform(
              get("/admin/connection/tables")
                  .param("connectionName", "dynamiccrud2")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("|MainMenu\"")));

      var result =
          mvc.perform(
                  post("/admin/mainMenu/" + mainMenu.getId() + "/menu")
                      .param("code", "screen")
                      .param("name", "화면 테스트")
                      .param("connectionName", "dynamiccrud2")
                      .param(
                          "table",
                          (ref.catalog() == null ? "" : ref.catalog())
                              + "|"
                              + (ref.schema() == null ? "" : ref.schema())
                              + "|"
                              + ref.name())
                      .with(admin()))
              .andExpect(status().is3xxRedirection())
              .andReturn();
      // 만들면 관리 화면(그 프로젝트)에 머문다 - 설정은 메뉴 이름을 눌러 메뉴 설정 화면에서
      assertThat(result.getResponse().getRedirectedUrl())
          .isEqualTo("/admin?project=" + project.getId());
      var menuId =
          menuRepository.findByMainMenuIdAndCode(mainMenu.getId(), "screen").orElseThrow().getId();
      created.add(menuId);
      var menu = menuService.getMenu(menuId);

      mvc.perform(get("/admin/menu/" + menuId).with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("수정 추가")))
          // 라벨을 정하는 필드 화면으로 가는 단추(들어온 메뉴로 돌아오도록 menu 를 붙인다)
          .andExpect(content().string(Matchers.containsString("필드 관리")))
          .andExpect(content().string(Matchers.containsString("?menu=" + menuId)));
      // 프로젝트 관리 화면의 복사 아이콘(이름 없이): 숨김 복사본을 만들고 프로젝트 관리 화면에 머문다
      mvc.perform(post("/admin/menu/" + menuId + "/copy").with(admin()))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/admin?project=" + project.getId()));
      var copied =
          menuRepository.findByMainMenuIdAndCode(mainMenu.getId(), "screen-copy").orElseThrow();
      created.add(copied.getId());
      assertThat(copied.isEnableDisplay()).isFalse();
      mvc.perform(get("/admin/menu/" + menuId + "/query/new").param("kind", "UPDATE").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("UPDATE public.\"MainMenu\" SET")))
          .andExpect(content().string(Matchers.containsString("name=\"p.id.role\"")));
      // 메뉴 관리 화면의 레이어(htmx): 화면 틀 없이 폼 조각만, 취소는 레이어 닫기
      mvc.perform(
              get("/admin/menu/" + menuId + "/query/new")
                  .param("kind", "UPDATE")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("hx-target=\"#queryPanel\"")))
          .andExpect(content().string(Matchers.containsString("data-modal-close")))
          .andExpect(content().string(Matchers.not(Matchers.containsString("<html"))));
      mvc.perform(
              post("/admin/menu/" + menuId + "/query/inspect")
                  .param("kind", "READ")
                  .param("queryText", "SELECT id, name FROM \"MainMenu\" WHERE name = :name")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("name=\"p.name.required\"")));

      // 저장 오류는 편집기를 다시 그리며 문구를 보인다(레이어면 레이어 안에)
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\"")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(header().doesNotExist("HX-Refresh"))
          .andExpect(content().string(Matchers.containsString("전체 행을 바꿀 수 있어")))
          .andExpect(content().string(Matchers.not(Matchers.containsString("<html"))));
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\"")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 행을 바꿀 수 있어")));
      // DB 가 받지 않는 쿼리(없는 컬럼)는 저장하지 않는다 - 실행하지 않는 검사(EXPLAIN)로 확인
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "UPDATE")
                  .param("queryText", "UPDATE \"MainMenu\" SET no_such_column = :x WHERE id = :id")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("DB 가 쿼리를 받지 않습니다")));
      // 쿼리 테스트: 문제가 없으면 성공 안내
      mvc.perform(
              post("/admin/menu/" + menuId + "/query/inspect")
                  .param("kind", "UPDATE")
                  .param("queryText", "UPDATE \"MainMenu\" SET name = :name WHERE id = :id")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("문제가 없습니다")));
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\" WHERE id = :id")
                  .param("p.id.role", "ROW_KEY")
                  .param("p.id.required", "true")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(header().string("HX-Refresh", "true"));
      assertThat(menuService.getMenu(menuId).getQueries())
          .anyMatch(q -> q.getKind() == QueryKind.DELETE);

      mvc.perform(get("/admin/table/" + menu.getDbTableId()).with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("컬럼 다시 읽기")));
      mvc.perform(post("/admin/table/" + menu.getDbTableId() + "/resync").with(admin()))
          .andExpect(status().is3xxRedirection());
    } finally {
      for (var menuId : created) {
        var dbTableId = menuService.getMenu(menuId).getDbTableId();
        menuService.deleteMenu(menuId);
        if (preexisting.isEmpty()) {
          dbTableRepository.deleteById(dbTableId);
        }
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
      net.luversof.web.dynamiccrud2.TestSettingLogs.purge(settingLogRepository, project.getId());
    }
  }
}
