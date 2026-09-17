package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자산 현황의 비중은 표 단위로 함께 반올림한다 &mdash; 표시값의 합이 100.0% 가 되도록.
 *
 * <p>실측 2026-09-11(전체 기간): '종목별 현황' 9 행의 표시 합이 99.9%, '계좌 보유 종목 상세' 다섯 표 중 둘이 100.1% 였다(합계행은
 * 100.0%). 고친 뒤 일곱 표 모두 100.0%, 행 하나가 원값에서 벗어나는 폭은 최대 0.06%p 였다(정확값은 {@code data-weight} 에 그대로 있다).
 */
class AssetStatusWeightSumTest {

  private static final Path ASSET_STATUS =
      Path.of("src/main/jte/stock/htmx/fragments/assetStatus.jte");

  private String read() throws IOException {
    return Files.readString(ASSET_STATUS, StandardCharsets.UTF_8);
  }

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
  void 두_표가_함께_반올림한다() throws IOException {
    String template = read();

    assertThat(count(template, "StockFormatUtil.balancedPct("))
        .as("종목별 현황 · 계좌 보유 종목 상세 · 종목 보유 계좌 상세(2026-09-17)")
        .isEqualTo(3);
    assertThat(template).contains("stockWeightLabelById.getOrDefault(");
    assertThat(template).contains("holdingWeightLabelById.getOrDefault(");
    assertThat(template).contains("accountShareShown.get(accountIndex)");
  }

  /**
   * 대시보드 비중 막대도 같은 규칙을 쓴다(상위 N + 기타가 한 묶음).
   *
   * <p>실측 2026-09-11: 대시보드 비중 표시값 합이 99.9% 였다.
   */
  @Test
  void 대시보드_비중_막대도_함께_반올림한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/allocationBars.jte"),
            StandardCharsets.UTF_8);

    assertThat(template).contains("StockFormatUtil.balancedPct(allocationWeights, 1)");
    assertThat(template).as("기타 줄도 같은 묶음").contains("allocationShown.get(allocationRows.size())");
    assertThat(template).contains("allocationShown.get(allocationRowIndex)");
  }

  @Test
  void 정확값은_그대로_남긴다() throws IOException {
    String template = read();

    assertThat(template)
        .as("표시값은 최대 0.1%p 움직이므로 정확값을 data-weight 로 남겨 둔다")
        .contains("data-weight=");
  }
}
