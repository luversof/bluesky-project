package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 매매 상세 목록의 실현 손익도 부호로 방향을 말한다.
 *
 * <p>실측 2026-09-12: 이 열의 55 값 중 <b>음수 6 개만</b> "-" 를 달았고 양수 49 개는 맨 숫자였다 ("981,000" · "737,600" …).
 * 방향은 글자색(빨강=수익)만 말하고 있었다.
 *
 * <p>고대비 모드에서 실제로 재 보니 {@code .text-profit}/{@code .text-loss} 의 색이 <b>모두 검정 한 가지</b>로 합쳐졌다(6 화면,
 * 손익 색 요소 273 개 → 색 1 종). 그 상태에서 이 열은 방향을 잃는다.
 *
 * <p>다른 화면은 이미 {@code StockFormatUtil.signedWon} 으로 부호를 붙인다 &mdash; 0 에는 붙이지 않는 규칙까지 같다.
 */
class RealizedProfitSignTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte");

  @Test
  void 실현_손익은_부호를_달고_나온다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("공용 규칙을 쓴다 - 0 에는 부호를 붙이지 않는 것까지 같아야 한다")
        .contains("StockFormatUtil.signedWon(StockFormatUtil.displayWon(trade.realizedProfit()))");
    assertThat(template)
        .as("부호 없이 찍던 옛 모양이 남아 있으면 안 된다")
        .doesNotContain("? decimalFormat.format(trade.realizedProfit()) :");
    assertThat(template).contains("@import net.luversof.web.gate.stock.util.StockFormatUtil");
  }

  /** 합계 줄도 같은 규칙이라야 한다 - 한 표 안에서 줄과 합계가 다르게 말하면 안 된다. */
  @Test
  void 합계_줄도_부호를_단다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .contains("StockFormatUtil.signedWon(StockFormatUtil.displayWon(totalRealizedProfit))");
    assertThat(template).doesNotContain("? decimalFormat.format(totalRealizedProfit) :");
  }

  /**
   * 색은 그대로 둔다 - 부호는 색을 대신하는 것이 아니라 색이 사라졌을 때의 버팀목이다.
   *
   * <p>파일 어딘가에 그 낱말이 있는지가 아니라 <b>그 칸이</b> 색을 쓰는지 본다(같은 낱말이 합계 줄에도 있어서, 파일 단위로 세면 한쪽을 지워도 통과한다 - 실측
   * 2026-09-12 변이에서 그렇게 새어 나갔다).
   */
  @Test
  void 실현_손익_칸은_색도_함께_쓴다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("줄의 실현 손익 칸")
        .contains(
            "${trade.realizedProfit() != null ? (trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? \"text-profit\" : \"text-loss\") : \"\"}");
    assertThat(template)
        .as("합계 줄의 실현 손익 칸")
        .contains(
            "${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? \"text-profit\" : \"text-loss\"}");
  }
}
