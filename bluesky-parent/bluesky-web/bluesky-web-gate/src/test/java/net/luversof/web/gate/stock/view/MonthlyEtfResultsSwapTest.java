package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 월배당 ETF 화면의 좁히기 · 칩(지급 시기 · 기간 · 추천 기준) · 이번 적립 보기 · 정렬 · 초기화는 페이지를 새로 부르지 않고 결과 구역만 바꿔 끼운다(사용자
 * 요청 2026-10-13 - 시뮬레이터와 같은 방식).
 *
 * <p>부스트는 컨트롤을 담은 상자(붙는 필터 줄 · 표 머리 · 보기 단추 · 점수 정렬 안내)에 건다. 그 안의 다른 화면으로 가는 링크(관리 · 시뮬레이터)는 빼야 한다
 * - 부스트되면 그 화면에 없는 구역을 찾다 빈 화면이 된다.
 */
class MonthlyEtfResultsSwapTest {

  private static final Path PAGE = Path.of("src/main/jte/stock/monthlyEtf.jte");

  private static String read() throws IOException {
    return Files.readString(PAGE, StandardCharsets.UTF_8);
  }

  @Test
  void 결과_구역이_자기만_바꿔_끼우고_맨_위로_굴리지_않는다() throws IOException {
    String page = read();
    int region = page.indexOf("id=\"monthlyEtfResults\"");
    assertThat(region).isNotNegative();
    String tag = page.substring(page.lastIndexOf('<', region), page.indexOf('>', region) + 1);
    assertThat(tag)
        .contains("hx-target=\"#monthlyEtfResults\"")
        .contains("hx-select=\"#monthlyEtfResults\"")
        .contains("show:none")
        .contains("hx-replace-url=\"true\"")
        .as("낭독기 안내")
        .contains("data-swap-announce=");
    assertThat(page.indexOf("table-filter-sticky")).isGreaterThan(region);
    assertThat(page.indexOf("<thead")).isGreaterThan(region);
    assertThat(page.indexOf("stock.monthly.etf.criteria.title"))
        .as("기준 설명 카드는 조건과 무관 - 구역 밖")
        .isGreaterThan(page.indexOf("</div>", page.indexOf("</table>")));
  }

  @Test
  void 컨트롤_상자가_부스트되고_다른_화면_링크는_빠진다() throws IOException {
    List<String> lines = read().lines().toList();
    String sticky =
        lines.stream()
            .filter(l -> l.contains("class=\"table-filter-sticky space-y-3\""))
            .findFirst()
            .orElseThrow();
    assertThat(sticky).contains("hx-boost=\"true\"");
    assertThat(lines.stream().filter(l -> l.contains("<thead")).findFirst().orElseThrow())
        .contains("hx-boost=\"true\"");
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-contribution-view"))
                .filter(l -> l.contains("<div"))
                .findFirst()
                .orElseThrow())
        .contains("hx-boost=\"true\"");
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-score-sort-link"))
                .findFirst()
                .orElseThrow())
        .contains("<p hx-boost=\"true\"");
    // 기준별 비교 카드의 링크도 이 화면 안에서는 구역만 바꾼다(2026-10-13 자율) - 누른 카드 상자를 기준으로 제자리에 둔다.
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-contribution-compare-cards"))
                .filter(l -> l.contains("<div"))
                .findFirst()
                .orElseThrow())
        .contains("hx-boost=\"true\"")
        .contains("data-swap-anchor-key=\"etf-compare\"");
    for (String outbound :
        List.of(
            "href=\"${referenceManageUrl}\" class=\"btn btn-outline", "href=\"${simulatorUrl}\"")) {
      assertThat(lines.stream().filter(l -> l.contains(outbound)).findFirst().orElseThrow())
          .as("붙는 줄 안의 다른 화면 링크는 부스트에서 뺀다: " + outbound)
          .contains("hx-boost=\"false\"");
    }
  }
}
