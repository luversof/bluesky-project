package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 전기 대비 카드는 변화가 없을 때 무한대로 적지 않는다.
 *
 * <p>실측 2026-09-10(qa/empty-range.cjs, qa/diff-zero.cjs): 배당이 없는 구간(2030-01)을 고르면 전기도 0, 이번 기간도 0
 * 인데 "▲ ∞% +₩0" 이 나왔다. 전기가 0 이면 변화율을 낼 수 없는 것은 맞지만, 이번 기간도 0 이면 그것은 "변화 없음"이다.
 *
 * <p>고친 뒤: 같은 구간에서 "- 0.0% ₩0" 으로 나오고, 전기에만 배당이 있던 구간(2023-09~10)은 그대로 "▼ -100.0%" 다.
 */
class DividendChangeInfinityTest {

  private static final Path SOURCE =
      Path.of("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte");
  private static final Path CARD =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendSummaryCards.jte");

  @Test
  void 변화가_0_이면_무한대_대신_변화_없음으로_적는다() throws IOException {
    String jte = Files.readString(SOURCE, StandardCharsets.UTF_8);
    assertThat(jte).as("변화 없음 판정이 없다").contains("boolean diffZero =");
    int infinity = jte.indexOf("diffPct = " + (char) 34);
    assertThat(infinity).as("변화율 계산부를 찾지 못했다").isGreaterThan(0);
    assertThat(jte).as("변화가 0 인데도 무한대로 적는다").contains("hasComparison && !diffZero");
    assertThat(jte)
        .as("변화 없음일 때 0.0% 를 넣지 않는다")
        .contains("diffPct = " + (char) 34 + "0.0" + (char) 34);
  }

  @Test
  void 변화가_0_이면_오르내림_표시도_붙이지_않는다() throws IOException {
    String card = Files.readString(CARD, StandardCharsets.UTF_8);
    assertThat(card).as("카드가 변화 없음을 전달받지 않는다").contains("@param boolean diffZero");
    assertThat(card)
        .as("변화가 0 인데 손익 색과 화살표가 붙는다")
        .contains("diffZero ? " + (char) 34 + "text-base-content/60" + (char) 34);
  }
}
