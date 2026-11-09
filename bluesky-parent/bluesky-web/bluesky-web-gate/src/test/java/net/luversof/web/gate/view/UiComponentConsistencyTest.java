package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 공용 컴포넌트가 있는 UI 는 화면에서 직접 짜지 않는다(사용자 원칙 2026-11-10: "ui 작업 할 때 항상 컴포넌트를 먼저 구성한 후 공통 사용").
 *
 * <p>카드는 {@code _components/ui/card.jte}(표 카드는 tableCard.jte), 로딩 표시는 {@code spinner.jte}.
 * 2026-11-10 전수 검토에서 카드 7 곳 · 스피너 9 곳이 화면에서 직접 짜여 테두리 · 그림자 · 크기가 화면마다 달랐다. 툴팁은 {@link
 * TooltipConsistencyTest}, 표는 {@link TableConsistencyTest} 가 본다.
 */
class UiComponentConsistencyTest {

  private static final Path JTE = Path.of("src/main/jte/stock");

  /** class 토큰이 정확히 card 인 요소(card-body · card-title 같은 안쪽 클래스는 대상이 아니다). */
  private static final Pattern RAW_CARD =
      Pattern.compile("class=\"(?:[^\"]*\\s)?card(?:\\s[^\"]*)?\"");

  private static final Pattern RAW_SPINNER = Pattern.compile("\\bloading-spinner\\b");

  private static List<String> offenders(Pattern pattern) throws IOException {
    List<String> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String[] lines =
            Files.readString(p, StandardCharsets.UTF_8)
                .replaceAll("(?s)<%--.*?--%>", "")
                .split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
          if (pattern.matcher(lines[i]).find()) {
            out.add(JTE.relativize(p).toString().replace('\\', '/') + ":" + (i + 1));
          }
        }
      }
    }
    return out;
  }

  @Test
  void 카드는_공용_card_로_그린다() throws IOException {
    assertThat(offenders(RAW_CARD)).as("@template._components.ui.card / tableCard 로").isEmpty();
  }

  @Test
  void 로딩_표시는_공용_spinner_로_그린다() throws IOException {
    assertThat(offenders(RAW_SPINNER)).as("@template._components.ui.spinner 로").isEmpty();
  }
}
