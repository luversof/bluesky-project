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
 * 주식 화면의 모든 {@code <table>} 은 접근성 이름을 가진다 - {@code aria-label}(그 표가 속한 카드/섹션 제목과 같은 문구), 또는 {@code
 * aria-labelledby}/{@code <caption>}.
 *
 * <p>실측 2026-09-09(13화면): 표 73개 전부 이름이 없었다. 화면 하나에 표가 여러 개(자산 현황 3, 배당 4, 시뮬레이터 연도별 40+)라 보조기술은 어느
 * 표인지 구분할 수 없었다(WCAG 1.3.1). 템플릿 31곳과 스크립트가 그리는 시뮬레이터 월 상세 표(연도별 1개, 이름에 연도 포함)에 붙였다. 표 이름은 근처 제목이
 * 이미 쓰는 표현식을 재사용해 문구를 늘리지 않는다.
 */
class TableAccessibleNameTest {

  private static final Path JTE = Path.of("src/main/jte/stock");

  /** 시뮬레이터 연도별 월 상세 표는 스크립트가 그린다 - 그 템플릿 문자열도 같은 규칙. */
  private static final Path TS = Path.of("src/main/frontend/src/stock");

  private static final Pattern TABLE = Pattern.compile("<table\\b[^>]*>");

  @Test
  void 모든_표에_접근성_이름이_있다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int tables = 0;
    for (Path root : List.of(JTE, TS)) {
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p :
            walk.filter(x -> x.toString().endsWith(".jte") || x.toString().endsWith(".ts"))
                .toList()) {
          String src = Files.readString(p, StandardCharsets.UTF_8);
          Matcher m = TABLE.matcher(src);
          while (m.find()) {
            // 스크립트의 주석(// ... <table data-sortable> ...)에 적힌 태그는 표가 아니다.
            int lineStart = src.lastIndexOf('\n', m.start()) + 1;
            String linePrefix = src.substring(lineStart, m.start()).strip();
            if (p.toString().endsWith(".ts")
                && (linePrefix.startsWith("//") || linePrefix.startsWith("*"))) {
              continue;
            }
            tables++;
            String tag = m.group();
            boolean named =
                tag.contains("aria-label=\"${")
                    || tag.contains("aria-labelledby=\"")
                    || src.startsWith("<caption", m.end());
            if (!named) {
              offenders.add(root.relativize(p) + ": " + tag.replaceAll("\\s+", " "));
            }
          }
        }
      }
    }
    assertThat(tables).as("표를 하나도 못 찾았다 - 경로가 옮겨졌다").isGreaterThanOrEqualTo(32);
    assertThat(offenders).as("이름 없는 표(보조기술이 구분 못 함)").isEmpty();
  }
}
