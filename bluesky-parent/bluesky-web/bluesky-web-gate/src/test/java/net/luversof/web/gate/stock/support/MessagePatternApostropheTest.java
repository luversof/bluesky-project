package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 인자({0} …)를 받는 주식 문구에 홑따옴표 하나가 있으면 MessageFormat 이 그 뒤를 글자 그대로의 구간으로 읽어 따옴표가 사라진다.
 *
 * <p>실측 2026-10-22: 영어 {@code stock.trade.realized.basis.gap} = "{0} on this account's own basis" 가
 * 매매 화면에서 "… this accounts own basis" 로 나갔다. 같은 문구를 계좌 상세는 자리표시자 앞뒤로 잘라(MessageFormat 없이) 쓰므로
 * {@code ''} 로 바꾸면 그쪽에 두 개가 찍힌다 - 그래서 그 문구는 따옴표 없는 말로 바꿨다. 두 칸 띄어쓰기(오타)도 함께 막는다.
 */
class MessagePatternApostropheTest {

  private static final Pattern ARGUMENT = Pattern.compile("\\{\\d");

  private static Properties load(String name) throws IOException {
    Properties properties = new Properties();
    try (Reader reader =
        Files.newBufferedReader(Path.of("src/main/resources", name), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  void 인자를_받는_주식_문구에_홑따옴표가_없다() throws IOException {
    List<String> bad = new ArrayList<>();
    int checked = 0;
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      Properties properties = load(bundle);
      for (String key : properties.stringPropertyNames()) {
        if (!key.startsWith("stock.")) {
          continue;
        }
        String value = properties.getProperty(key);
        if (value.contains("  ")) {
          bad.add(bundle + " " + key + " (두 칸)");
        }
        if (ARGUMENT.matcher(value).find()) {
          checked++;
          if (value.replace("''", "").contains("'")) {
            bad.add(bundle + " " + key);
          }
        }
      }
    }
    assertThat(checked).as("인자를 받는 문구를 찾지 못했다").isGreaterThan(200);
    assertThat(bad).as("홑따옴표는 '' 로 쓰거나 다른 말로").isEmpty();
  }

  /**
   * 계좌 상세는 이 문구를 자리표시자 앞뒤로 잘라(MessageFormat 없이) 쓴다 - 여기만은 {@code ''} 도 안 된다(화면에 두 개가 찍힌다). 매매 화면은
   * 같은 문구를 MessageFormat 으로 끼우므로 홑따옴표 하나도 안 된다. 그래서 따옴표 자체가 없어야 한다.
   */
  @Test
  void 앞뒤로_잘라_쓰는_문구에는_따옴표가_아예_없다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(load(bundle).getProperty("stock.trade.realized.basis.gap"))
          .as(bundle)
          .isNotNull()
          .doesNotContain("'");
    }
  }
}
