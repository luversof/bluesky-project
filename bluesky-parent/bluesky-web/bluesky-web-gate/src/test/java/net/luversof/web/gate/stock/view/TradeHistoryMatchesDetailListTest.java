package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 매매 표는 두 벌이다 - 매매 화면의 {@code tradeDetailList} 와 자산 성장 화면의 {@code tradeHistory}. 같은 거래를 같은 열로 보여
 * 주므로 <b>표기 규칙도 같아야</b> 한다.
 *
 * <p>규칙이 갈린 채로 있었다 - 실측 2026-09-13 자산 성장 쪽 258 행.
 *
 * <ul>
 *   <li>거래세 <b>228 행</b> · 수수료 <b>51 행</b>이 숫자 "0"(형제 표는 "-" 와 까닭)
 *   <li>매수 <b>203 행</b>의 실현 손익이 빈 칸이라 낭독기에 아무 말도 없었다
 *   <li>실현 손익 55 개 중 <b>부호가 붙은 것은 음수 6 개뿐</b>이고 양수 49 개는 맨 숫자였다 - 2026-09-12 에 형제 표만 {@code
 *       signedWon} 으로 고치고 이쪽을 빠뜨렸다. 색만으로 방향을 말하면 고대비 모드에서 뜻이 사라진다.
 * </ul>
 *
 * <p>이 가드는 두 표를 <b>함께</b> 본다. 한쪽만 고치면 다시 갈린다.
 */
class TradeHistoryMatchesDetailListTest {

  private static final String HISTORY = "src/main/jte/stock/htmx/tradeHistory.jte";
  private static final String DETAIL =
      "src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 두 표 모두 0 인 수수료 · 거래세를 "-" 로 적는다. */
  @Test
  void 두_표가_0_을_대시로_적는다() throws IOException {
    for (String path : new String[] {HISTORY, DETAIL}) {
      String src = read(path);

      assertThat(src).as(path).contains("@if(trade.fee() == null || trade.fee().signum() == 0)");
      assertThat(src).as(path).contains("@if(trade.tax() == null || trade.tax().signum() == 0)");
      assertThat(src).as(path).contains("${zeroAmountTitle}");
    }
  }

  /**
   * 두 표 모두 매수 행에 까닭을 적는다 - 빈 칸으로 두지 않는다.
   *
   * <p>까닭이 {@code title} 에만 있으면 마우스 전용이 된다. 그래서 <b>sr-only 까지</b> 확인한다 - 실측: sr-only 만 지우는 변이를
   * title 만 보는 단언은 놓쳤다.
   */
  @Test
  void 두_표가_매수_행에_까닭을_적는다() throws IOException {
    assertThat(read(HISTORY))
        .contains("<span class=\"sr-only\">${isBuy ? buyNoRealizedTitle : noDataTitle}</span>");
    assertThat(read(HISTORY)).contains("title=\"${isBuy ? buyNoRealizedTitle : noDataTitle}\"");
    assertThat(read(DETAIL))
        .contains(
            "<span class=\"sr-only\">${trade.type() == TradeType.BUY ? buyNoRealizedTitle :"
                + " noDataTitle}</span>");
  }

  /** 두 표 모두 실현 손익에 부호를 붙인다. */
  @Test
  void 두_표가_실현_손익에_부호를_붙인다() throws IOException {
    for (String path : new String[] {HISTORY, DETAIL}) {
      assertThat(read(path))
          .as(path)
          .contains(
              "StockFormatUtil.signedWon(StockFormatUtil.displayWon(trade.realizedProfit()))");
    }
  }

  /** 부호 없는 옛 형태가 남아 있으면 안 된다. */
  @Test
  void 부호_없는_옛_형태가_없다() throws IOException {
    assertThat(read(HISTORY)).doesNotContain("df.format(trade.realizedProfit())");
  }

  /** 0 을 그대로 찍던 옛 형태가 남아 있으면 안 된다. */
  @Test
  void 숫자_0_을_그대로_찍던_옛_형태가_없다() throws IOException {
    String src = read(HISTORY);

    assertThat(src).doesNotContain("trade.fee() != null ? df.format(trade.fee()) : \"0\"");
    assertThat(src).doesNotContain("trade.tax() != null ? df.format(trade.tax()) : \"0\"");
  }

  /** 자리표시 대시는 모두 가려져 있다 - 안 가리면 "-0원" 으로 읽힌다. */
  @Test
  void 자리표시_대시가_모두_가려져_있다() throws IOException {
    String src = read(HISTORY);
    int at = src.indexOf(">-</span>");
    int seen = 0;
    while (at >= 0) {
      String before = src.substring(Math.max(0, at - 160), at);

      assertThat(before).as("가려지지 않은 자리표시 대시: " + before).contains("aria-hidden=\"true\"");
      seen++;
      at = src.indexOf(">-</span>", at + 1);
    }
    assertThat(seen).as("자리표시 대시 수").isGreaterThanOrEqualTo(3);
  }
}
