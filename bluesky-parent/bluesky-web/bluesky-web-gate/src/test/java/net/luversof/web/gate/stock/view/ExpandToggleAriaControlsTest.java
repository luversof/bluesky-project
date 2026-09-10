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
 * 펼침 컨트롤({@code aria-expanded})은 {@code aria-controls} 로 펼치는 영역을 가리킨다.
 *
 * <p>실측 2026-09-10: 자산 현황 '보유 종목 보기' 버튼과 활동 달력의 활동 있는 날짜 칸이 aria-expanded 는 갖췄지만 aria-controls 가 없어
 * 보조기술이 어느 영역이 펼쳐지는지 알 수 없었다. 두 곳 모두 대상 id 가 이미 템플릿에 있었다(detailRowId, 날짜 키).
 */
class ExpandToggleAriaControlsTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Pattern EXPANDABLE =
      Pattern.compile("<(?:button|a|div|tr|td|span)\\b[^>]*aria-expanded=[^>]*>", Pattern.DOTALL);

  @Test
  void aria_expanded_컨트롤은_aria_controls_를_가진다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int controls = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        Matcher m = EXPANDABLE.matcher(Files.readString(p, StandardCharsets.UTF_8));
        while (m.find()) {
          controls++;
          String tag = m.group().replaceAll("\\s+", " ");
          if (!tag.contains("aria-controls="))
            offenders.add(JTE.relativize(p) + ": " + tag.substring(0, Math.min(tag.length(), 100)));
        }
      }
    }
    assertThat(controls).as("펼침 컨트롤을 하나도 못 찾았다").isGreaterThanOrEqualTo(2);
    assertThat(offenders).as("펼치는 영역을 가리키지 않는 컨트롤").isEmpty();
  }
}
