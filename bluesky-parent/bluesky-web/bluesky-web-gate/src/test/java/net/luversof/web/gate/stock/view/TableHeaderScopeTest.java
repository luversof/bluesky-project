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
 * 주식 화면 표의 머리칸(th)은 scope 를 적는다 - 열 머리는 col, 줄 이름은 row.
 *
 * <p>scope 가 있어야 화면 읽기 프로그램이 칸마다 "어느 열 · 어느 줄의 값인지" 를 같이 읽는다. 2026-10-22 정적 점검에서 표 머리칸 256 개 가운데
 * 대시보드 요약의 "핵심 보유 vs 나머지" 표 셋만 scope 가 없었고, 그 표는 모서리가 빈 th 이고 줄 이름이 td
 * 였다(SummaryPriceBasisRenderTest 의 표).
 */
class TableHeaderScopeTest {

  private static final Pattern TH = Pattern.compile("<th\\b((?:[^>\"']|\"[^\"]*\"|'[^']*')*)>");

  static List<String> headersWithoutScope(String template) {
    String text = template.replaceAll("(?s)<%--.*?--%>", " ");
    List<String> found = new ArrayList<>();
    Matcher m = TH.matcher(text);
    while (m.find()) {
      // 따옴표 안의 값은 지우고 본다 - title=\"scope=col\" 같은 글자에 속지 않게.
      String attributes = m.group(1).replaceAll("\"[^\"]*\"|'[^']*'", "");
      if (!Pattern.compile("(?:^|\\s)scope\\s*=").matcher(attributes).find()) {
        found.add(m.group().replaceAll("\\s+", " "));
      }
    }
    return found;
  }

  @Test
  void 주식_템플릿의_표_머리칸은_모두_scope_를_적는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int headers = 0;
    try (Stream<Path> walk = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String text = Files.readString(p, StandardCharsets.UTF_8);
        headers += text.split("<th\\b", -1).length - 1;
        for (String tag : headersWithoutScope(text)) {
          offenders.add(p.getFileName() + ": " + tag.substring(0, Math.min(120, tag.length())));
        }
      }
    }
    assertThat(headers).as("th 를 하나도 못 찾았다 - 경로가 바뀌었나").isGreaterThan(100);
    assertThat(offenders).isEmpty();
  }

  /** 판정 자체가 맞는가 - thead 같은 다른 태그에 속지 않고, 속성 값 안의 글자에 속지 않는다. */
  @Test
  void 판정은_th_의_scope_속성만_본다() {
    assertThat(headersWithoutScope("<thead><tr><th scope=\"col\">a</th></tr></thead>")).isEmpty();
    assertThat(headersWithoutScope("<th\n\tclass=\"x\"\n\tscope=\"row\">a</th>")).isEmpty();
    assertThat(headersWithoutScope("<th class=\"text-right\">a</th>")).hasSize(1);
    assertThat(headersWithoutScope("<th></th>")).hasSize(1);
    assertThat(headersWithoutScope("<th title=\"scope=col\">a</th>")).hasSize(1);
    assertThat(headersWithoutScope("<%-- <th> --%><th scope=\"col\">a</th>")).isEmpty();
  }
}
