package net.luversof.web.dynamiccrud2.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.SettingLog;

/** 설정 변경 이력의 바뀐 항목 계산 · 화면용 변환(DB 없이) */
class SettingLogServiceTest {

  @Test
  void diffKeepsOnlyChangedItems() {
    var before = new LinkedHashMap<String, String>();
    before.put("name", "a");
    before.put("code", "x");
    var after = new LinkedHashMap<String, String>();
    after.put("name", "b");
    after.put("code", "x");
    assertThat(SettingLogService.diff(before, after))
        .containsOnlyKeys("name")
        .containsEntry("name", Map.of("before", "a", "after", "b"));
    // 만들기는 이후만, 지우기는 이전만
    assertThat(SettingLogService.diff(null, after).get("code")).containsEntry("after", "x");
    assertThat(SettingLogService.diff(before, null).get("name")).containsEntry("before", "a");
    assertThat(SettingLogService.diff(before, new LinkedHashMap<>(before))).isEmpty();
  }

  /** DB 다시 읽기: 컬럼 추가 · 삭제(DB 에 없음) · 타입 변경이 항목으로 보이고, 가상 필드는 빠진다 */
  @Test
  void columnsShowAddedRemovedAndRetyped() {
    var before =
        SettingLogService.columns(List.of(column("a", "int4"), column("b", "text"), virtual("v")));
    var gone = column("b", "text");
    gone.setMissingInDb(true);
    var after =
        SettingLogService.columns(
            List.of(column("a", "int8"), gone, column("c", "bool"), virtual("v")));
    var changes = SettingLogService.diff(before, after);
    assertThat(changes).containsOnlyKeys("column.a", "column.b", "column.c");
    assertThat(changes.get("column.a"))
        .containsEntry("before", "int4")
        .containsEntry("after", "int8");
    assertThat(changes.get("column.b"))
        .containsEntry("before", "text")
        .containsEntry("after", null);
    assertThat(changes.get("column.c"))
        .containsEntry("before", null)
        .containsEntry("after", "bool");
    var log = new SettingLog();
    log.setChanges(new LinkedHashMap<>(changes));
    assertThat(SettingLogService.changesOf(log)).extracting(c -> c.label()).contains("컬럼 a");
  }

  private static DbField column(String name, String type) {
    var field = new DbField();
    field.setColumnName(name);
    field.setDataType(type);
    return field;
  }

  private static DbField virtual(String name) {
    var field = column(name, null);
    field.setVirtual(true);
    return field;
  }

  @Test
  void canonicalIgnoresMapOrder() {
    var a = new LinkedHashMap<String, Object>();
    a.put("id", Map.of("role", "ROW_KEY", "required", true));
    a.put("name", Map.of("role", "INPUT"));
    var b = new LinkedHashMap<String, Object>();
    b.put("name", Map.of("role", "INPUT"));
    b.put("id", Map.of("required", true, "role", "ROW_KEY"));
    assertThat(SettingLogService.canonical(a)).isEqualTo(SettingLogService.canonical(b));
  }

  @Test
  void changesUseLabels() {
    var log = new SettingLog();
    var changes = new LinkedHashMap<String, Object>();
    changes.put("pageSize", Map.of("before", "15", "after", "20"));
    log.setChanges(changes);
    assertThat(SettingLogService.changesOf(log))
        .singleElement()
        .satisfies(c -> {
          assertThat(c.label()).isEqualTo("페이지 크기");
          assertThat(c.before()).isEqualTo("15");
          assertThat(c.after()).isEqualTo("20");
        });
  }
}
