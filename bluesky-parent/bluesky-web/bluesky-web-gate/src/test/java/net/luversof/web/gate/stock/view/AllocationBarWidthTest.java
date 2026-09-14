package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 보유 비중 막대는 옆 글자와 <b>같은 값</b>을 써야 한다.
 *
 * <p>글자는 {@code StockFormatUtil.balancedPct}(최대잔여법으로 합을 100.0% 에 맞춘다)를 쓰는데 막대만 원값을 반올림해 그렸다 &mdash;
 * 실측 2026-09-12(대시보드 보유 비중 6행): RISE 200위클리커버드콜 이 <b>막대 4.5% · 글자 4.6%</b> 였다. 원값은 73,685,735 /
 * 1,622,109,770 = 4.543% 이고, 6행 원값 합이 99.9 라 잔차가 가장 큰 이 행이 0.1 올라갔다. 나머지 5행은 막대와 글자가 같았고, 금액 합은 총자산
 * 1,622,109,770 과 정확히 일치했다.
 *
 * <p>어긋나는 폭은 표시 자릿수 한 칸(0.1%p)을 넘지 않아 눈에 띄지는 않지만, 같은 줄이 두 수를 말한다. 글자를 원값으로 되돌리면 합 100.0%
 * 성질(2026-09-11 에 세운 규칙)이 깨지므로 막대가 글자를 따른다.
 *
 * <p>화면끼리의 0.1%p 차이(대시보드 4.6% vs 자산 현황 4.5%)는 <b>결함이 아니다</b> &mdash; 두 화면이 서로 다른 행 집합 (6행 vs 9행)에서
 * 합을 맞추기 때문이고, {@code balancedPct} 는 "행 하나가 원값에서 0.1%p 벗어날 수 있다" 를 이미 밝히고 있다.
 */
class AllocationBarWidthTest {

  @Test
  void 막대_너비는_표시값을_따른다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/allocationBars.jte"),
            StandardCharsets.UTF_8);
    String flat = flatten(template);
    assertThat(flat)
        .as("막대가 원값을 따로 반올림하면 옆 글자와 어긋난다")
        .doesNotContain("width: ${String.format(\"%.1f\", row.weightPercent())}%")
        .doesNotContain("width: ${String.format(\"%.1f\", othersRow.weightPercent())}%");
    assertThat(flat)
        .contains("width: ${allocationShown.get(allocationRowIndex).replace(\"%\", \"\")}%")
        .contains("width: ${allocationShown.get(allocationRows.size()).replace(\"%\", \"\")}%");
  }

  /** 글자 쪽은 balancedPct 를 그대로 써야 한다 - 원값으로 되돌리면 합이 100.0% 가 아니게 된다. */
  @Test
  void 글자는_합을_맞춘_값을_그대로_쓴다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/allocationBars.jte"),
            StandardCharsets.UTF_8);
    assertThat(template).contains("StockFormatUtil.balancedPct(allocationWeights, 1)");
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
