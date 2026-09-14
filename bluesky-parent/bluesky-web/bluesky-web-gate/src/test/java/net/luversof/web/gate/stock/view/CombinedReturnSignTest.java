package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 표의 "평단 기준 예상 합산" 열도 부호로 방향을 말한다.
 *
 * <p>실측 2026-09-12: 이 열의 16 값 중 <b>음수 7 개만</b> "-" 를 달았고 양수 9 개는 맨 숫자였다("10.69%" 등). 고대비 모드에서
 * {@code .text-profit}/{@code .text-loss} 의 색이 모두 검정 한 가지로 합쳐지면 방향이 사라진다.
 *
 * <p>칸 폭은 그대로다 &mdash; 98px 에 "-13.92%" 가 이미 들어가 있었으므로 "+10.69%" 도 같은 길이다.
 *
 * <p>{@code signedPct} 는 0 에 부호를 붙이지 않는다({@code signedWon}·{@code signedCompactKrw} 와 같은 규칙).
 */
class CombinedReturnSignTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  @Test
  void 두_퍼센트_모두_부호를_단다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("평단 기준 수익률")
        .contains(
            "StockFormatUtil.signedPct(row.totalReturnOnCostPct() != null ? row.totalReturnOnCostPct().doubleValue() : 0.0d, 2)");
    assertThat(template)
        .as("예상 합산")
        .contains(
            "StockFormatUtil.signedPct(row.expectedCombinedReturnPct() != null ? row.expectedCombinedReturnPct().doubleValue() : 0.0d, 2)");
    assertThat(template)
        .as("부호 없이 찍던 옛 모양이 남아 있으면 안 된다")
        .doesNotContain("${percentFormat.format(row.totalReturnOnCostPct()")
        .doesNotContain("${percentFormat.format(row.expectedCombinedReturnPct()");
  }

  /** 색은 그대로 둔다 - 부호는 색이 사라졌을 때의 버팀목이다. 칸의 식을 통째로 고정한다. */
  @Test
  void 손익_색도_그대로다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .contains(
            "${row.totalReturnOnCostPct() != null && row.totalReturnOnCostPct().signum() >= 0 ? \"text-profit\" : \"text-loss\"}");
    assertThat(template)
        .contains(
            "${row.expectedCombinedReturnPct() != null && row.expectedCombinedReturnPct().signum() >= 0 ? \"text-profit\" : \"text-loss\"}");
  }

  /** 방향이 없는 수익률(현재가/평단 기준 배당 수익률)은 손대지 않는다 - 배당률에 "+" 는 뜻이 없다. */
  @Test
  void 배당_수익률에는_부호를_붙이지_않는다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .contains(
            "${percentFormat.format(row.expectedMonthlyYieldPct() != null ? row.expectedMonthlyYieldPct() : BigDecimal.ZERO)}%");
  }
}
