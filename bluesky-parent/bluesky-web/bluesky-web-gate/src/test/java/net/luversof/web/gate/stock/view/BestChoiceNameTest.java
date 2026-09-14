package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "우선 검토 종목" 카드는 종목 이름도 적는다.
 *
 * <p>실측 2026-09-12(월배당 시뮬레이터 개요): 이 카드는 <b>"498400"</b> 다섯 글자뿐이었고 {@code title} 도 {@code
 * aria-label} 도 없었다. 같은 화면의 표는 줄마다 "498400 KODEX 200타겟위클리커버드콜" 처럼 코드와 이름을 함께 적는다 &mdash; 화면의 결론을
 * 말하는 자리만 코드로 남아 있었다(en 도 "Priority Candidate 498400").
 */
class BestChoiceNameTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  @Test
  void 우선_검토_카드가_이름을_함께_적는다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template).contains("data-best-choice-name");
    assertThat(template)
        .as("이름이 없을 수도 있다 - 그때는 코드만 적는다")
        .contains("bestChoice.stockItemName() != null && !bestChoice.stockItemName().isBlank()");
    assertThat(template)
        .as("고른 종목이 없을 때의 문구는 그대로여야 한다")
        .contains("stock.simulator.monthly.summary.no.best.choice");
  }

  /** 코드만 찍던 옛 모양이 남아 있으면 안 된다. */
  @Test
  void 코드만_찍던_한_줄은_사라졌다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .doesNotContain(
            "${bestChoice != null ? bestChoice.stockItemSymbol() : MessageUtil.getMessage(\"stock.simulator.monthly.summary.no.best.choice\")}");
  }
}
