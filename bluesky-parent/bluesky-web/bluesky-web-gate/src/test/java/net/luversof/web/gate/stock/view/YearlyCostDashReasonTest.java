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
      // 키 + " = " 로 본다 - 부분 문자열로 보면 위 키가 이 키의 앞부분이라 자명하게 통과한다.
      assertThat(text).as(bundle).contains("stock.asset.growth.cost.realized.none.total = ");
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

  /**
   * 합계 줄도 까닭을 말해야 한다.
   *
   * <p>실측 2026-09-15(매도 0 건인 종목 둘을 골라 두 해가 나오게 함): 두 줄은 다 "그 해엔 판 적이 없습니다" 라고 말하는데 <b>합계 줄만</b>
   * title 도 sr-only 도 aria-label 도 없는 "-" 한 글자였다. 게다가 갈라내는 기준이 매도 건수가 아니라 금액이라, 팔아서 정확히 0 원인 경우와도
   * 구분되지 않았다.
   */
  @Test
  void 합계_줄의_빈_실현손익도_까닭을_말한다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    assertThat(template)
        .as("합계 줄도 매도 건수로 갈라야 한다 - 금액으로 가르면 '판 적 없음' 과 '팔았는데 0 원' 이 같아진다")
        .contains("costTotals.sellCount() > 0 ? zeroAmountLabel : noSaleTotalLabel");
    assertThat(template)
        .as("합계 줄은 해가 아니다 - 줄 문구를 그대로 쓰면 \"그 해엔\" 이 전체 기간을 가리킨다")
        .contains("stock.asset.growth.cost.realized.none.total");
    // 까닭 없는 맨 대시가 남아 있으면 안 된다. 낱말이 있는지만 보면 줄 쪽 코드가 이미 갖고 있어 통과한다.
    assertThat(squeeze(template))
        .as("까닭 없는 '-' 한 글자가 남아 있다")
        .doesNotContain("<span class=\"text-base-content/60\">-</span>");
    assertThat(template).as("합계 줄 까닭이 마우스에 닿지 않는다").contains("title=\"${totalRealizedReason}\"");
    assertThat(template)
        .as("합계 줄 까닭이 낭독기에 닿지 않는다")
        .contains("<span class=\"sr-only\">${totalRealizedReason}</span>");
  }

  /** 계약: 합계 줄이 가르려면 합계에도 매도 건수가 있어야 한다. */
  @Test
  void 합계에도_매도_건수가_실린다() throws IOException {
    String util =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/util/StockYearlyCostTotalUtil.java"),
            StandardCharsets.UTF_8);
    assertThat(util).as("합계 record 에 매도 건수가 없다").contains("long sellCount) {}");
    assertThat(util).as("합계를 세는 곳이 없다").contains("sumSellCount(rows));");
  }

  /** 매매 화면의 같은 자리도 함께 본다 - 같은 표 꼴이라 한쪽만 고치면 다시 갈린다. */
  @Test
  void 매매_기간별_집계_합계도_까닭을_말한다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte"),
            StandardCharsets.UTF_8);

    assertThat(squeeze(template))
        .as("까닭 없는 '-' 한 글자가 남아 있다")
        .doesNotContain("<span class=\"text-base-content/60\">-</span>");
    // 줄 하나 + 합계 하나.
    assertThat(countOf(template, "title=\"${noSaleTitle}\">-</span><span"))
        .as("매도 0 건 까닭을 단 칸")
        .isEqualTo(2);
  }

  /** 빌드가 ${} 안 공백을 지우므로 원본 서식 그대로 비교하면 변이를 통과시킨다. 공백을 눌러 비교한다. */
  private static String squeeze(String source) {
    return source.replaceAll("\\s+", " ");
  }

  private static int countOf(String source, String marker) {
    int n = 0;
    for (int at = source.indexOf(marker); at >= 0; at = source.indexOf(marker, at + 1)) {
      n++;
    }
    return n;
  }
}
