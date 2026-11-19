package net.luversof.web.dynamiccrud2.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.ParamSetting.Role;
import net.luversof.web.dynamiccrud2.domain.QueryKind;
import net.luversof.web.dynamiccrud2.repository.DbTableRepository;

/**
 * 메뉴 등록 → 작업 추가 → 커스텀 조회 저장 흐름. 설정 catalog(dynamiccrud2) 자체 테이블을 대상으로 쓰고, 이 테스트가 만든 행만 지운다.
 * localdev 프로파일 필요.
 */
class AdminServiceTest implements GeneralWebTest {

  @Autowired MenuService menuService;
  @Autowired QueryService queryService;
  @Autowired FieldService fieldService;
  @Autowired DatabaseAdapterResolver resolver;
  @Autowired DbTableRepository dbTableRepository;

  private TableRef table(String name) {
    return resolver.resolve("dynamiccrud2").listTables().stream()
        .filter(t -> t.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private UUID existingTableId(String name) {
    var ref = table(name);
    return dbTableRepository
        .findIdByLocation("dynamiccrud2", ref.catalog(), ref.schema(), ref.name())
        .orElse(null);
  }

  /** 공유 테이블에 등록 뒤 늘어난 컬럼: 메뉴를 새로 만들거나 작업을 저장하면 필드로 들어온다(빠지면 값이 문자열로 바인딩돼 실패) */
  @Test
  void sharedTablePicksUpNewColumns() {
    var preexisting = existingTableId("MainMenu");
    var project =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "svc-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    var menuIds = new ArrayList<UUID>();
    try {
      var first =
          menuService.createMenu(mainMenu.getId(), "a", "a", "dynamiccrud2", table("MainMenu"));
      menuIds.add(first.getId());
      // DB 에 컬럼이 늘었는데 필드는 예전 그대로인 상태를 흉내 낸다
      dropField(first.getDbTableId(), "enableDisplay");
      var second =
          menuService.createMenu(mainMenu.getId(), "b", "b", "dynamiccrud2", table("MainMenu"));
      menuIds.add(second.getId());
      assertThat(fieldNames(second.getDbTableId())).contains("enableDisplay");

      dropField(first.getDbTableId(), "enableDisplay");
      queryService.save(
          first.getId(),
          null,
          QueryKind.INSERT,
          null,
          queryService.draft(menuService.getMenu(first.getId()), QueryKind.INSERT).queryText(),
          null,
          null);
      assertThat(fieldNames(first.getDbTableId())).contains("enableDisplay");
    } finally {
      menuIds.forEach(menuService::deleteMenu);
      var id = existingTableId("MainMenu");
      if (id != null && preexisting == null) {
        dbTableRepository.deleteById(id);
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
    }
  }

  /** 메뉴 복사(쿼리 · 설정 그대로, 숨김, 쿼리 id 새로) + 조회 쿼리 검사의 결과 미리 보기 */
  @Test
  void copyMenuAndPreviewRead() {
    var preexisting = existingTableId("MainMenu");
    var project =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "svc-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    var menuIds = new ArrayList<UUID>();
    try {
      var source =
          menuService.createMenu(mainMenu.getId(), "src", "src", "dynamiccrud2", table("MainMenu"));
      menuIds.add(source.getId());
      source =
          queryService.save(
              source.getId(),
              null,
              QueryKind.UPDATE,
              "이름만 수정",
              "UPDATE public.\"MainMenu\" SET name = :name WHERE id = :id",
              null,
              "ROLE_ADMIN");

      var copy = menuService.copyMenu(source.getId(), "src-copy", "복사본");
      menuIds.add(copy.getId());
      assertThat(copy.isEnableDisplay()).isFalse();
      assertThat(copy.getDbTableId()).isEqualTo(source.getDbTableId());
      assertThat(copy.getDisplayOrder()).isGreaterThan(source.getDisplayOrder());
      assertThat(copy.getQueries())
          .extracting(q -> q.getKind() + " " + q.getName() + " " + q.getAuthority())
          .containsExactlyInAnyOrderElementsOf(
              source.getQueries().stream()
                  .map(q -> q.getKind() + " " + q.getName() + " " + q.getAuthority())
                  .toList());
      var sourceQueryIds = source.getQueries().stream().map(q -> q.getId()).toList();
      assertThat(copy.getQueries()).noneMatch(q -> sourceQueryIds.contains(q.getId()));
      // 원래 메뉴는 그대로
      assertThat(menuService.getMenu(source.getId()).getQueries()).hasSize(2);
      var sourceId = source.getId();
      assertThatThrownBy(() -> menuService.copyMenu(sourceId, "src", "겹침"))
          .isInstanceOf(BlueskyException.class);

      // 미리 보기: 방금 만든 메인 메뉴 행이 있어 비지 않고, 최대 PREVIEW_ROWS 행
      var read =
          copy.getQueries().stream()
              .filter(q -> q.getKind() == QueryKind.READ)
              .findFirst()
              .orElseThrow();
      var inspection = queryService.inspect(copy, QueryKind.READ, read.getQueryText(), null);
      assertThat(inspection.error()).isNull();
      assertThat(inspection.preview()).isNotNull();
      assertThat(inspection.preview().labels()).contains("id", "name");
      // 결과 컬럼마다 필드 설정 연결(제목 · 형식 · 필드 관리 링크)
      assertThat(inspection.views())
          .filteredOn(v -> v.label().equals("name"))
          .singleElement()
          .satisfies(v -> {
            assertThat(v.tableId()).isEqualTo(copy.getDbTableId());
            assertThat(v.tableName()).isEqualTo("MainMenu");
          });
      assertThat(inspection.fieldTables())
          .singleElement()
          .satisfies(ft -> assertThat(ft.id()).isEqualTo(copy.getDbTableId()));
      // 미리 보기는 건수를 세지 않는다(큰 테이블 COUNT 회피)
      assertThat(inspection.preview().total()).isEqualTo(-1);
      assertThat(inspection.preview().rows())
          .isNotEmpty()
          .hasSizeLessThanOrEqualTo(QueryService.PREVIEW_ROWS);
      // 입력 · 수정 · 삭제는 실행하지 않으므로 미리 보기 없음
      assertThat(
              queryService
                  .inspect(
                      copy,
                      QueryKind.UPDATE,
                      "UPDATE public.\"MainMenu\" SET name = :name WHERE id = :id",
                      null)
                  .preview())
          .isNull();
    } finally {
      menuIds.forEach(menuService::deleteMenu);
      var id = existingTableId("MainMenu");
      if (id != null && preexisting == null) {
        dbTableRepository.deleteById(id);
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
    }
  }

  private void dropField(UUID dbTableId, String columnName) {
    var dbTable = fieldService.get(dbTableId);
    dbTable.getFields().removeIf(f -> f.getColumnName().equals(columnName));
    dbTableRepository.save(dbTable);
    assertThat(fieldNames(dbTableId)).doesNotContain(columnName);
  }

  private List<String> fieldNames(UUID dbTableId) {
    return fieldService.get(dbTableId).getFields().stream().map(DbField::getColumnName).toList();
  }

  @Test
  void registerMenuThenAddActionsAndCustomRead() {
    // 이미 등록돼 있던 공유 테이블은 지우지 않는다
    var preexisting = new ArrayList<UUID>();
    for (var name : List.of("MainMenu", "Project")) {
      var id = existingTableId(name);
      if (id != null) {
        preexisting.add(id);
      }
    }
    var project =
        menuService.saveProject(
            null,
            "t" + UUID.randomUUID().toString().substring(0, 8),
            "svc-test-" + UUID.randomUUID());
    var mainMenu = menuService.saveMainMenu(null, project.getId(), "main", "main");
    UUID menuId = null;
    try {
      // 1차 등록: 필드는 테이블 단위, 쿼리는 조회만
      var menu =
          menuService.createMenu(
              mainMenu.getId(), "list", "메인 메뉴 관리", "dynamiccrud2", table("MainMenu"));
      menuId = menu.getId();
      assertThat(menu.getQueries())
          .singleElement()
          .satisfies(q -> {
            assertThat(q.getKind()).isEqualTo(QueryKind.READ);
            assertThat(q.getQueryText())
                .contains("FROM public.\"MainMenu\"")
                .endsWith("ORDER BY id");
          });
      var fields = fieldService.get(menu.getDbTableId()).getFields();
      assertThat(fields).extracting(DbField::getColumnName).contains("id", "project_id", "name");
      assertThat(fields)
          .filteredOn(DbField::isSortable)
          .extracting(DbField::getColumnName)
          .contains("id", "project_id")
          .doesNotContain("name");

      // 작업 추가: 그 시점에 기본 쿼리 생성, 행 키는 PK 로 추정
      var draft = queryService.draft(menu, QueryKind.UPDATE);
      assertThat(draft.error()).isNull();
      assertThat(draft.queryText())
          .startsWith("UPDATE public.\"MainMenu\" SET")
          .endsWith("WHERE id = :id");
      var inspection = queryService.inspect(menu, QueryKind.UPDATE, draft.queryText(), null);
      assertThat(inspection.error()).isNull();
      assertThat(inspection.params())
          .filteredOn(p -> p.name().equals("id"))
          .singleElement()
          .satisfies(p -> assertThat(p.setting().role()).isEqualTo(Role.ROW_KEY));
      assertThat(inspection.params())
          .filteredOn(p -> p.name().equals("name"))
          .singleElement()
          .satisfies(p -> assertThat(p.setting().role()).isEqualTo(Role.INPUT));
      menu =
          queryService.save(
              menuId,
              null,
              QueryKind.UPDATE,
              "이름만 수정",
              "UPDATE public.\"MainMenu\" SET name = :name WHERE id = :id",
              null,
              "ROLE_ADMIN");
      assertThat(menu.getQueries()).hasSize(2);

      // 커스텀 조회: OR 안의 :pname 은 선택으로 바꿀 수 없다
      var read =
          """
          SELECT m.id, m.name AS "메뉴", p.name AS project_name, UPPER(m.name) AS upper_name
          FROM "MainMenu" m JOIN "Project" p ON p.id = m.project_id
          WHERE m.name = :name AND (p.name = :pname OR m.id IS NULL)
          """;
      var finalMenuId = menuId;
      assertThatThrownBy(() ->
              queryService.save(
                  finalMenuId,
                  null,
                  QueryKind.READ,
                  null,
                  read,
                  Map.of("pname", new ParamSetting(Role.SEARCH, false, null)),
                  null))
          .isInstanceOf(BlueskyException.class)
          .extracting("errorCode")
          .isEqualTo("QueryErrorCode.NOT_OPTIONAL_CAPABLE");

      var readInspection = queryService.inspect(menu, QueryKind.READ, read, null);
      assertThat(readInspection.warnings()).anyMatch(w -> w.startsWith("name: 인덱스"));
      menu =
          queryService.save(
              menuId,
              null,
              QueryKind.READ,
              null,
              read,
              Map.of("name", new ParamSetting(Role.SEARCH, false, null)),
              null);
      // 조회는 하나만: 새로 만들지 않고 고쳤다
      assertThat(menu.getQueries()).hasSize(2);
      var saved =
          menu.getQueries().stream()
              .filter(q -> q.getKind() == QueryKind.READ)
              .findFirst()
              .orElseThrow();
      assertThat(QueryService.settingsOf(saved).get("name").required()).isFalse();
      assertThat(QueryService.settingsOf(saved).get("pname").required()).isTrue();

      // 조인한 Project.name 은 Project 테이블 필드로, 계산 컬럼은 메뉴 테이블 가상 필드로
      assertThat(fieldService.get(existingTableId("Project")).getFields())
          .extracting(DbField::getColumnName)
          .contains("name");
      assertThat(fieldService.get(menu.getDbTableId()).getFields())
          .filteredOn(DbField::isVirtual)
          .extracting(DbField::getColumnName)
          .containsExactly("upper_name");

      var update =
          menu.getQueries().stream()
              .filter(q -> q.getKind() == QueryKind.UPDATE)
              .findFirst()
              .orElseThrow();
      assertThatThrownBy(() -> queryService.delete(finalMenuId, saved.getId()))
          .isInstanceOf(BlueskyException.class);
      assertThat(queryService.delete(menuId, update.getId()).getQueries()).hasSize(1);
    } finally {
      if (menuId != null) {
        menuService.deleteMenu(menuId);
        for (var name : List.of("MainMenu", "Project")) {
          var id = existingTableId(name);
          if (id != null && !preexisting.contains(id)) {
            dbTableRepository.deleteById(id);
          }
        }
      }
      menuService.deleteMainMenu(mainMenu.getId());
      menuService.deleteProject(project.getId());
    }
  }
}
