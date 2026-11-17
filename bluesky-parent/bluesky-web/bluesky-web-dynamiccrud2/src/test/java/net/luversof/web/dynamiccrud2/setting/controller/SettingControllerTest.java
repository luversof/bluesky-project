package net.luversof.web.dynamiccrud2.setting.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import net.luversof.web.dynamiccrud2.setting.domain.QueryKind;
import net.luversof.web.dynamiccrud2.setting.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.setting.service.MenuService;

/**
 * 설정 화면을 실제 컨트롤러 · JTE · 보안 필터로 렌더한다(브라우저 없이). 설정 catalog(dynamiccrud2) 자체 테이블을 대상으로 쓰고 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class SettingControllerTest implements GeneralWebTest {

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
    var anonymous = mvc.perform(get("/setting")).andExpect(status().is3xxRedirection()).andReturn();
    assertThat(anonymous.getResponse().getRedirectedUrl())
        .startsWith("https://user.web.bluesky.local/login/redirect?redirectUrl=")
        .endsWith("/setting");
    mvc.perform(get("/setting").header("HX-Request", "true")).andExpect(status().isUnauthorized());
    mvc.perform(get("/setting").with(user("u").roles("USER"))).andExpect(status().isForbidden());
    mvc.perform(get("/setting").with(user("m").roles("MASTER"))).andExpect(status().isOk());
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
    var project = menuService.saveProject(null, "ctl-test-" + UUID.randomUUID(), 0);
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", 0);
    var created = new ArrayList<UUID>();
    try {
      mvc.perform(get("/setting").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString(project.getName())));
      mvc.perform(get("/setting/mainMenu/" + mainMenu.getId() + "/menu/new").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("<option value=\"dynamiccrud2\">")));
      mvc.perform(
              get("/setting/connection/tables")
                  .param("connectionName", "dynamiccrud2")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("|MainMenu\"")));

      var result =
          mvc.perform(
                  post("/setting/mainMenu/" + mainMenu.getId() + "/menu")
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

      mvc.perform(get("/setting/menu/" + menuId).with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("수정 추가")));
      mvc.perform(
              get("/setting/menu/" + menuId + "/query/new").param("kind", "UPDATE").with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("UPDATE public.\"MainMenu\" SET")))
          .andExpect(content().string(Matchers.containsString("name=\"p.id.role\"")));
      mvc.perform(
              post("/setting/menu/" + menuId + "/query/inspect")
                  .param("kind", "READ")
                  .param("queryText", "SELECT id, name FROM \"MainMenu\" WHERE name = :name")
                  .header("HX-Request", "true")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("name=\"p.name.required\"")));

      // 저장 오류는 편집기를 다시 그리며 문구를 보인다
      mvc.perform(
              post("/setting/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\"")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 행을 바꿀 수 있어")));
      mvc.perform(
              post("/setting/menu/" + menuId + "/query")
                  .param("kind", "DELETE")
                  .param("queryText", "DELETE FROM \"MainMenu\" WHERE id = :id")
                  .param("p.id.role", "ROW_KEY")
                  .param("p.id.required", "true")
                  .with(admin()))
          .andExpect(status().is3xxRedirection());
      assertThat(menuService.getMenu(menuId).getQueries())
          .anyMatch(q -> q.getKind() == QueryKind.DELETE);

      mvc.perform(get("/setting/table/" + menu.getDbTableId()).with(admin()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("컬럼 다시 읽기")));
      mvc.perform(post("/setting/table/" + menu.getDbTableId() + "/resync").with(admin()))
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
