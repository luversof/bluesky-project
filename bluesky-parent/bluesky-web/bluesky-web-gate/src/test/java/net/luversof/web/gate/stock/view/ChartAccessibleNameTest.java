package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * A chart is a picture: it needs a name, and the numbers behind it need a way out.
 *
 * <p>Measured 2026-09-12 on the running screens: all 15 stock charts carry {@code role="img"}, an
 * {@code aria-label} and an {@code aria-describedby} pointing at the generated summary paragraph.
 * That state is what this guard keeps - a chart added later without a name reads as "graphic" and
 * nothing else, and a summary that stops being attached takes every number in the picture with it.
 *
 * <p>The summary itself is written by the browser, so the guard checks both the source that writes
 * it and the built file that actually ships (Maven does not build the frontend - the committed
 * output is the deployed one).
 */
class ChartAccessibleNameTest {

  private static final char Q = (char) 34;
  private static final Path TEMPLATE_ROOT = Path.of("src/main/jte/stock");

  /** Whitespace is not part of these calls - the built file has none of it. */
  private String squeeze(String source) {
    StringBuilder out = new StringBuilder(source.length());
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (!Character.isWhitespace(c)) {
        out.append(c);
      }
    }
    return out.toString();
  }

  private List<String> canvasTags() throws IOException {
    List<String> tags = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEMPLATE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        int at = source.indexOf("<canvas");
        while (at >= 0) {
          int end = source.indexOf('>', at);
          if (end < 0) {
            break;
          }
          tags.add(file.getFileName() + " :: " + source.substring(at, end + 1));
          at = source.indexOf("<canvas", end);
        }
      }
    }
    return tags;
  }

  @Test
  void 모든_차트가_이름과_역할을_가진다() throws IOException {
    List<String> tags = canvasTags();
    assertThat(tags).as("the stock screens draw charts").isNotEmpty();
    for (String tag : tags) {
      assertThat(tag).as(tag).contains("role=" + Q + "img" + Q);
      assertThat(tag).as(tag).contains("aria-label=" + Q);
      int at = tag.indexOf("aria-label=" + Q) + ("aria-label=" + Q).length();
      String value = tag.substring(at, tag.indexOf(Q, at));
      assertThat(value).as(tag + " must not be named with an empty string").isNotBlank();
    }
  }

  /** Charts hold numbers a picture cannot say out loud - the summary is how they get out. */
  @Test
  void 요약이_캔버스에_연결된다() throws IOException {
    // 배포본은 esbuild 가 쉼표 뒤 공백을 지운다 - 공백을 눌러 비교한다(기록된 함정).
    for (String path :
        new String[] {
          "src/main/frontend/src/common.ts", "src/main/resources/static/js/common.js"
        }) {
      String source = squeeze(Files.readString(Path.of(path), StandardCharsets.UTF_8));
      assertThat(source)
          .as(path + " marks the summary element")
          .contains("setAttribute(" + Q + "data-chart-summary" + Q + "," + Q + Q + ")");
      assertThat(source)
          .as(path + " points the canvas at the summary")
          .contains("canvas.setAttribute(" + Q + "aria-describedby" + Q + ",id)");
      assertThat(source)
          .as(path + " drops the link when there is nothing to say")
          .contains("canvas.removeAttribute(" + Q + "aria-describedby" + Q + ")");
    }
  }

  /** The toggle only changes a class, so the already-drawn summaries have to be rewritten. */
  @Test
  void 금액_가리기가_바뀌면_요약도_다시_쓴다() throws IOException {
    for (String path :
        new String[] {
          "src/main/frontend/src/common.ts", "src/main/resources/static/js/common.js"
        }) {
      String source = Files.readString(Path.of(path), StandardCharsets.UTF_8);
      assertThat(source).as(path).contains("resyncChartSummaries");
    }
  }
}
