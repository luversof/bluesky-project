package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 실현손익 카드의 금액과 건수는 같은 범위를 봐야 한다.
 *
 * <p>실측 2026-09-10: 자산성장 화면이 "실현손익 +225,630,135 · <b>매도 0건(페이지 기준)</b>" 이라고 했다. 금액은 기간 전체인데 건수만 현재
 * 페이지의 행에서 세고 있었고, 1페이지가 최근 매수로만 차 있어 0 이 나왔다. 같은 카드를 매매 화면은 "매도 55건" 으로 보여준다(원장도 55). 기간을 올해로 좁혀도
 * 마찬가지였다 &mdash; "+131,261,409 · 매도 0건".
 */
class TradeHistorySellCountTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 기간_매도_건수를_묶음에_담는다() throws IOException {
    String controller =
        read(
            "src/main/java/net/luversof/web/gate/stock/controller/"
                + "StockAssetGrowthHtmxController.java");

    assertThat(controller).contains("long totalSellCount,");
    assertThat(controller)
        .as("페이지가 아니라 기간 전체(allTrades)에서 세야 한다")
        .contains("allTrades.stream().filter(t -> t.type() == TradeType.SELL).count()");
    assertThat(controller)
        .as("프래그먼트 응답도 같은 값을 받아야 한다")
        .contains("model.addAttribute(\"totalSellCount\", data.totalSellCount())");
  }

  @Test
  void 화면은_기간_건수를_쓴다() throws IOException {
    String template = read("src/main/jte/stock/htmx/tradeHistory.jte");

    assertThat(template).contains("@param long totalSellCount");
    assertThat(template)
        .as("기간 건수가 오면 그것을 쓰고, 없을 때만 페이지 기준으로 떨어져야 한다")
        .contains("sellCountIsPeriod ? totalSellCount : pageSellCount");
  }

  @Test
  void 자산성장_화면이_기간_건수를_넘긴다() throws IOException {
    String template = read("src/main/jte/stock/htmx/asset-growth.jte");

    assertThat(template).contains("totalSellCount = tradeHistoryData.totalSellCount()");
  }
}
