package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 차트 범례의 ■ 는 그 선과 같은 색이라야 한다.
 *
 * <p>실측 2026-09-12(자산 성장, 라이트/다크 두 테마):
 *
 * <pre>
 *   범례 ■ text-indigo-400  rgb(129,140,248)   ↔  선 «누적 결산 손익» rgba( 99,102,241,1)
 *   범례 ■ text-green-400   rgb( 74,222,128)   ↔  선 «수익 합산»     rgba( 29,167, 80,1)
 * </pre>
 *
 * <p>색이 다르면 범례가 그림을 잘못 가리킨다. 게다가 흰 바탕에서 {@code text-green-400} 의 대비는 <b>1.78</b> 이라 사실상 보이지 않았다(같은
 * 화면의 다른 글자는 전부 AA 를 넘겼고, 이 화면만 미달 6 건이었다 &mdash; 이 화면은 그동안 주소를 잘못 적어 대비 검사에서 빠져 있었다).
 *
 * <p>선 색은 {@code assetGrowth.ts} 에 있고 범례는 JTE 에 있어 두 벌이 된다. 같은 값인지 여기서 대조한다.
 */
class ChartLegendSwatchTest {

  private static final Path TEMPLATE = Path.of("src/main/jte/stock/htmx/asset-growth.jte");
  private static final Path SCRIPT = Path.of("src/main/frontend/src/stock/assetGrowth.ts");

  @Test
  void 범례_색이_선_색과_같다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
    String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

    assertThat(template).contains("String assetGrowthRealizedSwatch = \"rgb(99, 102, 241)\";");
    assertThat(template).contains("String assetGrowthDividendSwatch = \"rgb(29, 167, 80)\";");
    assertThat(script).as("누적 결산 손익 선").contains("borderColor: 'rgba(99, 102, 241, 1)'");
    assertThat(script).as("수익 합산 선").contains("borderColor: 'rgba(29, 167, 80, 1)'");
    assertThat(template)
        .as("tailwind -400 계열로 돌아가면 선 색과 어긋난다")
        .doesNotContain("text-indigo-400")
        .doesNotContain("text-green-400");
  }

  /** 낱말이 바로 옆에 이름을 적고 있으므로 ■ 자체는 낭독되지 않아야 한다. */
  @Test
  void 범례_기호는_보조기술에서_숨긴다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(countOf(template, "<span aria-hidden=\"true\" style=\"color:${assetGrowth"))
        .as("두 기호 모두")
        .isEqualTo(2);
  }

  /** 뜻이 없는 구분자(· |)도 낭독되지 않아야 한다 - 대비가 낮은 것은 장식이라서 정당하다. */
  @Test
  void 장식용_구분자는_보조기술에서_숨긴다() throws IOException {
    String fragment =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte"),
            StandardCharsets.UTF_8);

    assertThat(countOf(fragment, "text-base-content/25\" aria-hidden=\"true\">|</span>"))
        .as("| 구분자")
        .isEqualTo(2);
    assertThat(countOf(fragment, "mx-0.5\" aria-hidden=\"true\">")).as("· 구분자").isEqualTo(3);
    assertThat(fragment)
        .as("숨기지 않은 구분자가 남아 있으면 안 된다")
        .doesNotContain("<span class=\"text-base-content/25\">|</span>")
        .doesNotContain("<span class=\"text-base-content/30 mx-0.5\">");
  }

  private int countOf(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }
}
