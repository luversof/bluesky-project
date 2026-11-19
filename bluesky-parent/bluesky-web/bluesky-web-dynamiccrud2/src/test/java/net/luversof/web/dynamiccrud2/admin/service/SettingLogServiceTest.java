package net.luversof.web.dynamiccrud2.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
