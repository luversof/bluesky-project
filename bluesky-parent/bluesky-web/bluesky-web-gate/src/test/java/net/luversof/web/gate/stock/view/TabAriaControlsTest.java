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
 * {@code role="tab"} 은 ARIA 탭 패턴대로 {@code aria-controls} 로 같은 템플릿의 {@code role="tabpanel"} 요소를
 * 가리킨다.
 *
 * <p>실측 2026-09-09(10화면): 탭 15개 중 aria-controls 0, tabpanel 0 이었다. 보조기술은 탭을 눌러도 어느 영역이 바뀌는지 알 수
 * 없었다. 배당 화면 상단의 {@code href} 탭(전체 이동)은 내비게이션이라 대상이 아니다. 패널 id 는 {@code ${...}} 표현식일 수 있어 텍스트로
 * 대조한다.
 */
class TabAriaControlsTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Pattern TAB =
      Pattern.compile("<(?:a|button|div)\\b[^>]*role=\"tab\"[^>]*>", Pattern.DOTALL);
  // 값이 JTE 표현식(${flag ? "id" : null})이면 안쪽 따옴표까지 통째로 잡는다.
  private static final Pattern CONTROLS =
      Pattern.compile("aria-controls=\"(\\$\\{[^}]*\\}|[^\"]+)\"");

  @Test
  void 탭은_같은_템플릿의_tabpanel_을_가리킨다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int tabs = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String src = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = TAB.matcher(src);
        while (m.find()) {
          String tag = m.group();
          if (tag.contains("href=")) continue; // 내비게이션 탭
          tabs++;
          Matcher c = CONTROLS.matcher(tag);
          if (!c.find()) {
            offenders.add(
                JTE.relativize(p)
                    + ": aria-controls 없음: "
                    + tag.replaceAll("\\s+", " ").substring(0, Math.min(tag.length(), 90)));
            continue;
          }
          // 활동 보기 탭은 그려진 뷰에만 연결한다(${flag ? "id" : null}) - 조건식에서 id 리터럴을 꺼낸다.
          String id = c.group(1);
          Matcher cond = Pattern.compile("^\\$\\{\\w+ \\? \"([^\"]+)\" : null\\}$").matcher(id);
          if (cond.matches()) id = cond.group(1);
          Pattern panel =
              Pattern.compile(
                  "<[a-z]+\\b[^>]*id=\"" + Pattern.quote(id) + "\"[^>]*>", Pattern.DOTALL);
          Matcher pm = panel.matcher(src);
          boolean ok = false;
          while (pm.find()) if (pm.group().contains("role=\"tabpanel\"")) ok = true;
          if (!ok) offenders.add(JTE.relativize(p) + ": " + id + " 를 가진 role=tabpanel 요소가 없다");
        }
      }
    }
    assertThat(tabs).as("탭을 하나도 못 찾았다").isGreaterThanOrEqualTo(13);
    assertThat(offenders).as("패널과 연결되지 않은 탭").isEmpty();
  }
}
