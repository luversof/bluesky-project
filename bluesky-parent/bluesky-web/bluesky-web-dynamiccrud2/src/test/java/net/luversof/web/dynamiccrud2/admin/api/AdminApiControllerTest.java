package net.luversof.web.dynamiccrud2.admin.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.TestSettingLogs;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.SettingLogRepository;

/**
 * 관리 JSON 호출(/admin/api/**): 화면과 같은 서비스 · 검사를 JSON 으로. 설정 catalog(dynamiccrud2) 자체 테이블로 만들고 만든 것만
 * 지운다
 */
class AdminApiControllerTest implements GeneralWebTest {

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;
  @Autowired SettingLogRepository settingLogRepository;
  @Autowired net.luversof.web.dynamiccrud2.admin.service.FieldService fieldService;

  MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private static RequestPostProcessor admin() {
    return user("admin").roles("ADMIN");
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder request, String body) {
    return request
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private String idOf(org.springframework.test.web.servlet.ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }

  /** 작업(쿼리) 초안 · 검사 · 추가 · 바꾸기 · 지우기, 검색 설정, 테이블 필드(공유 테이블이라 바꾼 라벨은 되돌린다) */
  @Test
  void queriesSearchAndFields() throws Exception {
    var ref =
        resolver.resolve("dynamiccrud2").listTables().stream()
            .filter(t -> t.name().equals("MainMenu"))
            .findFirst()
            .orElseThrow();
    var preexisting =
        dbTableRepository.findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
    var project =
        menuService.saveProject(null, "a" + UUID.randomUUID().toString().substring(0, 8), "api-q");
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    var menu = menuService.createMenu(mainMenu.getId(), "list", "목록q", "dynamiccrud2", ref);
    var base = "/admin/api/menus/" + menu.getId();
    var tableId = menu.getDbTableId();
    var since = java.time.Instant.now();
    String originalLabel = null;
    var labelChanged = false;
    try {
      var draft =
          JsonPath.<String>read(
              mvc.perform(get(base + "/queries/draft").param("kind", "UPDATE").with(admin()))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              "$.queryText");
      assertThat(draft).startsWith("UPDATE \"public\".\"MainMenu\" SET");
      var quoted = draft.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
      mvc.perform(
              json(
                      post(base + "/queries/inspect"),
                      "{\"kind\":\"UPDATE\",\"queryText\":\"" + quoted + "\"}")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.error").doesNotExist())
          .andExpect(
              jsonPath("$.params[?(@.name == 'id')].setting.role")
                  .value(Matchers.hasItem("ROW_KEY")));
      mvc.perform(
              json(
                      post(base + "/queries/explain"),
                      "{\"kind\":\"UPDATE\",\"queryText\":\"" + quoted + "\"}")
                  .with(admin()))
          .andExpect(jsonPath("$.lines").isNotEmpty());
      var added =
          mvc.perform(
                  json(
                          post(base + "/queries"),
                          "{\"kind\":\"UPDATE\",\"name\":\"수정\",\"queryText\":\"" + quoted + "\"}")
                      .with(admin()))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      String queryId =
          JsonPath.<java.util.List<String>>read(added, "$.queries[?(@.kind == 'UPDATE')].id")
              .get(0);
      mvc.perform(
              json(
                      put(base + "/queries/" + queryId),
                      "{\"name\":\"이름 수정\",\"queryText\":\"" + quoted + "\"}")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.queries[?(@.kind == 'UPDATE')].name").value(Matchers.hasItem("이름 수정")));
      // WHERE 없는 수정은 거부
      mvc.perform(
              json(
                      post(base + "/queries"),
                      "{\"kind\":\"UPDATE\",\"name\":\"x\",\"queryText\":\"UPDATE \\\"MainMenu\\\" SET name = :name\"}")
                  .with(admin()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").isNotEmpty());
      mvc.perform(delete(base + "/queries/" + queryId).with(admin()))
          .andExpect(status().isNoContent());

      // 검색 설정: 후보에서 골라 저장
      mvc.perform(get(base + "/search").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.parsed").value(true))
          .andExpect(jsonPath("$.candidates[?(@.label == 'name')]").isNotEmpty());
      mvc.perform(
              json(
                      put(base + "/search"),
                      "{\"fields\":{\"name\":{\"operator\":\"CONTAINS\"}},\"keyword\":[\"name\"]}")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.searchSettings.fields[0].label").value("name"));
      // 본문 모양이 틀리면(모르는 방식) {"error"} 400
      mvc.perform(
              json(put(base + "/search"), "{\"fields\":{\"name\":{\"operator\":\"NOPE\"}}}")
                  .with(admin()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").isNotEmpty());

      // 테이블 필드: 라벨 바꾸고 원래대로
      var table =
          mvc.perform(get("/admin/api/tables/" + tableId).with(admin()))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      originalLabel =
          JsonPath.<java.util.List<String>>read(table, "$.fields[?(@.columnName == 'name')].label")
              .get(0);
      labelChanged = true;
      mvc.perform(
              json(
                      put("/admin/api/tables/" + tableId + "/fields/name"),
                      "{\"label\":\"API 이름\",\"inputType\":\"TEXT\",\"sortable\":false,\"updatable\":true}")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.fields[?(@.columnName == 'name')].label")
                  .value(Matchers.hasItem("API 이름")));
      mvc.perform(post("/admin/api/tables/" + tableId + "/resync").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.columnsReadDate").isNotEmpty());

      // 변경 이력: 위에서 바꾼 것들이 보인다. 데이터 변경은 없음, 없는 이력 되돌리기는 400
      mvc.perform(get(base + "/history/settings").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.rows[*].targetName", Matchers.hasItem("이름 수정")));
      mvc.perform(get("/admin/api/projects/" + project.getId() + "/history").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.total").value(Matchers.greaterThan(0)));
      mvc.perform(get("/admin/api/tables/" + tableId + "/history").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.rows[*].targetName", Matchers.hasItem("MainMenu.name")));
      mvc.perform(
              get("/admin/api/history")
                  .param("target", "QUERY")
                  .param("from", java.time.LocalDate.now().toString())
                  .with(admin()))
          .andExpect(status().isOk());
      mvc.perform(get(base + "/history/data").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.total").value(0));
      mvc.perform(
              post(base + "/history/" + UUID.randomUUID() + "/revert")
                  .with(admin())
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").isNotEmpty());
    } finally {
      if (labelChanged) {
        var field =
            fieldService.get(tableId).getFields().stream()
                .filter(f -> f.getColumnName().equals("name"))
                .findFirst()
                .orElseThrow();
        fieldService.updateField(
            tableId,
            "name",
            originalLabel,
            field.getInputType(),
            field.getMaxLength(),
            field.isSortable(),
            field.isUpdatable(),
            field.getValueOptions(),
            field.getDateFormat());
      }
      menuService.deleteMenu(menu.getId());
      if (preexisting.isEmpty()) {
        dbTableRepository.deleteById(tableId);
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
      TestSettingLogs.purge(settingLogRepository, project.getId());
      TestSettingLogs.purgeFields(settingLogRepository, null, tableId, since);
    }
  }

  /** 프로젝트 → 메인 메뉴 → 메뉴 만들기 · 바꾸기 · 복사 · 옮기기 · 보이기 · 지우기, 하위가 있으면 못 지움, 없는 id 404, USER 403 */
  @Test
  void structureLifecycle() throws Exception {
    var ref =
        resolver.resolve("dynamiccrud2").listTables().stream()
            .filter(t -> t.name().equals("MainMenu"))
            .findFirst()
            .orElseThrow();
    var preexisting =
        dbTableRepository.findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
    var code = "a" + UUID.randomUUID().toString().substring(0, 8);
    var projectId =
        idOf(
            mvc.perform(
                    json(
                            post("/admin/api/projects"),
                            "{\"code\":\"" + code + "\",\"name\":\"api-p\"}")
                        .with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code)));
    String menuId = null;
    String copyId = null;
    String mainId = null;
    String otherId = null;
    try {
      // 형식이 틀린 주소 이름은 400 + 사유
      mvc.perform(
              json(post("/admin/api/projects"), "{\"code\":\"Bad Code\",\"name\":\"x\"}")
                  .with(admin()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").isNotEmpty());
      mainId =
          idOf(
              mvc.perform(
                      json(
                              post("/admin/api/projects/" + projectId + "/main-menus"),
                              "{\"code\":\"main\",\"name\":\"메인\"}")
                          .with(admin()))
                  .andExpect(status().isCreated()));
      otherId =
          idOf(
              mvc.perform(
                      json(
                              post("/admin/api/projects/" + projectId + "/main-menus"),
                              "{\"code\":\"other\",\"name\":\"다른\"}")
                          .with(admin()))
                  .andExpect(status().isCreated()));
      mvc.perform(get("/admin/api/connections/dynamiccrud2/tables").with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[?(@.name == 'MainMenu')]").isNotEmpty());
      menuId =
          idOf(
              mvc.perform(
                      json(
                              post("/admin/api/main-menus/" + mainId + "/menus"),
                              "{\"code\":\"list\",\"name\":\"목록\",\"connectionName\":\"dynamiccrud2\",\"schema\":\""
                                  + ref.schema()
                                  + "\",\"table\":\"MainMenu\"}")
                          .with(admin()))
                  .andExpect(status().isCreated())
                  .andExpect(jsonPath("$.queries[0].kind").value("READ")));
      mvc.perform(
              json(
                      put("/admin/api/menus/" + menuId),
                      "{\"code\":\"list\",\"name\":\"목록2\",\"authority\":null,\"pageSize\":30,\"enableExcel\":true,\"enableDisplay\":true}")
                  .with(admin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pageSize").value(30));
      copyId =
          idOf(
              mvc.perform(post("/admin/api/menus/" + menuId + "/copy").with(admin()))
                  .andExpect(status().isCreated())
                  .andExpect(jsonPath("$.code").value("list-copy"))
                  .andExpect(jsonPath("$.enableDisplay").value(false)));
      // 다른 메인 메뉴로 옮기기(순서에 넣기)
      mvc.perform(
              json(
                      put("/admin/api/main-menus/" + otherId + "/menus/order"),
                      "{\"ids\":[\"" + copyId + "\"]}")
                  .with(admin()))
          .andExpect(status().isNoContent());
      mvc.perform(get("/admin/api/menus/" + copyId).with(admin()))
          .andExpect(jsonPath("$.mainMenuId").value(otherId));
      mvc.perform(
              json(put("/admin/api/menus/" + copyId + "/display"), "{\"display\":true}")
                  .with(admin()))
          .andExpect(jsonPath("$.enableDisplay").value(true));
      mvc.perform(get("/admin/api/projects").with(admin()))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath(
                  "$[?(@.project.id == '" + projectId + "')].mainMenus[*].menus[*].name",
                  Matchers.hasItems("목록2", "목록2 복사본")));
      // 하위가 있으면 못 지움
      mvc.perform(
              delete("/admin/api/main-menus/" + mainId)
                  .with(admin())
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").isNotEmpty());
      // 없는 id
      mvc.perform(
              get("/admin/api/menus/" + UUID.randomUUID())
                  .with(admin())
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
      // 관리 권한이 없으면 403
      mvc.perform(
              get("/admin/api/projects")
                  .with(user("tester").roles("USER"))
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());

      mvc.perform(delete("/admin/api/menus/" + copyId).with(admin()))
          .andExpect(status().isNoContent());
      copyId = null;
      mvc.perform(delete("/admin/api/menus/" + menuId).with(admin()))
          .andExpect(status().isNoContent());
      menuId = null;
      mvc.perform(delete("/admin/api/main-menus/" + otherId).with(admin()))
          .andExpect(status().isNoContent());
      otherId = null;
      mvc.perform(delete("/admin/api/main-menus/" + mainId).with(admin()))
          .andExpect(status().isNoContent());
      mainId = null;
      mvc.perform(delete("/admin/api/projects/" + projectId).with(admin()))
          .andExpect(status().isNoContent());
      assertThat(menuService.tree()).noneMatch(p -> p.project().getCode().equals(code));
      projectId = null;
    } finally {
      for (var id : new String[] {copyId, menuId}) {
        if (id != null) {
          menuService.deleteMenu(UUID.fromString(id));
        }
      }
      for (var id : new String[] {otherId, mainId}) {
        if (id != null) {
          menuService.deleteMainMenu(UUID.fromString(id));
        }
      }
      if (projectId != null) {
        menuService.deleteProject(UUID.fromString(projectId));
      }
      var tableId =
          dbTableRepository.findIdByLocation(
              "dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
      if (preexisting.isEmpty() && tableId.isPresent()) {
        dbTableRepository.deleteById(tableId.get());
      }
      settingLogRepository.search(null, null, null, "admin", null, null, null, 1000, 0).stream()
          .filter(log ->
              log.getTargetName() != null
                  && (log.getTargetName().startsWith("api-p")
                      || log.getTargetName().startsWith("목록")
                      || log.getTargetName().equals("메인")
                      || log.getTargetName().equals("다른")))
          .forEach(settingLogRepository::delete);
    }
  }
}
