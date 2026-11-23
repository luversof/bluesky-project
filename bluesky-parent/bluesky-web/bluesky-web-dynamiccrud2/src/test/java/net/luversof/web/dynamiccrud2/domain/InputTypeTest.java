package net.luversof.web.dynamiccrud2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class InputTypeTest {

  /**
   * align() 의 클래스는 Java 에만 있어 Tailwind 가 이 파일을 읽지 않으면 빌드된 CSS 에서 빠진다(2026-11-24 text-center 가 빠졌던
   * 일)
   */
  @Test
  void alignClassesAreInBuiltCss() throws Exception {
    var css = Files.readString(Path.of("src/main/resources/static/main.css"));
    for (var type : InputType.values()) {
      if (!type.align().isEmpty()) {
        assertThat(css).as(type + " " + type.align()).contains("." + type.align() + "{");
      }
    }
  }
}
