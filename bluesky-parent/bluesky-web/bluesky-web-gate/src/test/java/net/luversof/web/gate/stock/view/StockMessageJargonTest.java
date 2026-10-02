package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면 문구는 저장 방식의 이름(DB · localStorage) 대신 사용자에게 생기는 일을 말한다.
 *
 * <p>실측 2026-10-01: 시뮬레이터 문구 5 개(한국어 · 영어)가 "DB에 저장한" · "localStorage에만 저장됩니다" · "No DB calls are
 * used" 처럼 구현을 그대로 드러냈다. 사용자에게 필요한 것은 "서버에 저장되지 않고 이 브라우저에만 남는다 - 다른 기기에서는 안 보인다" 는 결과다. 한국어는 "DB에"
 * 처럼 조사가 붙어 단어 경계(\b)로 찾으면 놓친다 - 처음 전수에서 실제로 하나를 놓쳤다.
 */
class StockMessageJargonTest {

  private static final Pattern JARGON = Pattern.compile("DB|localStorage|sessionStorage|IndexedDB");

  @Test
  void 주식_문구에_저장_방식_이름이_없다() throws IOException {
    List<String> hits = new ArrayList<>();
    for (String name : new String[] {"uiMessage_ko.properties", "uiMessage.properties"}) {
      Properties properties = new Properties();
      try (var reader =
          Files.newBufferedReader(Path.of("src/main/resources/" + name), StandardCharsets.UTF_8)) {
        properties.load(reader);
      }
      for (String key : properties.stringPropertyNames()) {
        if (key.startsWith("stock.") && JARGON.matcher(properties.getProperty(key)).find()) {
          hits.add(name + " " + key);
        }
      }
    }
    assertThat(hits).isEmpty();
  }
}
