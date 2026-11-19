package net.luversof.web.dynamiccrud2.menu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ValueConverterTest {

  @Test
  void excelNumberKeepsOnlyFifteenDigits() {
    assertThat(ValueConverter.fitsExcelNumber(123_456_789_012_345L)).isTrue();
    assertThat(ValueConverter.fitsExcelNumber(1_234_567_890_123_456L)).as("16자리 ID").isFalse();
    assertThat(ValueConverter.fitsExcelNumber(new BigDecimal("12.3400"))).isTrue();
    assertThat(ValueConverter.fitsExcelNumber(new BigDecimal("0.1234567890123456"))).isFalse();
    assertThat(ValueConverter.fitsExcelNumber(1.5d)).isTrue();
  }
}
