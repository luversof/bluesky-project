package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 매매 내역 표도 0 과 '해당 없음' 을 같은 규칙으로 적는다.
 *
 * <p>같은 화면의 기간 쪼갬 표와 연도별 세금 · 비용 표는 이미 "-" 와 까닭을 단다. 이 표만 숫자 0 을 그대로 적었다 - 실측 2026-09-13: 매매 258 건
 * 중 <b>거래세 228 행</b>(매수 203 + 매도 25) · <b>수수료 51 행</b>이 "0" 이었다.
 *
 * <p>매수 203 행의 '실현 손익' 칸은 <b>아예 빈 칸</b>이라 낭독기에 아무 말도 남지 않았다. 매수에 실현 손익이 없는 것은 사실이지만, 빈 칸은 그 사실을 말하지
 * 않는다.
 *
 * <p>자리표시 "-" 는 {@code aria-hidden} 으로 가린다 - 안 가리면 sr-only 와 함께 읽혀 "-0원" 이 된다.
 */
class TradeDetailZeroCellsTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte";
  private static final String KEY = "stock.trade.realized.buy.none";

  private String read() throws IOException {
    return Files.readString(Path.of(TEMPLATE), StandardCharsets.UTF_8)
        .replaceAll("[ \t\r\n]+", " ");
  }

  /** 수수료 0 은 "-". */
  @Test
  void 수수료_0_은_대시로_적는다() throws IOException {
    assertThat(read()).contains("@if(trade.fee() == null || trade.fee().signum() == 0)");
    assertThat(cell("trade.fee()")).contains("${zeroAmountTitle}");
  }

  /** 거래세 0 은 "-". */
  @Test
  void 거래세_0_은_대시로_적는다() throws IOException {
    assertThat(read()).contains("@if(trade.tax() == null || trade.tax().signum() == 0)");
    assertThat(cell("trade.tax()")).contains("${zeroAmountTitle}");
  }

  /** 0 이 아니면 예전 그대로 숫자. */
  @Test
  void 값이_있으면_숫자_그대로() throws IOException {
    String src = read();

    assertThat(src).contains("@else${decimalFormat.format(trade.fee())}@endif");
    assertThat(src).contains("@else${decimalFormat.format(trade.tax())}@endif");
  }

  /** 매수 행의 실현 손익은 빈 칸이 아니라 까닭이 있는 "-". */
  @Test
  void 매수_행은_실현_손익이_없다고_말한다() throws IOException {
    String src = read();

    assertThat(src).doesNotContain("TradeType.SELL ? StockFormatUtil.signedWon");
    assertThat(src).contains("trade.type() == TradeType.BUY ? buyNoRealizedTitle : noDataTitle");
    assertThat(src)
        .contains("String buyNoRealizedTitle = MessageUtil.getMessage(\"" + KEY + "\");");
  }

  /** 매도 행의 부호 있는 금액은 그대로 - 이 표의 기존 규칙이다. */
  @Test
  void 매도_행은_부호_있는_금액_그대로() throws IOException {
    assertThat(read())
        .contains(
            "@if(trade.realizedProfit() != null && trade.type() == TradeType.SELL)"
                + "${StockFormatUtil.signedWon(StockFormatUtil.displayWon("
                + "trade.realizedProfit()))}");
  }

  /** 자리표시 대시는 모두 가려져 있다 - 안 가리면 "-0원" 으로 읽힌다. */
  @Test
  void 자리표시_대시가_모두_가려져_있다() throws IOException {
    String src = read();
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

  /** 까닭 문구가 두 로케일에 있다 - ko 값은 이스케이프라 디코드해서 본다. */
  @Test
  void 매수_까닭이_두_로케일에_있다() throws IOException {
    assertThat(value("src/main/resources/uiMessage_ko.properties")).as("한글 값").contains("실현 손익");
    assertThat(value("src/main/resources/uiMessage.properties").toLowerCase())
        .as("영문 값")
        .contains("realized");
  }

  /** 그 칸 한 덩어리만 떼어 낸다 - 파일 전체로 보면 옆 칸에 걸린다. */
  private String cell(String accessor) throws IOException {
    String src = read();
    int at = src.indexOf("@if(" + accessor + " == null");
    if (at < 0) {
      return "";
    }
    return src.substring(at, src.indexOf("@endif", at));
  }

  /** 이스케이프를 푼 값. 없으면 빈 문자열이라 단언이 실패한다. */
  private String value(String path) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(Files.readString(Path.of(path), StandardCharsets.UTF_8)));
    return properties.getProperty(KEY, "");
  }
}
