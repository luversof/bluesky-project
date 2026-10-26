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

import org.junit.jupiter.api.Test;

/**
 * 주식 템플릿의 메모는 jte 주석({@code <%-- --%>})으로 - HTML 주석({@code <!-- -->})은 응답에 실려 브라우저로 간다(영어 화면 소스에도
 * 한글 메모가 나갔다). 2026-10-22 에 57 곳(20 파일)을 jte 주석으로 바꾸고 다시 생기지 않게 막는다. 같은 날 인라인 {@code <style>} ·
 * {@code <script>} 안의 블록 주석 · 줄 첫머리 {@code //} 메모 52 곳(13 파일)도 같은 까닭으로 바꿨다.
 */
class TemplateHtmlCommentTest {

  private static final Path STOCK_JTE = Path.of("src/main/jte/stock");

  private static final Pattern JTE_COMMENT = Pattern.compile("<%--.*?--%>", Pattern.DOTALL);

  private static final Pattern INLINE_BLOCK =
      Pattern.compile("<(style|script)\\b[^>]*>(.*?)</\\1>", Pattern.DOTALL);

  private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//");

  @Test
  void 주식_템플릿에_HTML_주석이_없다() throws IOException {
    List<String> found = new ArrayList<>();
    int scanned = 0;
    try (var paths = Files.walk(STOCK_JTE)) {
      for (Path path : paths.filter(p -> p.toString().endsWith(".jte")).toList()) {
        scanned++;
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.contains("<!--")) {
          found.add(STOCK_JTE.relativize(path).toString());
        }
      }
    }
    assertThat(scanned).as("템플릿을 못 찾았다").isGreaterThan(50);
    assertThat(found).as("<!-- --> 대신 <%-- --%>").isEmpty();
  }

  @Test
  void 주식_템플릿의_인라인_스타일_스크립트에_메모_주석이_없다() throws IOException {
    List<String> found = new ArrayList<>();
    int blocks = 0;
    try (var paths = Files.walk(STOCK_JTE)) {
      for (Path path : paths.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String text =
            JTE_COMMENT.matcher(Files.readString(path, StandardCharsets.UTF_8)).replaceAll("");
        Matcher block = INLINE_BLOCK.matcher(text);
        while (block.find()) {
          blocks++;
          String body = block.group(2);
          boolean lineComment =
              "script".equals(block.group(1)) && LINE_COMMENT.matcher(body).find();
          if (body.contains("/*") || lineComment) {
            found.add(STOCK_JTE.relativize(path) + " <" + block.group(1) + ">");
          }
        }
      }
    }
    assertThat(blocks).as("인라인 블록을 못 찾았다").isGreaterThan(20);
    assertThat(found).as("/* */ · 줄 첫머리 // 대신 <%-- --%> (줄 끝에 붙은 // 는 보지 않는다)").isEmpty();
  }
}
