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
 * {@code truncate} 로 잘리는 동적 문구(종목명 등)에는 전체 문구를 볼 수 있는 {@code title} 이 있어야 한다.
 *
 * <p>실측 2026-09-09(375px, 영어 화면): 대시보드 최근 활동의 종목명 링크 5개가 최대 208px 잘리고 다가오는 배당의 종목명도 잘렸는데 title 이 없어
 * 전체 이름을 알 길이 없었다(배당·매매 화면의 같은 이름은 title 이 있었다). 소스 스캔으로 같은 패턴 9곳이 나왔다. 정적 문구가 잘리는 경우는 대상이 아니다(내용이
 * {@code ${...}} 를 포함할 때만 본다).
 */
class TruncatedDynamicTextTitleTest {

  private static final Path JTE = Path.of("src/main/jte/stock");
  private static final Pattern TRUNCATE =
      Pattern.compile(
          "<(?:a|span|div|td|th|p|h[1-6])\\b[^>]*class=\"[^\"]*\\btruncate\\b[^\"]*\"[^>]*>",
          Pattern.DOTALL);

  @Test
  void 잘리는_동적_문구에는_title_이_있다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int dynamic = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String src = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = TRUNCATE.matcher(src);
        while (m.find()) {
          String inner = src.substring(m.end()).split("</", 2)[0];
          if (!inner.contains("${")) continue;
          dynamic++;
          if (!m.group().contains("title=")) {
            offenders.add(
                JTE.relativize(p)
                    + ": "
                    + m.group()
                        .replaceAll("\\s+", " ")
                        .substring(0, Math.min(m.group().length(), 100)));
          }
        }
      }
    }
    assertThat(dynamic).as("잘리는 동적 문구를 하나도 못 찾았다 - 경로가 옮겨졌다").isGreaterThanOrEqualTo(9);
    assertThat(offenders).as("잘려도 전체 문구를 볼 수 없는 요소").isEmpty();
  }
}
