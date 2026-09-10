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
 * 새 창으로 열리는 링크({@code target="_blank"})는 그 사실을 알리고({@code common.label.opens.new.window} 숨은 문구 또는
 * aria-label), {@code rel} 에 noopener/noreferrer 를 둔다.
 *
 * <p>실측 2026-09-10(13화면, 앵커 1,850개): 월 배당 기준 화면의 '링크 열기' 8개가 안내 없이 새 창으로 열렸다(WCAG G201). 보조기술 사용자는
 * 문맥이 바뀌는 것을 미리 알 수 없다.
 */
class NewWindowLinkHintTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Pattern BLANK_ANCHOR =
      Pattern.compile("<a\\b[^>]*target=\"_blank\"[^>]*>(.*?)</a>", Pattern.DOTALL);

  @Test
  void 새_창_링크는_안내와_rel_을_가진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int anchors = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = BLANK_ANCHOR.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          anchors++;
          String whole = m.group();
          String openTag = whole.substring(0, whole.indexOf('>') + 1);
          boolean hinted =
              whole.contains("common.label.opens.new.window") || openTag.contains("aria-label=");
          boolean rel = openTag.contains("noopener") || openTag.contains("noreferrer");
          if (!hinted || !rel)
            offenders.add(JTE.relativize(p) + ": " + openTag.replaceAll("\\s+", " "));
        }
      }
    }
    assertThat(anchors).as("새 창 링크를 하나도 못 찾았다").isGreaterThanOrEqualTo(1);
    assertThat(offenders).as("안내 없이 새 창으로 열리는 링크").isEmpty();
  }
}
