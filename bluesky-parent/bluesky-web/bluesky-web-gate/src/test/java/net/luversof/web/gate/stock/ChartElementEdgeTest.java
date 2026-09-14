package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 막대·도넛 조각은 채움만으로는 카드 배경과 구분되지 않는다 &mdash; 실측 2026-09-10(요소 단위): 라이트 152개 · 다크 47개가 WCAG
 * 1.4.11(비텍스트 3:1) 미달이었다. 막대는 {@code borderWidth: 0} 이고 도넛 테두리색은 카드 배경과 같아(대비 1.00) 채움이 유일한 단서였다.
 *
 * <p>팔레트를 어둡게 바꾸면 계열끼리 구분이 되레 나빠지므로 색은 두고 <b>경계를 보이게</b> 했다(폼 컨트롤과 같은 토큰). 결과: 라이트 152 → <b>0</b>,
 * 다크 47 → 24(남은 24 는 선 차트 색이라 테두리로는 못 고친다).
 *
 * <p>선 차트는 제외해야 한다 &mdash; 선의 {@code borderColor} 는 선 자체 색이다.
 */
class ChartElementEdgeTest {

  private static final Path STOCK_CHARTS = Path.of("src/main/frontend/src/stock-charts.ts");

  private static final Path DIVIDEND_HISTORY =
      Path.of("src/main/frontend/src/stock/dividendHistory.ts");

  private static final Path BUILT_JS = Path.of("src/main/resources/static/js/stock-charts.js");

  @Test
  void chartsGetAnEdgeFromTheControlLineToken() throws IOException {
    String source = Files.readString(STOCK_CHARTS, StandardCharsets.UTF_8);

    assertThat(source)
        .as("폼 컨트롤과 같은 토큰을 써야 라이트 3.25 · 다크 3.34 가 나온다")
        .contains("--color-control-line");
    assertThat(source)
        .as("테마 적용 경로에서 defaults 를 세워야 이후 만들어지는 차트에도 붙는다")
        .contains("chartLib.defaults.datasets[type].borderWidth = 1");
  }

  @Test
  void lineChartsAreExcluded() throws IOException {
    String source = Files.readString(STOCK_CHARTS, StandardCharsets.UTF_8);
    int at = source.indexOf("const EDGE_TYPES");

    assertThat(at).as("테두리 대상 목록을 찾지 못했다").isGreaterThan(0);

    String list = source.substring(at, source.indexOf(";", at));

    assertThat(list).contains("bar").contains("doughnut");
    assertThat(list).as("선의 borderColor 는 선 자체 색이다 - 덮으면 선이 회색으로 바뀐다").doesNotContain("\"line\"");
  }

  @Test
  void datasetsDoNotPinBorderWidthToZero() throws IOException {
    String source = Files.readString(DIVIDEND_HISTORY, StandardCharsets.UTF_8);

    assertThat(source)
        .as("borderWidth 0 을 박으면 기본값이 져서 그 차트만 3:1 미달로 남는다(실측: 라이트 84개)")
        .doesNotContain("borderWidth: 0");
  }

  @Test
  void builtBundleCarriesTheEdge() throws IOException {
    String built = Files.readString(BUILT_JS, StandardCharsets.UTF_8);

    assertThat(built)
        .as("빌드 산출물이 배포본이다 - npm run build 를 빠뜨리면 원본만 고쳐진다")
        .contains("--color-control-line");
  }
}
