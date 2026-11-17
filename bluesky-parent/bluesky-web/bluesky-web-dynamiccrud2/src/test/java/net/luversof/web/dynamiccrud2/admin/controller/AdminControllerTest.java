package net.luversof.web.dynamiccrud2.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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

/**
 * 설정 화면을 실제 컨트롤러 · JTE · 보안 필터로 렌더한다(브라우저 없이). 설정 catalog(dynamiccrud2) 자체 테이블을 대상으로 쓰고 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class AdminControllerTest implements GeneralWebTest {

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;

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

  @Test
  void newItemsGoLastAndReorderSaves() throws Exception {
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
      mvc.perform(get("/admin/mainMenu/" + mainMenu.getId() + "/menu/new").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("list=\"connectionOptions\"")));
      // 자동완성: 이름 일부로 찾는다
      mvc.perform(
              get("/admin/connection/search")
                  .param("keyword", "CRUD2")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("<option value=\"dynamiccrud2\">")));
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
      var location = result.getResponse().getRedirectedUrl();
      var menuId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
      created.add(menuId);
      var menu = menuService.getMenu(menuId);

      mvc.perform(get("/admin/menu/" + menuId).with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("수정 추가")))
          // 라벨을 정하는 필드 화면으로 가는 단추(들어온 메뉴로 돌아오도록 menu 를 붙인다)
          .andExpect(content().string(Matchers.containsString("필드 · 라벨 설정")))
          .andExpect(content().string(Matchers.containsString("?menu=" + menuId)));
      mvc.perform(get("/admin/menu/" + menuId + "/query/new").param("kind", "UPDATE").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("UPDATE public.\"MainMenu\" SET")))
          .andExpect(content().string(Matchers.containsString("name=\"p.id.role\"")));
      mvc.perform(
              post("/admin/menu/" + menuId + "/query/inspect")
                  .param("kind", "READ")
                  .param("queryText", "SELECT id, name FROM \"MainMenu\" WHERE name = :name")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("name=\"p.name.required\"")));

      // 저장 오류는 편집기를 다시 그리며 문구를 보인다
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\"")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 행을 바꿀 수 있어")));
      mvc.perform(
              post("/admin/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\" WHERE id = :id")
                  .param("p.id.role", "ROW_KEY")
                  .param("p.id.required", "true")
                  .with(admin()))
          .andExpect(status().is3xxRedirection());
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
    }
  }
}
