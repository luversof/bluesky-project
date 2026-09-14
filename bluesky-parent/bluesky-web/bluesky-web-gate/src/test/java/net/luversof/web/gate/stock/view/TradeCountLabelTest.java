package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * "기간 매매 현황" 카드의 건수는 <b>매수+매도 전체</b>다 - 라벨 없이 두면 옆의 "매도" 건수로 읽힌다.
 *
 * <p>실측 2026-09-12('올해' 2026-01-01~09-12, api-stock periodSummary 대조): 매수 91건 &middot; 매도 7건
 * &middot; 합계 98건이었는데, 매매 화면과 자산 성장의 매매 이력 패널은 그 자리에 <b>라벨 없는 "98건"</b>을 매도 금액과 같은 줄 ({@code
 * ml-auto})에 두고 있었다. 바로 옆 실현손익 카드는 같은 화면에서 "매도 7건" 이라고 적는다 &mdash; 한 화면이 매도를 98건과 7건 둘로 말했다.
 *
 * <p>활동 화면은 같은 수량({@code buyCount + sellCount})에 이미 {@code stock.activity.label.trade}("매매")를 붙이고
 * 있었다. 같은 라벨을 쓴다.
 */
class TradeCountLabelTest {

  @Test
  void 매매_요약_카드의_건수에_라벨이_붙는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradeSummaryCards.jte"),
            StandardCharsets.UTF_8);
    assertThat(flatten(template))
        .as("라벨 없이 두면 옆의 매도 건수로 읽힌다")
        .contains("${tradeCountLabel} ${countMessage.apply(totalItems)}");
    assertThat(template)
        .as("활동 화면과 같은 라벨을 쓴다")
        .contains("MessageUtil.getMessage(\"stock.activity.label.trade\")");
  }

  @Test
  void 자산성장의_매매_이력_패널도_같다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/tradeHistory.jte"), StandardCharsets.UTF_8);
    assertThat(flatten(template))
        .contains(
            "${MessageUtil.getMessage(\"stock.activity.label.trade\")} ${countMessage.apply(totalItems)}");
  }

  /** 매도 건수를 적는 자리는 그대로여야 한다 - 그쪽까지 총건수로 바꾸면 반대로 틀린다. */
  @Test
  void 실현손익_카드는_매도_건수를_그대로_적는다() throws IOException {
    String template =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/trade/tradeSummaryCards.jte"),
            StandardCharsets.UTF_8);
    assertThat(flatten(template)).contains("${sellLabel} ${countMessage.apply(sellCount)}");
  }

  @Test
  void 라벨_문구는_두_번들에_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.activity.label.trade");
    }
  }

  private String flatten(String source) {
    StringBuilder out = new StringBuilder();
    boolean lastWasSpace = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasSpace) {
          out.append(' ');
        }
        lastWasSpace = true;
      } else {
        out.append(c);
        lastWasSpace = false;
      }
    }
    return out.toString();
  }
}
