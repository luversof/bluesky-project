package net.luversof.web.gate.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 선 차트는 {@code borderColor} 가 곧 선 자체 색이라, 막대·도넛처럼 테두리를 덧대 3:1 을 만들 수 없다. 색 자체가 배경과 3:1 이어야 한다(WCAG
 * 1.4.11).
 *
 * <p>실측 2026-09-10(요소 단위): 라이트 자산 성장 36개 · 다크 24개가 미달이었다. 색조는 그대로 두고 최소만 바꿨다 &mdash; 라이트는 어둡게(투자원금
 * 2.54→3.17 · 총 자산 2.19→3.15 · 수익 합산 2.28→3.14), 다크 둘은 <b>알파만 0.9→1.0</b> 으로 올려 색조 변경 없이 통과시켰다(최근
 * 12개월 합 2.76→3.13 · 복리 수익 2.70→3.09).
 *
 * <p>알파를 다시 0.9 로 낮추거나 옛 밝은 색으로 되돌리면 그 선이 다시 배경에 묻힌다.
 */
class ChartLineColorContrastTest {

  private static final Path STOCK_CHARTS = Path.of("src/main/frontend/src/stock-charts.ts");

  private static final Path ASSET_GROWTH = Path.of("src/main/frontend/src/stock/assetGrowth.ts");

  private static final Path DIVIDEND_HISTORY =
      Path.of("src/main/frontend/src/stock/dividendHistory.ts");

  private static String read(Path p) throws IOException {
    return Files.readString(p, StandardCharsets.UTF_8);
  }

  @Test
  void lightLinesAreDarkEnough() throws IOException {
    String charts = read(STOCK_CHARTS);
    String growth = read(ASSET_GROWTH);

    assertThat(charts)
        .as("투자원금 선: rgba(156,163,175) 은 흰 배경에서 2.54 였다")
        .doesNotContain("rgba(156, 163, 175, 1)");
    assertThat(charts)
        .as("총 자산 평가액 선: rgba(75,192,192) 은 2.19 였다")
        .doesNotContain("rgba(75, 192, 192, 1)");
    assertThat(growth)
        .as("수익 합산 선: rgba(34,197,94) 은 2.28 이었다")
        .doesNotContain("rgba(34, 197, 94, 1)");
  }

  @Test
  void darkLinesAreOpaque() throws IOException {
    assertThat(read(DIVIDEND_HISTORY))
        .as("알파 0.9 면 어두운 배경에 섞여 2.76 이 된다 - 색조는 그대로 두고 알파만 올린다")
        .doesNotContain("rgba(20,116,73,0.9)");
    assertThat(read(STOCK_CHARTS))
        .as("복리 수익 선도 같은 이유로 알파 1.0 이어야 한다(2.70 -> 3.09)")
        .doesNotContain("rgba(189,44,56,0.9)");
  }

  @Test
  void theLinesStillExist() throws IOException {
    assertThat(read(STOCK_CHARTS))
        .contains("rgba(139, 145, 156, 1)")
        .contains("rgba(62, 159, 159, 1)");
    assertThat(read(ASSET_GROWTH)).contains("rgba(29, 167, 80, 1)");
  }
}
