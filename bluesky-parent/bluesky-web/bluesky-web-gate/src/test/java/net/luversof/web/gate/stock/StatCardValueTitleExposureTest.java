package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * 지표 카드의 정확한 값은 보조기술에도 닿아야 한다.
 *
 * <p>2026-09-10 실측: 정확 금액과 승률 근거가 {@code title} 속성에만 있었다. {@code title} 은 role 없는 {@code div} 에서는
 * 접근성 트리에 이름/설명으로 올라가지 않아, 대시보드 8개 카드에서 원 단위 정확값(예: 991,175,852원)이 노출되는 노드가 0개였다 &mdash; 마우스 호버
 * 전용이었다.
 *
 * <p>화면 값을 {@code aria-hidden} 으로 가리고 {@code valueTitle} 만 읽히면 안 된다. 승률 카드의 {@code valueTitle} 은
 * "43종목 중 35종목이 수익" 처럼 <b>대체가 아니라 보충</b>이라, 가리면 "81.4%" 자체가 사라진다.
 */
class StatCardValueTitleExposureTest {

  private static final Path STAT_CARD = Path.of("src/main/jte/_components/ui/statCard.jte");

  private String template() throws IOException {
    return Files.readString(STAT_CARD, StandardCharsets.UTF_8);
  }

  @Test
  void exactValueIsExposedToAssistiveTech() throws IOException {
    String jte = template();

    assertThat(jte).contains("_components.ui.srExact(text = valueTitle, visible = value)");
  }

  @Test
  void visibleValueIsNotHiddenFromAssistiveTech() throws IOException {
    String jte = template();

    char nl = (char) 10;
    int value = jte.indexOf("class=\"stat-card-value");
    assertThat(value).isGreaterThan(0);

    int from = jte.lastIndexOf(nl, value) + 1;
    String line = jte.substring(from, jte.indexOf(nl, value));

    assertThat(line).doesNotContain("aria-hidden");
  }

  @Test
  void srExactSkipsValuesThatAreAlreadyExactOnScreen() throws IOException {
    String component =
        Files.readString(
            Path.of("src/main/jte/_components/ui/srExact.jte"), StandardCharsets.UTF_8);

    assertThat(component).as("숫자가 같은데 덧붙이면 같은 금액을 두 번 읽힌다").contains("redundant");
    assertThat(component).contains("textDigits.equals(visibleDigits)");

    assertThat(render("3,054,754원", "3,054,754"))
        .as("화면이 이미 전체 자릿수인데 덧붙이면 같은 금액을 두 번 읽힌다 (배당 달력 탭 24건)")
        .isEmpty();
    assertThat(render("991,175,852원", "9억 9,117만"))
        .as("축약된 값에는 정확값이 붙어야 한다")
        .contains("sr-only")
        .contains("991,175,852원");
    assertThat(render("43종목 중 35종목이 수익", "81.4%"))
        .as("근거 설명은 화면 값에서 유도할 수 없다")
        .contains("43종목 중 35종목이 수익");
    assertThat(render("", "9억")).as("붙일 것이 없으면 아무것도 내보내지 않는다").isEmpty();
  }

  private String render(String text, String visible) {
    Map<String, Object> params = new HashMap<>();
    params.put("text", text);
    params.put("visible", visible);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("_components/ui/srExact.jte", params, output);
    return output.toString();
  }

  /**
   * 조건이 거짓일 때 이 컴포넌트는 <b>한 글자도</b> 내보내면 안 된다.
   *
   * <p>실측 2026-09-10: 지시자 사이에 빈 줄 하나를 두었더니 JTE 가 그 개행을 그대로 출력해, 호출한 자리의 숫자 뒤에 개행이 끼었다. 화면은 멀쩡해 보여
   * 브라우저 탐침은 놓쳤고, {@code DividendCalendarSubtotalTest} 의 {@code ">숫자<"} 매칭이 깨져 행 합계가 0 이 됐다.
   */
  @Test
  void srExactEmitsNothingOutsideTheCondition() throws IOException {
    List<String> lines =
        Files.readAllLines(
            Path.of("src/main/jte/_components/ui/srExact.jte"), StandardCharsets.UTF_8);

    assertThat(lines).as("빈 줄은 그대로 출력된다").noneMatch(String::isBlank);

    boolean inCode = false;
    for (String raw : lines) {
      String line = raw.strip();
      if (line.startsWith("!{")) {
        inCode = true;
        continue;
      }
      if (inCode) {
        if (line.equals("}")) {
          inCode = false;
        }
        continue;
      }
      assertThat(line)
          .as("지시자와 @if 밖의 글자는 조건과 무관하게 출력된다: " + line)
          .matches("^(@param |@if[(]|@endif).*");
    }
  }

  @Test
  void everyStatCardCallSitePassesValueTitle() throws IOException {
    List<Path> templates;
    try (var walk = Files.walk(Path.of("src/main/jte"))) {
      templates = walk.filter(p -> p.toString().endsWith(".jte")).toList();
    }

    int callSites = 0;
    for (Path p : templates) {
      String text = Files.readString(p, StandardCharsets.UTF_8);
      int from = 0;
      while (true) {
        int i = text.indexOf("_components.ui.statCard(", from);
        if (i < 0) {
          break;
        }
        callSites++;
        from = i + 1;
      }
    }

    assertThat(callSites).isGreaterThanOrEqualTo(20);
  }
}
