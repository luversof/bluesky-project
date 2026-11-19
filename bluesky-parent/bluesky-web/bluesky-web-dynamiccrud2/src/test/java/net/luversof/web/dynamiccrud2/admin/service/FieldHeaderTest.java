package net.luversof.web.dynamiccrud2.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.domain.DbField;

/** 화면 제목 순서: 필드 라벨 → 쿼리 별칭 → DB 주석 → 컬럼 이름(DB 없이) */
class FieldHeaderTest {

  private static DbField field(String label, String comment) {
    var field = new DbField();
    field.setColumnName("user_name");
    field.setLabel(label);
    field.setDbComment(comment);
    return field;
  }

  @Test
  void labelWinsOverAlias() {
    assertThat(FieldService.headerOf(field("이름", "주석"), "별칭", "user_name")).isEqualTo("이름");
    assertThat(FieldService.headerOf(field(" ", "주석"), "별칭", "user_name")).isEqualTo("별칭");
    assertThat(FieldService.headerOf(field(null, "주석"), null, "user_name")).isEqualTo("주석");
    assertThat(FieldService.headerOf(field(null, null), null, "x")).isEqualTo("user_name");
    assertThat(FieldService.headerOf(null, "별칭", "x")).isEqualTo("별칭");
    assertThat(FieldService.headerOf(null, null, "x")).isEqualTo("x");
  }
}
