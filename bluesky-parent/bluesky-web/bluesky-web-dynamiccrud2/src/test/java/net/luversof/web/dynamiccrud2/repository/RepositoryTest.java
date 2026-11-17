package net.luversof.web.dynamiccrud2.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/** 설정 catalog 왕복 확인. 자기가 만든 행만 지운다. localdev 프로파일 + 스키마 적용된 dynamiccrud2 catalog 필요. */
class RepositoryTest implements GeneralWebTest {

  @Autowired DbTableRepository dbTableRepository;

  @Autowired MenuRepository menuRepository;

  @Test
  void dbTableWithFieldsRoundTrip() {
    var dbTable = new DbTable();
    dbTable.setConnectionName("test-" + UUID.randomUUID());
    dbTable.setTableName("article");
    var field = new DbField();
    field.setColumnName("title");
    field.setInputType(InputType.TEXT);
    field.setMaxLength(200);
    field.setSortable(true);
    dbTable.getFields().add(field);

    var saved = dbTableRepository.save(dbTable);
    try {
      var found = dbTableRepository.findById(saved.getId()).orElseThrow();
      assertThat(found.getCreatedDate()).isNotNull();
      assertThat(found.getFields())
          .singleElement()
          .satisfies(f -> {
            assertThat(f.getColumnName()).isEqualTo("title");
            assertThat(f.getInputType()).isEqualTo(InputType.TEXT);
            assertThat(f.getMaxLength()).isEqualTo(200);
            assertThat(f.isSortable()).isTrue();
          });
    } finally {
      dbTableRepository.deleteById(saved.getId());
    }
  }

  @Test
  void menuWithQueriesRoundTripKeepsQueryIdAndJsonb() {
    var menu = new Menu();
    menu.setMainMenuId(UUID.randomUUID());
    menu.setDbTableId(UUID.randomUUID());
    menu.setCode("test-menu");
    menu.setName("test-menu");
    var read = new MenuQuery();
    read.setKind(QueryKind.READ);
    read.setName("조회");
    read.setQueryText("SELECT * FROM article WHERE status = :status");
    read.setParamSettings(Map.of("status", Map.of("role", "SEARCH", "required", false)));
    menu.getQueries().add(read);

    var saved = menuRepository.save(menu);
    try {
      var queryId = saved.getQueries().iterator().next().getId();
      assertThat(queryId).isNotNull();

      // 묶음 재저장(하위 삭제 후 재삽입)에도 MenuQuery id 가 그대로여야 ChangeLog 참조가 안 깨진다
      saved.setName("test-menu-2");
      menuRepository.save(saved);

      var found = menuRepository.findById(saved.getId()).orElseThrow();
      assertThat(found.getName()).isEqualTo("test-menu-2");
      assertThat(found.getQueries())
          .singleElement()
          .satisfies(q -> {
            assertThat(q.getId()).isEqualTo(queryId);
            assertThat(q.getKind()).isEqualTo(QueryKind.READ);
            assertThat(q.getParamSettings())
                .containsEntry("status", Map.of("role", "SEARCH", "required", false));
          });
    } finally {
      menuRepository.deleteById(saved.getId());
    }
  }
}
