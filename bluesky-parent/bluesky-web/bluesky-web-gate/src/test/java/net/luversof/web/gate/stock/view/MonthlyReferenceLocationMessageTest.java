package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 월배당 기준 데이터를 어디서 관리하는지 알려 주는 문구는 실제 자리(관리 &gt; 월배당 기준 데이터)를 가리킨다.
 *
 * <p>실측 2026-10-01: 기준 데이터 화면은 관리 메뉴의 탭({@code /stock/admin?tab=monthly-reference})으로 옮겨졌는데, 시뮬레이터
 * 월배당 탭의 머리 안내 · 빈 상태 문구와 오류 문구 둘이 여전히 "배당 메뉴" 를 가리켰다(한국어 4 · 영어 4). 머리 안내는 메뉴 · 탭 이름을 그 문구에서 받아
 * 다시는 어긋나지 않게 했다.
 */
class MonthlyReferenceLocationMessageTest {

  private static Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (var reader =
        Files.newBufferedReader(Path.of("src/main/resources/" + name), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  void 기준_데이터_안내는_배당_메뉴를_가리키지_않는다() throws IOException {
    for (String name : new String[] {"uiMessage_ko.properties", "uiMessage.properties"}) {
      Properties properties = load(name);
      for (String key : properties.stringPropertyNames()) {
        String value = properties.getProperty(key);
        if (!value.contains("월배당 기준")
            && !value.toLowerCase().contains("monthly dividend reference")) {
          continue;
        }
        assertThat(value)
            .as(name + " " + key)
            .doesNotContain("배당 메뉴")
            .doesNotContainIgnoringCase("dividend menu")
            .doesNotContain("Dividend >");
      }
    }
  }

  @Test
  void 머리_안내는_메뉴와_탭_이름을_문구에서_받는다() throws IOException {
    assertThat(load("uiMessage.properties").getProperty("stock.simulator.monthly.storage.note"))
        .contains("{0}")
        .contains("{1}");
    assertThat(load("uiMessage_ko.properties").getProperty("stock.simulator.monthly.storage.note"))
        .contains("{0}")
        .contains("{1}");
    String simulator =
        Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8);
    assertThat(simulator)
        .contains(
            "MessageUtil.getMessage(\"stock.simulator.monthly.storage.note\"), MessageUtil.getMessage(\"layout.menu.stock.admin\"), MessageUtil.getMessage(\"stock.page.dividend.tab.monthly.reference\")");
  }
}
