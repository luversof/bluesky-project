package net.luversof.web.dynamiccrud2.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClients;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.TestSettingLogs;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.connection.ConnectionNameService;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.menu.service.RowSigner;
import net.luversof.web.dynamiccrud2.repository.ChangeLogRepository;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;
import net.luversof.web.dynamiccrud2.repository.SettingLogRepository;

/**
 * Mongo 메뉴 한 바퀴: 관리(메뉴 등록 · 작업 저장) → 사용 화면 조회 · 검색 · 입력 · 수정 · 삭제. 대상 Mongo 는 버리는 컨테이너(mongo:8.2,
 * bluesky-boot.connection-info.mongodb.connection-map 에 시험 연결로 넣는다), 설정은 localdev 설정
 * DB(GeneralWebTest). Docker 가 없으면 건너뛴다.
 */
class MongoMenuFlowTest implements GeneralWebTest {

  private static final String CONNECTION = "dc2MongoFlowTest";

  private static MongoDBContainer container;

  @Autowired WebApplicationContext context;
  @Autowired MenuService menuService;
  @Autowired QueryService queryService;
  @Autowired ConnectionNameService connectionNameService;
  @Autowired DbTableRepository dbTableRepository;
  @Autowired ChangeLogRepository changeLogRepository;
  @Autowired SettingLogRepository settingLogRepository;
  @Autowired RowSigner rowSigner;

  @BeforeAll
  static void start() {
    Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker");
    container = new MongoDBContainer("mongo:8.2");
    container.start();
    try (var client = MongoClients.create(container.getConnectionString())) {
      client
          .getDatabase("dmt")
          .getCollection("users")
          .insertMany(
              List.of(
                  new Document("name", "alpha").append("age", 20),
                  new Document("name", "beta").append("age", 30),
                  new Document("name", "gamma").append("age", 40)));
    }
  }

  @AfterAll
  static void stop() {
    if (container != null) {
      container.stop();
    }
  }

  @DynamicPropertySource
  static void mongoConnection(DynamicPropertyRegistry registry) {
    // 앱 기본 설정엔 Mongo 연결이 없다 - 시험에서만 connection-map reader · MongoClient loader 를 켠다
    registry.add(
        "bluesky-boot.connection-info.readers.properties-mongoclient.enabled", () -> "true");
    registry.add("bluesky-boot.connection-info.loaders.mongoclient.enabled", () -> "true");
    registry.add(
        "bluesky-boot.connection-info.mongodb.connection-map." + CONNECTION + ".hosts",
        () -> container.getHost() + ":" + container.getFirstMappedPort());
    registry.add(
        "bluesky-boot.connection-info.mongodb.connection-map." + CONNECTION + ".database",
        () -> "dmt");
  }

  private static RequestPostProcessor normalUser() {
    return user("tester").roles("USER");
  }

  @Test
  void registerMongoMenuAndUseIt() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    assertThat(connectionNameService.search("mongoflow")).contains(CONNECTION);
    // 자동완성 후보: 이름 옆 흐린 글자로 읽은 출처(Mongo 는 properties-mongoclient)
    mvc.perform(
            get("/admin/connection/search")
                .param("keyword", "mongoflow")
                .header("HX-Request", "true")
                .with(user("admin").roles("MASTER")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        CONNECTION
                            + " <span class=\"combobox-sub\">properties-mongoclient</span>")));
    var owner =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "mongo-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, owner.getId(), "main", "main");
    UUID menuId = null;
    UUID dbTableId = null;
    try {
      var menu =
          menuService.createMenu(
              mainMenu.getId(), "users", "몽고 사용자", CONNECTION, new TableRef(null, null, "users"));
      menuId = menu.getId();
      dbTableId = menu.getDbTableId();
      queryService.save(
          menuId,
          null,
          QueryKind.READ,
          null,
          "{\"collection\": \"users\", \"operation\": \"find\", \"filter\": {\"name\": :name}, \"sort\": {\"age\": 1}}",
          Map.of("name", new ParamSetting(Role.SEARCH, false, null)),
          null);
      for (var kind : List.of(QueryKind.INSERT, QueryKind.UPDATE, QueryKind.DELETE)) {
        queryService.save(
            menuId,
            null,
            kind,
            kind.name(),
            queryService.draft(menuService.getMenu(menuId), kind).queryText(),
            null,
            null);
      }
      var saved = menuService.getMenu(menuId);
      assertThat(saved.getQueries()).hasSize(4);
      var base = "/" + owner.getCode() + "/main/users";

      // 조회 · 선택 검색 조건
      mvc.perform(get(base).with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("전체 3건")))
          .andExpect(content().string(Matchers.containsString("gamma")));
      mvc.perform(get(base).param("s.name", "beta").with(normalUser()))
          .andExpect(content().string(Matchers.containsString("전체 1건")));

      // 입력: 숫자 칸은 숫자로 들어간다
      mvc.perform(
              post(base + "/action/" + idOf(saved, QueryKind.INSERT) + "/run")
                  .param("v.name", "delta")
                  .param("v.age", "50")
                  .header("HX-Request", "true")
                  .with(normalUser()))
          .andExpect(header().string("HX-Refresh", "true"));
      var delta = findOne("delta");
      assertThat(delta.get("age")).isEqualTo(50);
      var id = delta.getObjectId("_id").toHexString();

      // 수정 · 삭제: 행 키(_id 글자)는 서명한 행 JSON 으로 온다
      var row =
          rowSigner.sign(menuId, "{\"_id\":\"" + id + "\",\"name\":\"delta\",\"age\":\"50\"}");
      mvc.perform(
              post(base + "/action/" + idOf(saved, QueryKind.UPDATE) + "/form")
                  .param("row", row)
                  .with(normalUser()))
          .andExpect(status().isOk())
          .andExpect(content().string(Matchers.containsString("value=\"delta\"")));
      mvc.perform(
              post(base + "/action/" + idOf(saved, QueryKind.UPDATE) + "/run")
                  .param("rows", row)
                  .param("v.name", "delta2")
                  .param("v.age", "51")
                  .with(normalUser()))
          .andExpect(header().string("HX-Refresh", "true"));
      assertThat(findOne("delta2").get("age")).isEqualTo(51);
      var fresh =
          rowSigner.sign(menuId, "{\"_id\":\"" + id + "\",\"name\":\"delta2\",\"age\":\"51\"}");
      mvc.perform(
              post(base + "/action/" + idOf(saved, QueryKind.DELETE) + "/run")
                  .param("rows", fresh)
                  .with(normalUser()))
          .andExpect(header().string("HX-Refresh", "true"));
      assertThat(findOne("delta2")).isNull();
      assertThat(changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId)).hasSize(3);
    } finally {
      if (menuId != null) {
        changeLogRepository.deleteAll(
            changeLogRepository.findByMenuIdOrderByCreatedDateDesc(menuId));
        menuService.deleteMenu(menuId);
      }
      if (dbTableId != null) {
        dbTableRepository.deleteById(dbTableId);
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(owner.getId());
      TestSettingLogs.purge(settingLogRepository, owner.getId());
    }
  }

  private static Document findOne(String name) {
    try (var client = MongoClients.create(container.getConnectionString())) {
      return client
          .getDatabase("dmt")
          .getCollection("users")
          .find(new Document("name", name))
          .first();
    }
  }

  private static UUID idOf(Menu menu, QueryKind kind) {
    return menu.getQueries().stream()
        .filter(q -> q.getKind() == kind)
        .findFirst()
        .orElseThrow()
        .getId();
  }
}
