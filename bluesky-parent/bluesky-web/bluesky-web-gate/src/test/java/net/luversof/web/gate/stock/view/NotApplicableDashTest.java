package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "해당 없음" 을 뜻하는 "-" 에도 까닭을 단다.
 *
 * <p>0 원 쪽은 이미 "0원" 을 달았다. 남은 대시는 값이 0 이라서가 아니라 <b>그 줄에 그런 값이 없어서</b> 비어 있다 &mdash; 실측
 * 2026-09-11(전체 기간): 종목 상세 매매 내역의 실현 손익 <b>17 칸</b>(매수 줄), 자산 현황 종목별 현황의 실현 손익 <b>6 칸</b>(판 적 없음).
 * 같은 기호가 "0 원" 과 "해당 없음" 을 함께 뜻하고 있었다.
 */
class NotApplicableDashTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 매수_줄의_실현손익에는_까닭이_붙는다() throws IOException {
    String template = read("src/main/jte/stock/htmx/stockItemDetailContent.jte");

    assertThat(template).contains("stock.trade.realized.none.buy");
    assertThat(template)
        .as("값이 있는 줄에는 title 이 나가지 않아야 한다(JTE 스마트 속성)")
        .contains("trade.realizedProfit() != null ? null : MessageUtil.getMessage");
  }

  @Test
  void 판_적_없음과_배당_없음도_말한다() throws IOException {
    String template = read("src/main/jte/stock/htmx/fragments/assetStatus.jte");

    assertThat(template).contains("stock.profit.realized.none");
    assertThat(template).contains("stock.dividend.received.none");
  }

  @Test
  void 세_문구가_양_번들에_있다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String bundle = read("src/main/resources/" + name);
      assertThat(bundle).contains("stock.trade.realized.none.buy");
      assertThat(bundle).contains("stock.profit.realized.none");
      assertThat(bundle).contains("stock.dividend.received.none");
    }
  }
}
