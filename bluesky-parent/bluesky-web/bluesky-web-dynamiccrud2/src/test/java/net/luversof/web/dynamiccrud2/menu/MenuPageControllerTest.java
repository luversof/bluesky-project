package net.luversof.web.dynamiccrud2.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
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
  @Autowired net.luversof.web.dynamiccrud2.repository.SettingLogRepository settingLogRepository;
  @Autowired RowSigner rowSigner;
  @Autowired FieldService fieldService;

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
    var apiRowId = UUID.randomUUID();
    var name = "row-" + rowId;
    UUID menuId = null;
    String originalOptions = null;
    var optionsChanged = false;
    var startedAt = java.time.Instant.now();
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
      // 상단 프로젝트 고르기: 볼 수 있는 프로젝트가 둘 이상이면 링크 목록(지금 프로젝트는 aria-current), 하나면 이름만
      var visibleProjects =
          org.apache.commons.lang3.StringUtils.countMatches(
              mvc.perform(get("/").with(normalUser()))
                  .andReturn()
                  .getResponse()
                  .getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
              "<section class=\"card\">");
      var usePage =
          mvc.perform(get(base).with(normalUser()))
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      if (visibleProjects > 1) {
        assertThat(usePage)
            .contains("data-dropdown")
            .contains("<a href=\"/" + owner.getCode() + "\" aria-current=\"page\">");
      } else {
        assertThat(usePage).doesNotContain("data-dropdown");
      }
      // 가이드: 상단 오른쪽 링크, 로그인한 사용자 누구나
      mvc.perform(get("/guide").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("<section id=\"search\">")))
          .andExpect(
              content()
                  .string(
                      Matchers.containsString(
                          "href=\"/guide\" class=\"app-nav-link\" aria-current=\"page\"")));
      // 프로젝트 · 메인 메뉴 주소까지만 와도 첫 메뉴로, 없는 메인 메뉴는 404
      mvc.perform(get("/" + owner.getCode() + "/main").with(normalUser()))
          .andExpect(status().is3xxRedirection())
          .andExpect(header().string("Location", base));
      mvc.perform(get("/" + owner.getCode()).with(normalUser()))
          .andExpect(header().string("Location", base));
      mvc.perform(get("/" + owner.getCode() + "/nope").with(normalUser()))
          .andExpect(status().isNotFound());
      // 메인 메뉴 · 프로젝트를 숨기면 전체 메뉴에서 아래 메뉴까지 빠지고, 메뉴 주소 · 엑셀도 404(숨기면 아래도 숨긴 것)
      menuService.setMainMenuDisplay(mainMenu.getId(), false);
      mvc.perform(get("/").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      mvc.perform(get(base).with(normalUser())).andExpect(status().isNotFound());
      menuService.setMainMenuDisplay(mainMenu.getId(), true);
      menuService.setProjectDisplay(owner.getId(), false);
      mvc.perform(get("/").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      mvc.perform(get(base).with(normalUser())).andExpect(status().isNotFound());
      mvc.perform(get(base + "/excel").with(normalUser())).andExpect(status().isNotFound());
      menuService.setProjectDisplay(owner.getId(), true);
      // 메뉴 자체를 숨겨도 주소 · JSON 호출 · 엑셀 모두 404(프로젝트 · 메인 메뉴와 같다, DMT2 UDP-6558 11-26 - 관리자도)
      menuService.setMenuDisplay(menuId, false);
      mvc.perform(get(base).with(normalUser())).andExpect(status().isNotFound());
      mvc.perform(
              get(base + "/api")
                  .with(normalUser())
                  .accept(org.springframework.http.MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
      mvc.perform(get(base + "/excel").with(user("master").roles("MASTER")))
          .andExpect(status().isNotFound());
      menuService.setMenuDisplay(menuId, true);
      // 목록 표시 여부(DMT2 80c8003 · b78cadd): 끈 칸은 그려 두되 hidden - 행 자세히가 읽는다. 관리 화면은 체크가 꺼진 채로
      menuService.setListHidden(menuId, java.util.List.of("displayOrder"));
      var hiddenPage =
          mvc.perform(get(base).with(normalUser()))
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      assertThat(
              java.util.regex.Pattern.compile("<th [^>]*\\shidden[\\s>]")
                  .matcher(hiddenPage)
                  .results()
                  .count())
          .isEqualTo(1);
      mvc.perform(get("/admin/menu/" + menuId).with(user("master").roles("MASTER")))
          .andExpect(
              content().string(Matchers.containsString("name=\"show\" value=\"displayOrder\" />")));
      mvc.perform(
              post("/admin/menu/" + menuId + "/list-hidden")
                  .param("all", "displayOrder", "name")
                  .param("show", "displayOrder", "name")
                  .with(user("master").roles("MASTER")))
          .andExpect(status().is3xxRedirection());
      assertThat(menuService.getMenu(menuId).getListHidden()).isNull();
      // 기본 쿼리 다시 만들기(DMT2 90c5952): 입력칸 조각만(저장하지 않음), 종류마다 그 기본 쿼리
      for (var kind : QueryKind.values()) {
        mvc.perform(
                post("/admin/menu/" + menuId + "/query/draft")
                    .param("kind", kind.name())
                    .param("queryText", "old text")
                    .with(user("master").roles("MASTER")))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("id=\"q-queryText\"")))
            .andExpect(content().string(Matchers.not(Matchers.containsString("old text"))))
            .andExpect(
                content()
                    .string(
                        Matchers.containsString(kind == QueryKind.READ ? "SELECT" : kind.name())));
      }
      assertThat(menuService.getMenu(menuId).getQueries()).hasSize(saved.getQueries().size());
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 0건")))
          // 같은 메인 메뉴의 왼쪽 메뉴 링크는 지금 검색 조건을 넘긴다(DMT2 ed3997b)
          .andExpect(
              content()
                  .string(Matchers.containsString("href=\"" + base + "?s.name=" + name + "\"")));

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
          .andExpect(content().string(Matchers.containsString("2026-11-17T10:00")))
          // 숫자 열(displayOrder)은 제목도 칸처럼 오른쪽
          .andExpect(content().string(Matchers.containsString("<th class=\"text-right\">")));

      // 값 목록: 목록 칸은 표시 글자(원래 값은 title), 입력 폼은 그 목록의 셀렉트
      originalOptions = setValueOptions(menuId, "3=셋째\n5=다섯째");
      optionsChanged = true;
      mvc.perform(get(base).param("s.name", name).with(normalUser()))
          .andExpect(content().string(Matchers.containsString("title=\"3\">셋째</td>")))
          // 값 목록이 있으면 칸이 글자라 제목도 왼쪽
          .andExpect(
              content().string(Matchers.not(Matchers.containsString("<th class=\"text-right\">"))));
      mvc.perform(post(base + "/action/" + insert + "/form").with(normalUser()))
          .andExpect(content().string(Matchers.containsString("<option value=\"5\">다섯째</option>")));

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
      // 고르기 "DB 값 전체": 검색칸이 그 컬럼에 있는 값의 셀렉트가 되고 같음으로 찾는다
      searchService.saveFields(
          menuId,
          Map.of(
              "이름",
              new net.luversof.web.dynamiccrud2.admin.service.SearchService.FieldInput(
                  SearchOperator.CONTAINS,
                  net.luversof.web.dynamiccrud2.domain.SearchSettings.Pick.DISTINCT,
                  null,
                  false)),
          java.util.List.of("이름", "createdDate"));
      mvc.perform(get(base).param("f.이름", name).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .string(
                      Matchers.containsString(
                          "<option value=\"" + name + "\" selected>" + name + "</option>")))
          .andExpect(content().string(Matchers.containsString("전체 1건")));
      // 일부만 넣으면 같음이라 맞는 행이 없다
      mvc.perform(get(base).param("f.이름", name.substring(0, 10)).with(normalUser()))
          .andExpect(content().string(Matchers.containsString("전체 0건")));
      // 검색용 값 목록 + 필수: 적은 값만 고르고, 비우면 목록을 읽지 않고 안내만(화면 · JSON)
      searchService.saveFields(
          menuId,
          Map.of(
              "이름",
              new net.luversof.web.dynamiccrud2.admin.service.SearchService.FieldInput(
                  SearchOperator.CONTAINS,
                  net.luversof.web.dynamiccrud2.domain.SearchSettings.Pick.CUSTOM,
                  name + "=찾는 행",
                  true)),
          java.util.List.of());
      mvc.perform(get(base).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("필수 검색 조건을 입력하세요")))
          .andExpect(content().string(Matchers.containsString("전체 0건")))
          .andExpect(content().string(Matchers.containsString(">찾는 행</option>")));
      mvc.perform(
              get(base + "/api")
                  .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                  .with(normalUser()))
          .andExpect(jsonPath("$.total").value(0))
          .andExpect(jsonPath("$.message").value(Matchers.containsString("필수 검색 조건")));
      mvc.perform(get(base).param("f.이름", name).with(normalUser()))
          .andExpect(content().string(Matchers.containsString("전체 1건")));
      searchService.save(
          menuId, Map.of("이름", SearchOperator.CONTAINS), java.util.List.of("이름", "createdDate"));

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
      // 되돌리기(관리 → 변경 이력): 수정은 바뀌기 전 값으로 다시 수정, 한 번만. 되돌린 뒤 다시 같은 수정을 실행해 아래 흐름을 잇는다
      var updateLog = changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId).get(0);
      var revertUrl = "/admin/menu/" + menuId + "/history/" + updateLog.getId() + "/revert";
      mvc.perform(post(revertUrl).with(user("admin").roles("ADMIN")))
          .andExpect(flash().attribute("notice", Matchers.containsString("1행")));
      assertThat(projectRepository.findById(rowId).orElseThrow().getName()).isEqualTo(name);
      mvc.perform(post(revertUrl).with(user("admin").roles("ADMIN")))
          .andExpect(flash().attribute("error", Matchers.containsString("이미 되돌린")));
      mvc.perform(
              post(base + "/action/" + update + "/run")
                  .param("rows", rowJson)
                  .param("v.name", name + "-2")
                  .with(normalUser()))
          .andExpect(header().string("HX-Refresh", "true"));
      // 그 사이 다시 바뀐 행은 되돌리지 않는다(지금 값 name-2 ≠ 되돌리기 이력이 넣은 값 name)
      var revertLog = changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId).get(1);
      assertThat(revertLog.getBindValues()).containsKey("revertOf");
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

      // 이름을 바꾸기 전에 연 목록의 행(rowJson)으로는 지우지 못한다(동시 수정), 다시 연 목록의 행으로는 지운다
      mvc.perform(
              post(base + "/action/" + delete + "/run").param("rows", rowJson).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("다른 사람이 이 행을 바꿨습니다")));
      assertThat(projectRepository.findById(rowId)).isPresent();
      var freshRowJson =
          rowSigner.sign(menuId, "{\"id\":\"" + rowId + "\",\"이름\":\"" + name + "-2\"}");
      // 없는 작업은 404
      mvc.perform(
              post(base + "/action/" + UUID.randomUUID() + "/run")
                  .param("rows", freshRowJson)
                  .with(normalUser()))
          .andExpect(status().isNotFound());
      mvc.perform(
              post(base + "/action/" + delete + "/run")
                  .param("rows", freshRowJson)
                  .with(normalUser()))
          .andExpect(header().string("HX-Refresh", "true"));
      assertThat(projectRepository.findById(rowId)).isEmpty();
      // 입력 · 수정 · (되돌리기) · 수정 · 삭제
      assertThat(changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId))
          .hasSize(5)
          .filteredOn(log -> !log.getBindValues().containsKey("revertOf"))
          .allSatisfy(log -> assertThat(log.getUsername()).isEqualTo("tester"));
      // 두 번째 수정은 그 뒤 행이 지워져 되돌리지 못한다(최근 순: 삭제 · 두 번째 수정 · 되돌리기 · 첫 수정 · 입력)
      // 같은 시각이면 순서가 바뀔 수 있어 작업 id · 되돌리기 여부로 찾는다(DMT2 ed3997b)
      var logs = changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId);
      var secondUpdate =
          logs.stream()
              .filter(log ->
                  log.getMenuQueryId().equals(update)
                      && !log.getBindValues().containsKey("revertOf"))
              .filter(log -> !log.getId().equals(updateLog.getId()))
              .findFirst()
              .orElseThrow();
      mvc.perform(
              post("/admin/menu/" + menuId + "/history/" + secondUpdate.getId() + "/revert")
                  .with(user("admin").roles("ADMIN")))
          .andExpect(flash().attribute("error", Matchers.notNullValue()));
      // 삭제 되돌리기: 지운 행 중 목록에 있던 컬럼만 다시 넣는다 - 이 목록 행에는 code(NOT NULL)가 없어 DB 가 거부하고 사유가 보인다
      var deleteLog =
          logs.stream()
              .filter(log -> log.getMenuQueryId().equals(delete))
              .findFirst()
              .orElseThrow();
      mvc.perform(
              post("/admin/menu/" + menuId + "/history/" + deleteLog.getId() + "/revert")
                  .with(user("admin").roles("ADMIN")))
          .andExpect(flash().attribute("error", Matchers.containsString("실행 실패")));
      assertThat(projectRepository.findById(rowId)).isEmpty();
      // 변경 이력 화면(관리): 세 건, 넣은 값이 보이고, 작업 · 사용자로 거른다
      var admin = user("admin").roles("ADMIN");
      // 기본 탭은 메뉴 설정 변경 - 데이터 변경은 tab=data
      var history = "/admin/menu/" + menuId + "/history?tab=data";
      mvc.perform(get(history).with(admin))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("이력 5건")))
          .andExpect(content().string(Matchers.containsString(name)));
      // 전후 비교: 수정은 이전 이름(지움) → 새 이름, 삭제는 지운 행 값
      mvc.perform(get(history).param("query", update.toString()).with(admin))
          .andExpect(content().string(Matchers.containsString("이력 3건")))
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

      // JSON 호출: 화면과 같은 처리 · 검사, 응답만 JSON. 조회 결과의 row 로 수정 · 삭제한다
      var apiName = "api-" + apiRowId;
      mvc.perform(
              post(base + "/api/action/" + insert)
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content(
                      "{\"inputs\": {\"id\": \""
                          + apiRowId
                          + "\", \"code\": \"a"
                          + apiRowId.toString().substring(0, 8)
                          + "\", \"name\": \""
                          + apiName
                          + "\", \"displayOrder\": \"1\", \"enableDisplay\": \"false\","
                          + " \"createdDate\": \"2026-11-25T09:00:00\"}}")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.affected").value(1));
      var listed =
          mvc.perform(
                  get(base + "/api")
                      .param("s.name", apiName)
                      .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                      .with(normalUser()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.total").value(1))
              .andExpect(jsonPath("$.columns[1].label").value("이름"))
              .andExpect(jsonPath("$.rows[0].values[1]").value(apiName))
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      String apiRow = com.jayway.jsonpath.JsonPath.read(listed, "$.rows[0].row");
      // 서명 없는 행은 400 과 사유
      mvc.perform(
              post(base + "/api/action/" + delete)
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content("{\"rows\": [\"{\\\"id\\\":\\\"" + apiRowId + "\\\"}\"]}")
                  .with(normalUser()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value(Matchers.containsString("선택한 행 정보가 올바르지 않습니다")));
      mvc.perform(
              post(base + "/api/action/" + delete)
                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                  .content(
                      "{\"rows\": ["
                          + "\""
                          + apiRow.replace("\\", "\\\\").replace("\"", "\\\"")
                          + "\""
                          + "]}")
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.affected").value(1));
      assertThat(projectRepository.findById(apiRowId)).isEmpty();
      // 메뉴 관리 화면 아래 호출 설명: 조회 주소 · 작업 주소 · 파라미터
      mvc.perform(get("/admin/menu/" + menuId).with(admin))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("JSON 호출")))
          // 메뉴 관리 안내: 처음엔 접혀 있고 접고 편 상태를 기억한다
          .andExpect(
              content()
                  .string(
                      Matchers.containsString(
                          "<details class=\"guide mb-4\" data-remember-open=\"menu-guide\">")))
          // 제목 오른쪽 사용 화면 주소 링크
          .andExpect(
              content()
                  .string(
                      Matchers.containsString(
                          "<a class=\"hint font-mono hover:underline\" href=\"" + base + "\"")))
          .andExpect(content().string(Matchers.containsString(base + "/api")))
          .andExpect(content().string(Matchers.containsString(base + "/api/action/" + update)))
          .andExpect(content().string(Matchers.containsString(">s.name<")));

      // 엑셀로 여러 행 넣기: 양식은 작업 폼 제목, 틀린 행이 있으면 아무것도 넣지 않고 행 번호 · 사유, 다 맞으면 넣는다
      mvc.perform(get(base).with(normalUser()))
          .andExpect(content().string(Matchers.containsString("/action/" + insert + "/bulk-form")));
      mvc.perform(get(base + "/action/" + insert + "/bulk-template").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(
              header()
                  .string("Content-Type", Matchers.startsWith("application/vnd.openxmlformats")));
      mvc.perform(post(base + "/action/" + insert + "/bulk-form").with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("양식 내려받기")));
      mvc.perform(post(base + "/action/" + update + "/bulk-form").with(normalUser()))
          .andExpect(status().isBadRequest());
      var bulkIds = java.util.List.of(UUID.randomUUID(), UUID.randomUUID());
      try {
        var bad =
            xlsx(
                java.util.List.of(
                    "id", "code", "name", "displayOrder", "enableDisplay", "createdDate"),
                java.util.List.of(
                    java.util.List.of(
                        bulkIds.get(0).toString(),
                        "b" + bulkIds.get(0).toString().substring(0, 8),
                        "bulk-1",
                        "1",
                        "true",
                        "2026-11-26T09:00:00"),
                    java.util.List.of(
                        bulkIds.get(1).toString(),
                        "b" + bulkIds.get(1).toString().substring(0, 8),
                        "bulk-2",
                        "숫자아님",
                        "true",
                        "2026-11-26T09:00:00")));
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                        base + "/action/" + insert + "/bulk")
                    .file("file", bad)
                    .with(normalUser()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("틀린 행이 1개")))
            .andExpect(content().string(Matchers.containsString("3행:")));
        assertThat(projectRepository.findById(bulkIds.get(0))).isEmpty();
        var good =
            xlsx(
                java.util.List.of(
                    "id", "code", "name", "displayOrder", "enableDisplay", "createdDate"),
                java.util.List.of(
                    java.util.List.of(
                        bulkIds.get(0).toString(),
                        "b" + bulkIds.get(0).toString().substring(0, 8),
                        "bulk-1",
                        "1",
                        "true",
                        "2026-11-26T09:00:00"),
                    java.util.List.of(
                        bulkIds.get(1).toString(),
                        "b" + bulkIds.get(1).toString().substring(0, 8),
                        "bulk-2",
                        "2",
                        "false",
                        "2026-11-26T09:00:00")));
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                        base + "/action/" + insert + "/bulk")
                    .file("file", good)
                    .with(normalUser()))
            .andExpect(header().string("HX-Refresh", "true"));
        assertThat(projectRepository.findById(bulkIds.get(1)).orElseThrow().getName())
            .isEqualTo("bulk-2");
      } finally {
        bulkIds.forEach(projectRepository::deleteById);
      }

      // 메뉴를 ADMIN 전용으로 바꾸면 USER 는 403, 목록에서도 빠진다
      menuService.updateMenu(menuId, "list", "프로젝트 관리", "ROLE_ADMIN", 20, true, true);
      mvc.perform(get(base).with(normalUser())).andExpect(status().isForbidden());
      mvc.perform(
              get(base + "/api")
                  .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                  .with(normalUser()))
          .andExpect(status().isForbidden());
      mvc.perform(get("/").with(normalUser()))
          .andExpect(content().string(Matchers.not(Matchers.containsString("프로젝트 관리"))));
      mvc.perform(get(base).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    } finally {
      projectRepository.deleteById(rowId);
      projectRepository.deleteById(apiRowId);
      if (menuId != null) {
        // Project 테이블 필드는 다른 메뉴와 같이 쓸 수 있어 값 목록을 원래대로 돌린다
        if (optionsChanged) {
          setValueOptions(menuId, originalOptions);
        }
        changeLogRepository.deleteAll(
            changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId));
        var dbTableId = menuService.getMenu(menuId).getDbTableId();
        // 값 목록을 바꿨다 되돌린 필드 이력(테이블 단위라 프로젝트 정리에 안 걸린다) - 이 시험이 시작한 뒤의 것만
        net.luversof.web.dynamiccrud2.TestSettingLogs.purgeFields(
            settingLogRepository, menuId, dbTableId, startedAt);
        menuService.deleteMenu(menuId);
        if (preexisting.isEmpty()) {
          dbTableRepository.deleteById(dbTableId);
        }
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(owner.getId());
      net.luversof.web.dynamiccrud2.TestSettingLogs.purge(settingLogRepository, owner.getId());
    }
  }

  /** displayOrder 필드의 값 목록만 바꾼다(다른 설정은 그대로), 바꾸기 전 값을 돌려준다 */
  private String setValueOptions(UUID menuId, String valueOptions) {
    var dbTableId = menuService.getMenu(menuId).getDbTableId();
    var field =
        fieldService.get(dbTableId).getFields().stream()
            .filter(f -> f.getColumnName().equals("displayOrder"))
            .findFirst()
            .orElseThrow();
    var before = field.getValueOptions();
    fieldService.updateField(
        dbTableId,
        field.getColumnName(),
        field.getLabel(),
        field.getInputType(),
        field.getMaxLength(),
        field.isSortable(),
        field.isUpdatable(),
        valueOptions,
        field.getDateFormat());
    return before;
  }

  private static UUID idOf(net.luversof.web.dynamiccrud2.domain.Menu menu, QueryKind kind) {
    return menu.getQueries().stream()
        .filter(q -> q.getKind() == kind)
        .findFirst()
        .orElseThrow()
        .getId();
  }

  /** 첫 줄 칸 이름 + 행들(모두 글자 칸)의 xlsx 바이트 */
  private static byte[] xlsx(
      java.util.List<String> header, java.util.List<java.util.List<String>> rows)
      throws java.io.IOException {
    try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        var out = new java.io.ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("data");
      var all = new java.util.ArrayList<java.util.List<String>>();
      all.add(header);
      all.addAll(rows);
      for (int r = 0; r < all.size(); r++) {
        var row = sheet.createRow(r);
        for (int c = 0; c < all.get(r).size(); c++) {
          row.createCell(c).setCellValue(all.get(r).get(c));
        }
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }
}
