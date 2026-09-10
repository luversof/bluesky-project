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
 * 차트 캔버스에는 접근성 이름이 있다 &mdash; {@code role="img"} + {@code aria-label}(곁의 차트 제목).
 *
 * <p>실측 2026-09-09: 10화면의 캔버스 15개 전부 role 도 aria-label 도 대체 텍스트도 없었다. axe 는 빈 canvas 를 잡지 않으므로 여기서
 * 소스로 못박는다. 차트 내용 자체(수치)는 같은 화면의 표·요약 카드가 텍스트로 제공한다.
 */
class ChartCanvasLabelTest {

  private static final Path STOCK_TEMPLATES = Path.of("src/main/jte/stock");
  private static final Pattern CANVAS = Pattern.compile("<canvas\\b[^>]*>");

  @Test
  void 모든_캔버스에_role_img_와_aria_label_이_있다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int canvases = 0;
    try (Stream<Path> walk = Files.walk(STOCK_TEMPLATES)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = CANVAS.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          canvases++;
          String tag = m.group();
          if (!tag.contains("role=\"img\"") || !tag.contains("aria-label=\"${")) {
            offenders.add(STOCK_TEMPLATES.relativize(p) + ": " + tag);
          }
        }
      }
    }
    assertThat(canvases).as("캔버스를 하나도 못 찾았다 - 경로가 옮겨졌다").isGreaterThanOrEqualTo(15);
    assertThat(offenders).as("접근성 이름이 없는 캔버스").isEmpty();
  }
}
