package net.luversof.api.stock;

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
 * javadoc 도구가 버리는 Javadoc - 다른 Javadoc 바로 앞(사이에 빈 줄이나 줄 주석만 있어도)이거나 애너테이션 뒤에 놓인 것. 대개 멤버 사이에 새 상수가
 * 끼어들며 주석만 떨어져 남은 자리다(2026-10-22: TradeProfitService.summarizeSeries 의 주석이 ZERO_POINT 위에 있었다). 게이트의
 * 같은 이름 시험과 같은 규칙.
 */
class StackedJavadocTest {

  private static final List<Path> ROOTS =
      List.of(Path.of("src/main/java"), Path.of("src/test/java"));

  private static final Pattern STACKED =
      Pattern.compile(
          "/\\*\\*(?:(?!\\*/).)*\\*/[ \\t]*\\R(?:[ \\t]*(?:(?://[^\\n]*|/\\*(?!\\*)(?:(?!\\*/).)*\\*/)[ \\t]*)?\\R)*[ \\t]*/\\*\\*",
          Pattern.DOTALL);

  private static final Pattern AFTER_ANNOTATION =
      Pattern.compile(
          "^[ \\t]*@[\\w.]+(?:\\([^\\n]*\\))?[ \\t]*(?://[^\\n]*)?\\R[ \\t]*/\\*\\*",
          Pattern.MULTILINE);

  @Test
  void 버려지는_Javadoc_이_없다() throws IOException {
    List<String> found = new ArrayList<>();
    int scanned = 0;
    for (Path root : ROOTS) {
      try (var paths = Files.walk(root)) {
        for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
          scanned++;
          String text = Files.readString(path, StandardCharsets.UTF_8);
          for (Pattern pattern : List.of(STACKED, AFTER_ANNOTATION)) {
            Matcher m = pattern.matcher(text);
            while (m.find()) {
              int line = text.substring(0, m.start()).split("\\R", -1).length;
              found.add(root.relativize(path) + ":" + line);
            }
          }
        }
      }
    }
    assertThat(scanned).as("훑은 파일").isGreaterThan(150);
    assertThat(found).as("앞 덩이를 제 멤버로 옮기거나, 애너테이션을 Javadoc 아래로").isEmpty();
  }
}
