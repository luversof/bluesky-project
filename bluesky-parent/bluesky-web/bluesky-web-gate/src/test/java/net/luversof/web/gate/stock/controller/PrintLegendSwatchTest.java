package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 차트 범례의 색 점은 종이에도 찍혀야 한다.
 *
 * <p>범례 점(8px 원)의 색은 캔버스 조각 색과 짝지어 읽는 유일한 단서다. 그런데 배경색은 종이에 기본으로 찍히지 않고 ({@code print-color-adjust:
 * economy}) <b>캔버스는 이미지라 색이 그대로 찍힌다</b> &mdash; 점만 사라져 범례를 차트에 맞출 수 없게 된다.
 *
 * <p>실측 2026-09-11(816px, print 미디어, 다크·라이트 공통 - 점 색은 고정 팔레트다): 흰 종이 대비 1.51~2.99 인 점이 대시보드 5개 · 배당
 * 16개 · 매매 11개였고 모두 {@code economy} 라 인쇄되지 않는다.
 *
 * <p>점에만 {@code print-color-adjust: exact} 를 준다. 나머지 배경(버튼 등)은 지금처럼 찍지 않는다.
 */
class PrintLegendSwatchTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 범례_점에_클래스를_붙인다() throws IOException {
    for (String src :
        new String[] {
          "src/main/frontend/src/stock-charts.ts", "src/main/frontend/src/stock/dividendHistory.ts"
        }) {
      assertThat(read(src))
          .as(src + " 의 범례 점에 클래스가 없으면 인쇄 규칙이 걸리지 않는다")
          .contains("class=" + '"' + "chart-legend-swatch");
    }
  }

  @Test
  void 인쇄에서만_점_색을_강제한다() throws IOException {
    String css = read("src/main/frontend/main.css");

    int print = css.indexOf("@media print {");
    assertThat(print).as("인쇄 블록을 찾지 못했다").isGreaterThan(0);

    assertThat(css.substring(print))
        .as("인쇄 블록 안에 있어야 화면 렌더링에 영향이 없다")
        .contains(".chart-legend-swatch {")
        .contains("print-color-adjust: exact;");
    assertThat(css.substring(0, print))
        .as("화면 쪽에는 색 강제를 두지 않는다")
        .doesNotContain("chart-legend-swatch");
  }

  @Test
  void 다른_배경은_여전히_찍지_않는다() throws IOException {
    String css = read("src/main/frontend/main.css");
    int print = css.indexOf("@media print {");

    assertThat(css.substring(print))
        .as("버튼 배경은 종이에서 투명하게 두는 기존 규칙을 유지한다")
        .contains("background-color: transparent !important;");
  }
}
