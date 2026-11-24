package net.luversof.web.dynamiccrud2.menu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Types;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.SqlParameterValue;

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

  @Test
  void jsonColumnBindsUntyped() {
    var field = new DbField();
    field.setInputType(InputType.TEXTAREA);
    field.setDataType("jsonb");
    assertThat(ValueConverter.toBindValue("{\"a\":1}", field))
        .as("PostgreSQL jsonb 는 varchar 로 보내면 거부된다")
        .isInstanceOfSatisfying(
            SqlParameterValue.class,
            value -> {
              assertThat(value.getSqlType()).isEqualTo(Types.OTHER);
              assertThat(value.getValue()).isEqualTo("{\"a\":1}");
            });
    field.setDataType("JSON");
    assertThat(ValueConverter.toBindValue("{}", field)).as("MySQL JSON 은 글자 그대로").isEqualTo("{}");
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
