package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 연도별 세금·비용 표의 실현 손익 "-" 에는 까닭이 붙는다.
 *
 * <p>실측 2026-09-12: 14 개 해 중 셋(2024·2017·2009)이 실현 손익 자리에 "-" 한 글자만 있었고 {@code title} 도 {@code
 * sr-only} 도 없었다 &mdash; 마우스로도 낭독기로도 까닭을 알 수 없었다. 그리고 셋 다 <b>그 해 매도가 0 건</b>이었다.
 *
 * <p>금액만으로는 "판 적이 없다" 와 "팔았는데 0 원" 이 갈리지 않는다. api-stock 의 {@code yearlyCost} 응답에 {@code sellCount}
 * 를 더해(실측 2026-09-12: 2026:7 2025:11 2024:0 2023:1 2022:8 2021:4 2020:14 2019:2 2018:2 2017:0
 * 2015:2 2014:1 2010:3 2009:0 &mdash; 원장을 직접 세어 얻은 수와 전부 같다) 그 수로 문구를 고른다.
 *
 * <p>같은 표의 다른 금액 칸은 {@code amountCell} 이 이미 이 규칙을 지키고 있다(0 이면 "0원" 을 단다).
 */
class YearlyCostDashReasonTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/htmx/fragments/yearlyCostSummary.jte");

  @Test
  void 매도가_없던_해와_팔았는데_0원인_해를_가른다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("매도 건수로 갈라야 한다 - 금액만 보면 둘이 같아 보인다")
        .contains("row.sellCount() > 0 ? zeroAmountLabel : noSaleLabel");
    assertThat(template).contains("stock.asset.growth.cost.realized.none");
    assertThat(template).contains("common.amount.zero.title");
  }

  @Test
  void 까닭은_마우스와_낭독기_모두에_닿는다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    // title 만 달면 마우스 전용이다 - 이 세션에서 두 번 겪은 실수다.
    assertThat(template).as("빈 자리에 title 이 없다").contains("title=\"${realizedReason}\"");
    assertThat(template)
        .as("빈 자리에 sr-only 가 없다 - title 만으로는 낭독기에 닿지 않는다")
        .contains("<span class=\"sr-only\">${realizedReason}</span>");
  }

  @Test
  void 두_언어에_문구가_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains("stock.asset.growth.cost.realized.none");
    }
  }

  /** 계약: 매도 건수를 받아야 갈라낼 수 있다. */
  @Test
  void 매도_건수는_응답에_실려_온다() throws IOException {
    String dto =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/dto/response/YearlyCostSummary.java"),
            StandardCharsets.UTF_8);
    assertThat(dto).as("게이트 DTO 에 매도 건수가 없다").contains("long sellCount");
  }
}
