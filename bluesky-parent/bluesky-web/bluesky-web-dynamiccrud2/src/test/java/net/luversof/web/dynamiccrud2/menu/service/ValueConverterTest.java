package net.luversof.web.dynamiccrud2.menu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.adapter.UntypedText;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.InputType;

class ValueConverterTest {

  @Test
  void excelNumberKeepsOnlyFifteenDigits() {
    assertThat(ValueConverter.fitsExcelNumber(123_456_789_012_345L)).isTrue();
    assertThat(ValueConverter.fitsExcelNumber(1_234_567_890_123_456L)).as("16자리 ID").isFalse();
    assertThat(ValueConverter.fitsExcelNumber(new BigDecimal("12.3400"))).isTrue();
    assertThat(ValueConverter.fitsExcelNumber(new BigDecimal("0.1234567890123456"))).isFalse();
    assertThat(ValueConverter.fitsExcelNumber(1.5d)).isTrue();
  }

  /** 표준 문자 타입이 아닌 컬럼(jsonb · inet · enum …)은 UntypedText - 어댑터가 DB 에 맞게 보낸다. 문자 타입은 글자 그대로 */
  @Test
  void nonStringColumnsBindUntyped() {
    var field = new DbField();
    field.setInputType(InputType.TEXTAREA);
    for (var type : new String[] {"jsonb", "JSON", "inet", "my_enum", "interval", "_int4"}) {
      field.setDataType(type);
      assertThat(ValueConverter.toBindValue("x", field)).as(type).isEqualTo(new UntypedText("x"));
    }
    for (var type : new String[] {"varchar", "VARCHAR", "text", "bpchar", "nvarchar", "longtext"}) {
      field.setDataType(type);
      assertThat(ValueConverter.toBindValue("x", field)).as(type).isEqualTo("x");
    }
    field.setDataType(null);
    assertThat(ValueConverter.toBindValue("x", field)).as("타입 모름").isEqualTo("x");
    // JSON 칸(여러 줄 입력 · 목록에서 줄임): json · jsonb, 대소문자 무관
    assertThat(ValueConverter.isJson("jsonb")).isTrue();
    assertThat(ValueConverter.isJson("JSON")).isTrue();
    assertThat(ValueConverter.isJson("text")).isFalse();
    assertThat(ValueConverter.isJson(null)).isFalse();
  }

  @Test
  void dateFormatAppliesOrKeepsRaw() {
    assertThat(ValueConverter.formatDate("2026-11-22T13:05:09", "yyyy.MM.dd HH:mm"))
        .isEqualTo("2026.11.22 13:05");
    assertThat(ValueConverter.formatDate("2026-11-22", "MM/dd")).isEqualTo("11/22");
    assertThat(ValueConverter.formatDate("2026-11-22", "yyyy-MM-dd HH:mm"))
        .as("날짜에 시각 패턴은 0시")
        .isEqualTo("2026-11-22 00:00");
    assertThat(ValueConverter.formatDate("어제", "yyyy.MM.dd")).as("읽을 수 없으면 원래 값").isEqualTo("어제");
    assertThat(ValueConverter.formatDate("2026-11-22", null)).isEqualTo("2026-11-22");
    assertThat(ValueConverter.isDateFormat("yyyy.MM.dd")).isTrue();
    assertThat(ValueConverter.isDateFormat(" ")).isTrue();
    assertThat(ValueConverter.isDateFormat("yyyy-bb")).isFalse();
    assertThat(ValueConverter.isDateFormat("yyyy-MM-dd VV")).as("시간대 없는 값에 못 씀").isFalse();
  }
}
