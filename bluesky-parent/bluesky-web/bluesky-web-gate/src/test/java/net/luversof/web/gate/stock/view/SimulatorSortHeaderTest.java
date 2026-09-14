package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터 정렬 머리글: 같은 이름 링크를 가르고, 정렬 상태를 알린다.
 *
 * <p>실측 2026-09-11(월배당 탭): 링크 이름이 같은데 목적지가 다른 것이 <b>2 쌍</b>이었다 &mdash; "월배당 수익률" 이 {@code
 * sort=monthly-yield} 와 {@code sort=monthly-yield-on-cost} 두 곳, "연배당 수익률" 도 마찬가지다. 화면에서는 바로 위의 묶음
 * 제목(현재가 기준 / 평단 기준)으로 갈리지만 링크 이름만 들으면 구분되지 않는다(WCAG 2.4.4).
 *
 * <p>또 {@code sort=monthly-yield&direction=desc} 로 열어도 그 칸에 {@code aria-sort} 가 없었다 &mdash; 화면에는
 * "↓" 가 붙는데 보조기술에는 어느 열로 정렬됐는지 전해지지 않았다.
 */
class SimulatorSortHeaderTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 같은_이름_링크에_묶음을_덧붙인다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(
            count(
                template,
                "<span class="
                    + (char) 34
                    + "sr-only"
                    + (char) 34
                    + "> ${marketBasisLabel}</span>"))
        .as("현재가 기준 묶음의 월·연 두 링크")
        .isEqualTo(2);
    assertThat(
            count(
                template,
                "<span class="
                    + (char) 34
                    + "sr-only"
                    + (char) 34
                    + "> ${onCostBasisLabel}</span>"))
        .as("평단 기준 묶음의 월·연 두 링크")
        .isEqualTo(2);
  }

  @Test
  void 정렬된_열은_aria_sort_로_알린다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("aria-sort=" + (char) 34 + "${marketYieldAriaSort}" + (char) 34);
    assertThat(template).contains("aria-sort=" + (char) 34 + "${onCostYieldAriaSort}" + (char) 34);
    // 두 정렬 키 중 하나라도 걸리면 그 칸이 정렬된 것이다.
    assertThat(template)
        .contains(
            "(\"monthly-yield\".equals(monthlyDividendSort) || \"annual-yield\".equals(monthlyDividendSort))");
    assertThat(template)
        .contains(
            "(\"monthly-yield-on-cost\".equals(monthlyDividendSort) || \"annual-yield-on-cost\".equals(monthlyDividendSort))");
  }
}
