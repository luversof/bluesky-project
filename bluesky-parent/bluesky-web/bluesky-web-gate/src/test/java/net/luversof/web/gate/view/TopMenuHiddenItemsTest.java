package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 머리 막대 메뉴에서 게시판 · 가계부는 숨긴다(사용자 요청 2026-10-02: "상단의 게시판 가계부 메뉴는 비활성화 해두는게 나을거 같네"). 화면은 주소로는 그대로
 * 열린다 - 메뉴의 display 만 끈다(블로그 · 개발과 같은 방식).
 */
class TopMenuHiddenItemsTest {

  private static Properties load() throws IOException {
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static String displayOf(Properties properties, String url) {
    for (int index = 0; index < 20; index++) {
      String prefix = "bluesky.web.common.menu.main[" + index + "].";
      if (url.equals(properties.getProperty(prefix + "url"))) {
        return properties.getProperty(prefix + "display", "true");
      }
    }
    throw new AssertionError("menu not found: " + url);
  }

  @Test
  void 게시판과_가계부는_메뉴에서_숨긴다() throws IOException {
    Properties properties = load();
    assertThat(displayOf(properties, "/board")).isEqualTo("false");
    assertThat(displayOf(properties, "/bookkeeping")).isEqualTo("false");
  }

  @Test
  void 주식과_PoE_는_그대로_보인다() throws IOException {
    Properties properties = load();
    assertThat(displayOf(properties, "/stock")).isEqualTo("true");
    assertThat(displayOf(properties, "/poe")).isEqualTo("true");
    assertThat(displayOf(properties, "/poe2")).isEqualTo("true");
  }
}
