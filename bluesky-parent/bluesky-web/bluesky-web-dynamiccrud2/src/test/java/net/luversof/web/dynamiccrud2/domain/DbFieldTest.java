package net.luversof.web.dynamiccrud2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DbFieldTest {

  @Test
  void valueOptionsKeepOrderAndAllowValueOnlyLines() {
    var field = new DbField();
    assertThat(field.valueOptionMap()).isEmpty();
    field.setValueOptions(" 1 = 사용 \r\n\r\n0=중지\nREADY\n1=중복은 앞 줄\na=b=c");
    assertThat(field.valueOptionMap())
        .containsExactly(
            java.util.Map.entry("1", "사용"),
            java.util.Map.entry("0", "중지"),
            java.util.Map.entry("READY", "READY"),
            java.util.Map.entry("a", "b=c"));
  }

  @Test
  void optionsKeepUnknownKeysAndBecomeNullWhenEmpty() {
    var field = new DbField();
    field.setOptions(java.util.Map.of("future", "x"));
    field.setDateFormat(" ");
    field.setValueOptions("1=사용");
    field.setDateFormat("yyyy.MM.dd");
    assertThat(field.getOptions())
        .containsEntry("future", "x")
        .containsEntry("valueOptions", "1=사용")
        .containsEntry("dateFormat", "yyyy.MM.dd");
    field.setOptions(null);
    field.setValueOptions("1=사용");
    field.setValueOptions(null);
    assertThat(field.getOptions()).isNull();
  }
}
