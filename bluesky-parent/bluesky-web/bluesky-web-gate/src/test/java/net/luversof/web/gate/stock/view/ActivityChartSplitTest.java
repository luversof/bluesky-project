package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 내역의 월별 차트를 <b>성격별로 둘</b>로 나눈 것.
 *
 * <p>예전에는 매수 · 매도 · 배당 금액을 한 축에 올렸다. 그런데 셋은 크기대가 다르다 &mdash; 실측 2026-09-07: 배당 중앙값 1,527,030 원은 최대
 * 막대 246,858,685 원의 <b>0.62%</b> 로, 170px 차트에서 약 1px 이다. 초록 막대는 사실상 그려지지 않았다.
 *
 * <p>게다가 그 그림은 현금흐름이라 <b>위/아래가 규칙</b>이었다 &mdash; "매수가 왜 0 선 아래인가" 를 알아야만 읽혔고, 그 설명은 화면에 없었다.
 *
 * <p>그래서 둘로 나눈다.
 *
 * <ul>
 *   <li><b>배당</b> &mdash; 매달 꾸준하고 작다. 늘 들어오는 돈이라 부호가 없다.
 *   <li><b>실현손익</b> &mdash; 판 달에만 크게 난다. 부호가 곧 뜻이라(플러스는 벌었고 마이너스는 잃은 것) 설명이 필요 없다.
 * </ul>
 *
 * <p>금액 자체를 보는 차트는 매매 내역·배당 내역 화면에 따로 있다. 이 화면은 "언제 무엇이 있었나" 를 답한다.
 */
class ActivityChartSplitTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");

  private String source() throws IOException {
    return Files.readString(FRAGMENT, StandardCharsets.UTF_8);
  }

  @Test
  void 배당과_실현손익을_각자_차트로_그린다() throws IOException {
    String source = source();

    assertThat(source)
        .as("성격이 다른 값을 한 축에 올리면 작은 쪽이 1px 이 된다")
        .contains("activityDividendChart")
        .contains("activityRealizedChart");
    assertThat(source)
        .as("두 차트에 각각 제목이 있어야 무엇을 보는지 알 수 있다")
        .contains("stock.activity.chart.dividend.title")
        .contains("stock.activity.chart.realized.title");
  }

  /** 옛 현금흐름 그림이 남아 있으면 축이 뒤섞인 그래프가 그대로 뜬다. */
  @Test
  void 옛_현금흐름_차트는_남아_있지_않다() throws IOException {
    String source = source();

    assertThat(source)
        .as("매수를 음수로 뒤집어 아래로 그리던 그림은 걷어냈다")
        .doesNotContain("activityChartBuyOut")
        .doesNotContain("activityChartNetFlow")
        .doesNotContain("activityMonthlyChart");
  }

  /**
   * 실현손익은 <b>매도에만</b> 붙인다.
   *
   * <p>매수 행까지 더하면 요약 카드·연도별 표와 어긋난다. 이 코드베이스가 여러 곳에서 지키는 규칙이다.
   */
  @Test
  void 실현손익은_매도에만_붙인다() throws IOException {
    assertThat(source())
        .as("매수 행의 실현손익까지 더하면 다른 화면과 어긋난다")
        .contains("chartActivity.realizedProfit() != null");
  }

  /** 배당은 늘 들어온 돈이라 부호를 붙이지 않고, 손익만 부호를 붙인다. */
  @Test
  void 부호는_손익_차트에만_붙인다() throws IOException {
    String source = source();

    assertThat(source).contains("signed ? (raw >= 0 ? '+' : '-') : ''");
    assertThat(source)
        .as("배당 차트는 부호 없이, 손익 차트는 부호와 함께 그린다")
        .contains("activityChartDividend,")
        .contains("}, false);")
        .contains("activityChartRealized,")
        .contains("}, true);");
  }

  /**
   * 활동에 실현손익이 실려 와야 한다.
   *
   * <p>예전 {@code Activity} 에는 거래 금액만 있어서, 차트가 현금흐름 말고는 그릴 수 있는 것이 없었다.
   */
  @Test
  void 활동이_실현손익을_싣고_온다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/"
                    + "StockTradeHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller)
        .as("Activity 가 실현손익을 나르지 않으면 차트가 손익을 그릴 수 없다")
        .contains("BigDecimal realizedProfit) {}")
        .as("하루로 묶을 때 손익도 함께 더해야 묶인 줄에서 손익이 사라지지 않는다")
        .contains("newRealizedProfit");
  }
}
