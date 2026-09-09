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
 * 주식 화면마다 {@code <title>} 이 다르다 &mdash; "화면 이름 · Bluesky Stock".
 *
 * <p>실측 2026-09-09: 화면 12개(대시보드·자산현황·…·종목/계좌 상세·404)의 제목이 전부 "Bluesky Stock" 이었다. 탭·히스토리·북마크에서 구분이
 * 안 되고, 화면 읽기 프로그램은 제목부터 읽는다(WCAG 2.4.2). stockLayout 이 {@code pageTitle} 을 받아 제목을 만들고, 레이아웃을 쓰는
 * 화면은 전부 h1 과 같은 문구를 넘긴다.
 */
class PageTitleTest {

  private static final Path JTE = Path.of("src/main/jte");
  private static final Pattern CALL =
      Pattern.compile("@template\\._layout\\.stockLayout\\(\\R(\\s*)pageTitle = ([^\\n\\r]+),");

  @Test
  void 레이아웃은_화면_이름으로_제목을_만든다() throws IOException {
    String layout =
        Files.readString(JTE.resolve("_layout/stockLayout.jte"), StandardCharsets.UTF_8);
    assertThat(layout).contains("@param String pageTitle = \"\"");
    assertThat(layout)
        .contains(
            "title = pageTitle == null || pageTitle.isEmpty() ? \"Bluesky Stock\" : pageTitle + \" \\u00B7 Bluesky Stock\",");
  }

  @Test
  void 레이아웃을_쓰는_화면은_전부_화면_이름을_넘긴다() throws IOException {
    List<String> callers = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(p, StandardCharsets.UTF_8);
        if (!source.contains("@template._layout.stockLayout(")) {
          continue;
        }
        callers.add(JTE.relativize(p).toString());
        Matcher m = CALL.matcher(source);
        if (!m.find() || m.group(2).isBlank()) {
          missing.add(JTE.relativize(p).toString());
        }
      }
    }
    assertThat(callers).as("stockLayout 을 쓰는 화면").hasSizeGreaterThanOrEqualTo(11);
    assertThat(missing).as("pageTitle 을 첫 인자로 넘기지 않는 화면").isEmpty();
  }
}
