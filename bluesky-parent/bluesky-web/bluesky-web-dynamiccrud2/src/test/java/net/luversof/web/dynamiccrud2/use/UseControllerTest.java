package net.luversof.web.dynamiccrud2.use;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
import net.luversof.web.dynamiccrud2.setting.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.setting.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.setting.domain.QueryKind;
import net.luversof.web.dynamiccrud2.setting.repository.ChangeLogRepository;
import net.luversof.web.dynamiccrud2.setting.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.setting.repository.ProjectRepository;
import net.luversof.web.dynamiccrud2.setting.service.MenuService;
import net.luversof.web.dynamiccrud2.setting.service.QueryService;

/**
 * 사용 화면 한 바퀴: 설정 catalog 의 Project 테이블을 대상으로 입력 → 검색 → 수정 → 정렬 · 엑셀 → 삭제 → 변경 이력 → 권한. 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class UseControllerTest implements GeneralWebTest {

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired QueryService queryService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;
  @Autowired ProjectRepository projectRepository;
  @Autowired ChangeLogRepository changeLogRepository;

  MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private static RequestPostProcessor normalUser() {
    return user("tester").roles("USER");
  }

  @Test
  void insertSearchUpdateExportDelete() throws Exception {
    var ref =
        resolver.resolve("dynamiccrud2").listTables().stream()
            .filter(t -> t.name().equals("Project"))
            .findFirst()
            .orElseThrow();
    var preexisting =
        dbTableRepository.findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name());
    var owner = menuService.saveProject(null, "use-test-" + UUID.randomUUID(), 0);
    var mainMenu = menuService.saveMainMenu(null, owner.getId(), "main", 0);
    var rowId = UUID.randomUUID();
    var name = "row-" + rowId;
    UUID menuId = null;
    try {
      var menu = menuService.createMenu(mainMenu.getId(), "프로젝트 관리", "dynamiccrud2", ref);
      menuId = menu.getId();
      menuService.updateMenu(menuId, menu.getName(), null, 20, true, true, 0);
      queryService.save(
          menuId,
          null,
          QueryKind.READ,
          null,
          "SELECT id, name AS \"이름\", \"displayOrder\", \"createdDate\" FROM \"Project\" WHERE name = :name ORDER BY \"createdDate\"",
          Map.of("name", new ParamSetting(Role.SEARCH, false, null)),
          null);
      queryService.save(
          menuId,
          null,
          QueryKind.INSERT,
          "등록",
          queryService.draft(menuService.getMenu(menuId), QueryKind.INSERT).queryText(),
          null,
          null);
      queryService.save(
          menuId,
          null,
          QueryKind.UPDATE,
          "이름 수정",
          "UPDATE \"Project\" SET name = :name WHERE id = :id",
          null,
          null);
      queryService.save(
          menuId,
          null,
          QueryKind.DELETE,
          "삭제",
          queryService.draft(menuService.getMenu(menuId), QueryKind.DELETE).queryText(),
          null,
          null);
      var saved = menuService.getMenu(menuId);
      var insert = idOf(saved, QueryKind.INSERT);
      var update = idOf(saved, QueryKind.UPDATE);
      var delete = idOf(saved, QueryKind.DELETE);
      var base = "/use/menu/" + menuId;

      mvc.perform(get("/use").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("프로젝트 관리")));
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 0건")));

      // 입력: uuid · 숫자 · 날짜시간 문자열이 컬럼 타입으로 바뀌어야 PostgreSQL 이 받는다
      mvc.perform(
              post(base + "/action/" + insert + "/run")
                  .param("v.id", rowId.toString())
                  .param("v.name", name)
                  .param("v.displayOrder", "3")
                  .param("v.createdDate", "2026-11-17T10:00:00")
                  .header("HX-Request", "true")
                  .header("HX-Current-URL", "https://dev.bluesky.local:40125" + base)
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(header().string("HX-Redirect", "https://dev.bluesky.local:40125" + base));
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 1건")))
          .andExpect(content().string(Matchers.containsString("이름")))
          .andExpect(content().string(Matchers.containsString("2026-11-17T10:00")));

      // 수정 폼: 선택한 행 값으로 채운다(별칭 "이름" → 파라미터 name)
      var rowJson = "{\"id\":\"" + rowId + "\",\"이름\":\"" + name + "\"}";
      mvc.perform(
              post(base + "/action/" + update + "/form").param("row", rowJson).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("value=\"" + name + "\"")));
      mvc.perform(
              post(base + "/action/" + update + "/run")
                  .param("rows", rowJson)
                  .param("v.name", "")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("값을 입력하세요")));
      mvc.perform(
              post(base + "/action/" + update + "/run")
                  .param("rows", rowJson)
                  .param("v.name", name + "-2")
                  .with(normalUser()))
          .andExpect(header().exists("HX-Redirect"));
      mvc.perform(
              get(base)
                  .param("s.name", name + "-2")
                  .param("sort", "id")
                  .param("desc", "true")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 1건")))
          .andExpect(content().string(Matchers.containsString(" ▼")));

      mvc.perform(get(base + "/excel").param("s.name", name + "-2").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(
              header()
                  .string("Content-Type", Matchers.startsWith("application/vnd.openxmlformats")));

      mvc.perform(
              post(base + "/action/" + delete + "/run").param("rows", rowJson).with(normalUser()))
          .andExpect(header().exists("HX-Redirect"));
      assertThat(projectRepository.findById(rowId)).isEmpty();
      assertThat(changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId))
          .hasSize(3)
          .allSatisfy(log -> assertThat(log.getUsername()).isEqualTo("tester"));

      // 메뉴를 ADMIN 전용으로 바꾸면 USER 는 403, 목록에서도 빠진다
      menuService.updateMenu(menuId, "프로젝트 관리", "ROLE_ADMIN", 20, true, true, 0);
      mvc.perform(get(base).with(normalUser())).andExpect(status().isForbidden());
      mvc.perform(get("/use").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      mvc.perform(get(base).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    } finally {
      projectRepository.deleteById(rowId);
      if (menuId != null) {
        changeLogRepository.deleteAll(
            changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId));
        var dbTableId = menuService.getMenu(menuId).getDbTableId();
        menuService.deleteMenu(menuId);
        if (preexisting.isEmpty()) {
          dbTableRepository.deleteById(dbTableId);
        }
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(owner.getId());
    }
  }

  private static UUID idOf(net.luversof.web.dynamiccrud2.setting.domain.Menu menu, QueryKind kind) {
    return menu.getQueries().stream()
        .filter(q -> q.getKind() == kind)
        .findFirst()
        .orElseThrow()
        .getId();
  }
}
