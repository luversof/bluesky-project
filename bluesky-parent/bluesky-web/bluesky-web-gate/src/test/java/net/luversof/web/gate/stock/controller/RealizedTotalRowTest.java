package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 계좌별·종목별 실현손익 표에도 합계 줄이 있어야 한다.
 *
 * <p>같은 화면의 연도별/월별 매매 표와 상세 목록은 이미 {@code tfoot} 합계를 보여 주는데 이 두 표만 없었다(실측 2026-09-11). 그래서 <b>매도원가
 * 총합</b>(전체 기간 1,166,104,134)은 화면 어디에도 없었고, 매도금액·실현손익도 표에서 더해 위 카드(+225,630,135)와 맞춰 볼 수 없었다.
 *
 * <p>합계는 행에서 쓰는 식을 그대로 다시 쓴다 &mdash; 종목별의 매도원가는 {@code 매도금액 - 거래세 - 기록된 실현손익} 이다(계좌별은 서버가 준
 * soldCost). 두 표의 매도금액·실현손익 합은 서로, 그리고 카드와 일치한다(실측).
 */
class RealizedTotalRowTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 두_표_모두_합계_줄을_가진다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(count(template, "<tfoot>")).isEqualTo(2);
    assertThat(count(template, "data-realized-total")).isEqualTo(2);
    assertThat(count(template, "${totalLabel}")).isEqualTo(2);
  }

  @Test
  void 합계는_행과_같은_식으로_더한다() throws IOException {
    String template = read(TEMPLATE);

    // 계좌별: 서버가 준 soldCost 를 그대로 더한다.
    assertThat(template).contains("accRows.stream().map(r -> zeroIfNull.apply(r.soldCost()))");
    // 종목별: 매도금액 - 거래세 - 실현손익 (행과 같은 식).
    assertThat(template)
        .contains(
            "zeroIfNull.apply(i.totalSellAmount())"
                + ".subtract(zeroIfNull.apply(i.totalSellTax()))"
                + ".subtract(zeroIfNull.apply(i.realizedProfit()))");
    assertThat(template)
        .as("행이 쓰는 식이 바뀌면 합계도 함께 바뀌어야 한다")
        .contains("item.totalSellAmount().subtract(itemSellTax).subtract(itemNet)");
  }

  @Test
  void 수익률은_매도원가가_있을_때만_적는다() throws IOException {
    String template = read(TEMPLATE);

    assertThat(template).contains("@if(accTotalSoldCost.signum() > 0)");
    assertThat(template).contains("@if(stockTotalSoldCost.signum() > 0)");
    // 2026-09-15: 그 대시 넷은 title 도 sr-only 도 없는 맨 대시였고 opacity-40 은 대비가 AA 에
    // 못 미쳤다(RealizedRateNoneReasonTest). 세는 자리는 그대로 넷이고 모양만 바뀌었다.
    assertThat(count(template, "title=\"${rateNoneLabel}\">-</span>"))
        .as("행 2 곳 + 합계 2 곳")
        .isEqualTo(4);
  }
}
