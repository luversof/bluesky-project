package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 주식 화면의 button 은 type 을 적는다.
 *
 * <p>type 이 없는 button 은 submit 이다. 지금 form 밖에 있어도 나중에 화면이 form 으로 감싸이면 누를 때마다 그 form 이 같이 제출된다.
 * 2026-10-22 정적 점검에서 관리 화면의 데이터 갱신 단추 넷(종목 · 매매 · 배당 · 시세)만 type 이 없었다 - 나머지는 모두 type 을 적고 있었다.
 */
class ButtonTypeTest {

  private static final Pattern BUTTON =
      Pattern.compile("<button\\b((?:[^>\"']|\"[^\"]*\"|'[^']*')*)>");

  static List<String> buttonsWithoutType(String template) {
    String text = template.replaceAll("(?s)<%--.*?--%>", " ");
    List<String> found = new ArrayList<>();
    Matcher m = BUTTON.matcher(text);
    while (m.find()) {
      // 따옴표 안의 값은 지우고 본다 - data-x=\"type=button\" 같은 글자에 속지 않게(2026-10-22 TableHeaderScopeTest 와 같은
      // 규칙).
      String attributes = m.group(1).replaceAll("\"[^\"]*\"|'[^']*'", "");
      if (!Pattern.compile("(?:^|\\s)type\\s*=").matcher(attributes).find()) {
        found.add(m.group().replaceAll("\\s+", " "));
      }
    }
    return found;
  }

  @Test
  void 주식_템플릿의_button_은_모두_type_을_적는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int buttons = 0;
    try (Stream<Path> walk = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String text = Files.readString(p, StandardCharsets.UTF_8);
        buttons += text.split("<button\\b", -1).length - 1;
        for (String tag : buttonsWithoutType(text)) {
          offenders.add(p.getFileName() + ": " + tag.substring(0, Math.min(120, tag.length())));
        }
      }
    }
    assertThat(buttons).as("button 을 하나도 못 찾았다 - 경로가 바뀌었나").isGreaterThan(50);
    assertThat(offenders).isEmpty();
  }

  /** 판정 자체가 맞는가 - 속성이 여러 줄이어도, 다른 속성 값 안의 글자에 속지 않는다. */
  @Test
  void 판정은_type_속성만_본다() {
    assertThat(buttonsWithoutType("<button type=\"button\" class=\"btn\">a</button>")).isEmpty();
    assertThat(buttonsWithoutType("<button\n\t\tclass=\"btn\"\n\t\ttype=\"submit\">a</button>"))
        .isEmpty();
    assertThat(buttonsWithoutType("<button hx-post=\"/x\" class=\"btn\">a</button>")).hasSize(1);
    assertThat(buttonsWithoutType("<button data-x=\"a > b\" class=\"btn\">a</button>")).hasSize(1);
    assertThat(buttonsWithoutType("<button data-x=\"type=button\">a</button>")).hasSize(1);
    assertThat(
            buttonsWithoutType("<%-- <button class=\"x\"> --%><button type=\"button\">a</button>"))
        .isEmpty();
  }
}
