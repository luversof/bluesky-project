package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 긴 화면은 화면 안에서 구역으로 뛸 수 있다.
 *
 * <p>실측 2026-09-10(qa/ui-nav.cjs): 배당 7.2화면·활동 5.5화면·매매 5.4화면·시뮬레이터 4.4화면인데 화면 안 이동 링크는 건너뛰기 링크
 * 1개뿐이었고, 제목에 id 가 붙은 것은 한 화면도 없었다. 배당 화면의 마지막 구역(상세 목록)은 3,185px 아래에 있어 굴리는 것 말고는 닿을 길이 없었다.
 *
 * <p>구역 막대는 common.ts 가 한 번만 만든다 - 화면은 data-page-section 으로 어디가 구역인지만 알려 준다. 또 구역 제목이 글자만 굵은 div 면
 * 화면 낭독기의 제목 목록에도 안 잡힌다(실측: 배당 화면의 구역 제목 8개 중 5개가 div 였다).
 */
class PageSectionNavTest {

  private static final List<Path> MARKED =
      List.of(
          Path.of("src/main/jte/stock/htmx/fragments/dividend"),
          Path.of("src/main/jte/stock/htmx/fragments/trade"),
          Path.of("src/main/jte/stock/htmx/fragments/components"),
          Path.of("src/main/jte/stock/simulator.jte"));

  private static final Path DIVIDEND = Path.of("src/main/jte/stock/htmx/fragments/dividend");
  private static final Path COMMON = Path.of("src/main/resources/static/js/common.js");
  private static final Path CSS = Path.of("src/main/frontend/main.css");
  private static final Path LAYOUT = Path.of("src/main/jte/_layout/defaultLayout.jte");

  private static int count(String text, String needle) {
    int found = 0;
    int at = text.indexOf(needle);
    while (at >= 0) {
      found++;
      at = text.indexOf(needle, at + needle.length());
    }
    return found;
  }

  private static int marksIn(Path root) throws IOException {
    if (Files.isRegularFile(root)) {
      return count(Files.readString(root, StandardCharsets.UTF_8), "data-page-section");
    }
    int marks = 0;
    try (Stream<Path> walk = Files.walk(root)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        marks += count(Files.readString(p, StandardCharsets.UTF_8), "data-page-section");
      }
    }
    return marks;
  }

  @Test
  void 긴_화면들은_구역을_알려_준다() throws IOException {
    int total = 0;
    for (Path root : MARKED) {
      int marks = marksIn(root);
      assertThat(marks).as(root + " 에 구역 표식이 없다").isGreaterThan(0);
      total += marks;
    }
    // 배당 8 + 매매(조각 4 + 공용 차트 껍데기 2) + 시뮬레이터 6.
    assertThat(total).as("구역이 3개보다 적은 화면에는 막대가 아예 안 나온다").isGreaterThanOrEqualTo(20);
  }

  @Test
  void 구역_제목은_굵은_div_가_아니라_제목_태그다() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(DIVIDEND)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String html = Files.readString(p, StandardCharsets.UTF_8);
        int at = html.indexOf("data-page-section");
        while (at >= 0) {
          int open = html.lastIndexOf((char) 60, at);
          String tag = html.substring(open, Math.min(html.length(), open + 6));
          if (tag.startsWith("<div")) offenders.add(p.getFileName() + ": " + tag);
          at = html.indexOf("data-page-section", at + 1);
        }
      }
    }
    assertThat(offenders).as("굵은 div 는 화면 낭독기의 제목 목록에 잡히지 않는다").isEmpty();
  }

  @Test
  void 구역_막대는_공용_처리가_만들고_이름표는_화면이_준다() throws IOException {
    String js = Files.readString(COMMON, StandardCharsets.UTF_8);
    assertThat(js).as("구역 표식을 읽지 않는다").contains("data-page-section");
    assertThat(js).as("막대를 만들지 않는다").contains("page-section-nav");
    assertThat(js).as("지금 보는 구역을 표시하지 않는다").contains("aria-current");

    String layout = Files.readString(LAYOUT, StandardCharsets.UTF_8);
    assertThat(layout).as("이름표를 화면에서 주지 않으면 막대에 이름이 없다").contains("data-section-nav-label");
    assertThat(layout).contains("stock.page.sections");

    String css = Files.readString(CSS, StandardCharsets.UTF_8);
    assertThat(css).as("막대 모양 정의가 없다").contains(".page-section-nav {");
  }
}
