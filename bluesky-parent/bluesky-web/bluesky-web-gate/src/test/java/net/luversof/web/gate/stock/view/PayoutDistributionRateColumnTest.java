package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 지급 이력 표의 <b>분배율</b> 열은 값이 하나라도 있을 때만 낸다.
 *
 * <p>실측 2026-09-11: 저장된 지급 이력 <b>210 건 전부 {@code distributionRatePct} 가 null</b> 이라(8 종목 모두) 이 열은
 * 언제 열어도 "-" 만 29 줄이었다. 출처(KODEX/RISE/TIGER/PLUS)에서 오는 값이 아니고, 게이트가 만들어 주는 붙여넣기 머리말({@code 지급기준일 ·
 * 실지급일 · 분배금액(원) · 주당과세표준액(원)})에도 그 칸이 없다.
 *
 * <p>붙여넣기로 직접 채우면 파서가 그 열을 읽으므로, 값이 생기면 열이 다시 나오게 조건만 건다.
 */
class PayoutDistributionRateColumnTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/fragments/monthlyDividendReference.jte");

  @Test
  void 값이_있을_때만_열을_낸다() throws IOException {
    String fragment = Files.readString(FRAGMENT, StandardCharsets.UTF_8);

    assertThat(fragment).contains("boolean showDistributionRate");
    assertThat(fragment)
        .as("머리칸과 값칸 모두에 조건이 있어야 한다 - 한쪽만 감추면 열이 밀린다")
        .contains("@if(showDistributionRate)");
    int guards = 0;
    String marker = "@if(showDistributionRate)";
    for (int at = fragment.indexOf(marker); at >= 0; at = fragment.indexOf(marker, at + 1)) {
      guards++;
    }
    assertThat(guards).isEqualTo(2);
  }

  @Test
  void 파서는_그_열을_여전히_읽는다() throws IOException {
    String parser =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/util/MonthlyDividendPayoutImportParser.java"),
            StandardCharsets.UTF_8);

    // 열을 감추는 것이지 기능을 없애는 것이 아니다 - 붙여넣기에 분배율이 있으면 그대로 저장된다.
    assertThat(parser).contains("distributionRateIndex");
  }
}
