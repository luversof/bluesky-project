package net.luversof.web.gate.stock.support;

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
 * Javadoc 두 덩이가 바로 붙어 있으면 앞 덩이는 javadoc 도구가 버린다 - 대개 멤버 사이에 새 상수 · 메서드가 끼어들거나 멤버가 옮겨지며 주석만 남은
 * 자리다(2026-10-22: 26 곳을 제 멤버로 옮기거나 낡은 덩이를 지웠다). 다시 생기지 않게 막는다.
 */
class StackedJavadocTest {

  /** 주식 코드와 그 시험(시험의 꾸밈 · 도우미 주석도 같은 까닭으로 떨어져 나간다 - 2026-10-22 시험 쪽 4 곳). */
  private static final List<Path> ROOTS =
      List.of(
          Path.of("src/main/java/net/luversof/web/gate/stock"),
          Path.of("src/test/java/net/luversof/web/gate/stock"));

  /** 사이에 빈 줄이나 줄 · 블록 주석만 있어도 앞 덩이는 버려진다(api TradeProfitService 에 // 한 줄이 낀 자리가 있었다). */
  private static final Pattern STACKED =
      Pattern.compile(
          "/\\*\\*(?:(?!\\*/).)*\\*/[ \\t]*\\R(?:[ \\t]*(?:(?://[^\\n]*|/\\*(?!\\*)(?:(?!\\*/).)*\\*/)[ \\t]*)?\\R)*[ \\t]*/\\*\\*",
          Pattern.DOTALL);

  /**
   * 애너테이션 뒤에 놓인 Javadoc 도 버려진다(StockDividendViewController 세 곳, 2026-10-22). 정규화된 이름 · 뒤 줄 주석도 본다.
   */
  private static final Pattern AFTER_ANNOTATION =
      Pattern.compile(
          "^[ \\t]*@[\\w.]+(?:\\([^\\n]*\\))?[ \\t]*(?://[^\\n]*)?\\R[ \\t]*/\\*\\*",
          Pattern.MULTILINE);

  @Test
  void 주식_코드에_붙은_Javadoc_두_덩이가_없다() throws IOException {
    List<String> stacked = new ArrayList<>();
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
              stacked.add(root.relativize(path) + ":" + line);
            }
          }
        }
      }
    }
    assertThat(scanned).as("훑은 파일").isGreaterThan(400);
    assertThat(stacked).as("앞 덩이를 제 멤버로 옮기거나 지울 곳").isEmpty();
  }
}
