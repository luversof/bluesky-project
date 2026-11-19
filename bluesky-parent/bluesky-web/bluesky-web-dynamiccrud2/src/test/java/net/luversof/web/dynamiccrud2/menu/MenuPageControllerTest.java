package net.luversof.web.dynamiccrud2.menu;

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
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.admin.service.SearchService;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;
import net.luversof.web.dynamiccrud2.menu.service.RowSigner;
import net.luversof.web.dynamiccrud2.repository.ChangeLogRepository;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.ProjectRepository;

/**
 * 사용 화면 한 바퀴: 설정 catalog 의 Project 테이블을 대상으로 입력 → 검색 → 수정 → 정렬 · 엑셀 → 삭제 → 변경 이력 → 권한. 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class MenuPageControllerTest implements GeneralWebTest {

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired QueryService queryService;
  @Autowired SearchService searchService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;
  @Autowired ProjectRepository projectRepository;
  @Autowired ChangeLogRepository changeLogRepository;
  @Autowired RowSigner rowSigner;

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
    var owner =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "use-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, owner.getId(), "main", "main");
    var rowId = UUID.randomUUID();
    var name = "row-" + rowId;
    UUID menuId = null;
    try {
      var menu = menuService.createMenu(mainMenu.getId(), "list", "프로젝트 관리", "dynamiccrud2", ref);
      menuId = menu.getId();
      menuService.updateMenu(menuId, "list", menu.getName(), null, 20, true, true);
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
      var base = "/" + owner.getCode() + "/main/list";

      mvc.perform(get("/").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("프로젝트 관리")))
          // 관리 권한이 없으면 상단 "관리" 링크도 없다
          .andExpect(content().string(Matchers.not(Matchers.containsString("href=\"/admin\""))));
      // 프로젝트 · 메인 메뉴 주소까지만 와도 첫 메뉴로, 없는 메인 메뉴는 404
      mvc.perform(get("/" + owner.getCode() + "/main").with(normalUser()))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", base));
      mvc.perform(get("/" + owner.getCode()).with(normalUser()))
          .andExpect(header().string("Location", base));
      mvc.perform(get("/" + owner.getCode() + "/nope").with(normalUser()))
          .andExpect(status().isNotFound());
      // 메인 메뉴 · 프로젝트를 숨기면 전체 메뉴에서 아래 메뉴까지 빠지고, 메뉴 주소로는 그대로 열린다
      menuService.setMainMenuDisplay(mainMenu.getId(), false);
      mvc.perform(get("/").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      mvc.perform(get(base).with(normalUser())).andExpect(status().isOk());
      menuService.setMainMenuDisplay(mainMenu.getId(), true);
      menuService.setProjectDisplay(owner.getId(), false);
      mvc.perform(get("/").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      menuService.setProjectDisplay(owner.getId(), true);
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 0건")));

      // 입력: uuid · 숫자 · 날짜시간 문자열이 컬럼 타입으로 바뀌어야 PostgreSQL 이 받는다
      mvc.perform(
              post(base + "/action/" + insert + "/run")
                  .param("v.id", rowId.toString())
                  .param("v.code", "r" + rowId.toString().substring(0, 8))
                  .param("v.name", name)
                  .param("v.displayOrder", "3")
                  .param("v.enableDisplay", "true")
                  .param("v.createdDate", "2026-11-17T10:00:00")
                  .header("HX-Request", "true")
                  .with(normalUser()))
          .andExpect(status().isOk())
          // 성공하면 새로 고침(스크롤 위치 유지)
          .andExpect(header().string("HX-Refresh", "true"));
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 1건")))
          .andExpect(content().string(Matchers.containsString("이름")))
          .andExpect(content().string(Matchers.containsString("2026-11-17T10:00")));

      // 메뉴 검색 설정: "이름" 포함 검색칸 + 통합 검색(대소문자 무시). 쿼리를 고치지 않고 WHERE 에 붙는다
      searchService.save(
          menuId, Map.of("이름", SearchOperator.CONTAINS), java.util.List.of("이름", "createdDate"));
      assertThat(
              searchService.setup(menuService.getMenu(menuId)).candidates().stream()
                  .filter(c -> c.label().equals("createdDate"))
                  .findFirst()
                  .orElseThrow()
                  .keywordOn())
          .as("날짜 컬럼은 통합 검색에 넣을 수 없다")
          .isFalse();
      var setup = searchService.setup(menuService.getMenu(menuId));
      assertThat(
              setup.candidates().stream()
                  .filter(c -> c.label().equals("id"))
                  .findFirst()
                  .orElseThrow()
                  .operators()
                  .get(0))
          .as("PK(uuid)는 같음이 기본")
          .isEqualTo(SearchOperator.EQUALS);
      assertThat(setup.candidates().get(0).label()).as("고른 검색칸이 앞").isEqualTo("이름");
      mvc.perform(get(base).param("f.이름", rowId.toString().substring(0, 13)).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("통합 검색")))
          .andExpect(content().string(Matchers.containsString("전체 1건")));
      mvc.perform(get(base).param("q", name.toUpperCase()).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 1건")));
      mvc.perform(get(base).param("q", "%").param("f.이름", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 0건")));

      // 수정 폼: 선택한 행 값으로 채운다(별칭 "이름" → 파라미터 name). 행 JSON 은 목록이 서명해 내보낸다
      var plainRowJson = "{\"id\":\"" + rowId + "\",\"이름\":\"" + name + "\"}";
      var rowJson = rowSigner.sign(menuId, plainRowJson);
      // 서명 없는(화면에서 고친) 행은 폼 · 실행 모두 거부
      mvc.perform(
              post(base + "/action/" + update + "/form")
                  .param("row", plainRowJson)
                  .with(normalUser()))
          .andExpect(status().isBadRequest());
      mvc.perform(
              post(base + "/action/" + update + "/run")
                  .param("rows", plainRowJson)
                  .param("v.name", name + "-forged")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("선택한 행 정보가 올바르지 않습니다")));
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
          .andExpect(header().string("HX-Refresh", "true"));
      // 수정 불가(unique 인 code): 폼은 읽기 전용, 받은 값은 쓰지 않는다. 조회 결과에 원래 값이 없으면 거부
      queryService.save(
          menuId,
          null,
          QueryKind.UPDATE,
          "주소 수정",
          "UPDATE \"Project\" SET code = :code WHERE id = :id",
          null,
          null);
      var codeUpdate =
          menuService.getMenu(menuId).getQueries().stream()
              .filter(q -> "주소 수정".equals(q.getName()))
              .findFirst()
              .orElseThrow()
              .getId();
      mvc.perform(
              post(base + "/action/" + codeUpdate + "/form")
                  .param("row", rowJson)
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("수정할 수 없는 값입니다")))
          .andExpect(content().string(Matchers.containsString("readonly")));
      mvc.perform(
              post(base + "/action/" + codeUpdate + "/run")
                  .param("rows", rowJson)
                  .param("v.code", "changed-by-form")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("원래 값을 알 수 없습니다")));
      assertThat(projectRepository.findById(rowId).orElseThrow().getCode())
          .isEqualTo("r" + rowId.toString().substring(0, 8));

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
          .andExpect(header().string("HX-Refresh", "true"));
      assertThat(projectRepository.findById(rowId)).isEmpty();
      assertThat(changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId))
          .hasSize(3)
          .allSatisfy(log -> assertThat(log.getUsername()).isEqualTo("tester"));
      // 변경 이력 화면(관리): 세 건, 넣은 값이 보이고, 작업 · 사용자로 거른다
      var admin = user("admin").roles("ADMIN");
      var history = "/admin/menu/" + menuId + "/history";
      mvc.perform(get(history).with(admin))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("이력 3건")))
          .andExpect(content().string(Matchers.containsString(name)));
      // 전후 비교: 수정은 이전 이름(지움) → 새 이름, 삭제는 지운 행 값
      mvc.perform(get(history).param("query", update.toString()).with(admin))
          .andExpect(content().string(Matchers.containsString("이력 1건")))
          .andExpect(content().string(Matchers.containsString(name + "</del>")))
          .andExpect(content().string(Matchers.containsString(name + "-2</ins>")));
      mvc.perform(get(history).param("query", delete.toString()).with(admin))
          .andExpect(content().string(Matchers.containsString("이력 1건")))
          .andExpect(content().string(Matchers.containsString("지운 값 </span>")));
      mvc.perform(get(history).param("user", "nobody").with(admin))
          .andExpect(content().string(Matchers.containsString("이력 0건")));
      mvc.perform(get(history).param("from", "2000-01-01").param("to", "2000-01-02").with(admin))
          .andExpect(content().string(Matchers.containsString("이력 0건")));
      // 관리 화면이라 USER 는 못 본다
      mvc.perform(get(history).with(normalUser())).andExpect(status().isForbidden());

      // 메뉴를 ADMIN 전용으로 바꾸면 USER 는 403, 목록에서도 빠진다
      menuService.updateMenu(menuId, "list", "프로젝트 관리", "ROLE_ADMIN", 20, true, true);
      mvc.perform(get(base).with(normalUser())).andExpect(status().isForbidden());
      mvc.perform(get("/").with(normalUser()))
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

  private static UUID idOf(net.luversof.web.dynamiccrud2.domain.Menu menu, QueryKind kind) {
    return menu.getQueries().stream()
        .filter(q -> q.getKind() == kind)
        .findFirst()
        .orElseThrow()
        .getId();
  }
}
