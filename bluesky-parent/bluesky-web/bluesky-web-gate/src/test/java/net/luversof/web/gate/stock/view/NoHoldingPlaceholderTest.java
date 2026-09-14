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
 * 보유가 없을 때 <b>0 을 잰 값처럼 적지 않는다</b>.
 *
 * <p>실측 2026-09-13.
 *
 * <ul>
 *   <li>종목 상세 '평균 단가': 86 종목 중 <b>77 개</b>(전량 매도 34 · 무거래 43)가 "0" 이었다. NAVER 는 바로 옆 현재가가 210,000
 *       이라 0 원에 산 것처럼 읽혔다.
 *   <li>'평가 손익' 옆 비율: 원가가 0 이라 {@code 0 / 0} 인데 삼항 폴백이 0.0 으로 떨어져 "0 0.0%" 가 나왔다. 종목 77 개와 계좌 1
 *       개(동양증권 - 기간 매수 275,457,500 · 실현 +33,095,880)가 해당한다.
 * </ul>
 *
 * <p>금액 0 은 맞다(보유가 없으니 평가액도 0). 고치는 것은 <b>모르는 값을 0 으로 적는 것</b>뿐이다.
 */
class NoHoldingPlaceholderTest {

  private static final String ITEM = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String ACCOUNT = "src/main/jte/stock/htmx/accountDetailContent.jte";
  private static final String KEY = "stock.detail.value.no.holding";
  private static final String KO = "src/main/resources/uiMessage_ko.properties";
  private static final String EN = "src/main/resources/uiMessage.properties";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 원가가 0 이면 비율을 모른다 - 두 화면 모두 그 사실을 판단한 뒤 쓴다. */
  @Test
  void 두_화면이_비율을_아는지_먼저_판단한다() throws IOException {
    assertThat(read(ITEM)).contains("boolean evalRateKnown = evalCostBasis.signum() != 0;");
    assertThat(read(ACCOUNT)).contains("boolean evalRateKnown = evalCostBasis.signum() != 0;");
  }

  /** 모르면 보조줄을 비운다 - statCard 는 빈 sub 를 아예 그리지 않는다. */
  @Test
  void 모르는_비율은_적지_않는다() throws IOException {
    String expected =
        "sub = evalRateKnown ? StockFormatUtil.signedPct(evalProfitRatePct, 1) : \"\",";

    assertThat(read(ITEM)).contains(expected);
    assertThat(read(ACCOUNT)).contains(expected);
  }

  /** 알면 예전 그대로 적는다 - 값이 있는 화면까지 비우면 정보가 준다. */
  @Test
  void 아는_비율은_그대로_적는다() throws IOException {
    assertThat(read(ITEM)).contains("StockFormatUtil.signedPct(evalProfitRatePct, 1)");
    assertThat(read(ACCOUNT)).contains("StockFormatUtil.signedPct(evalProfitRatePct, 1)");
  }

  /** 보유가 없으면 평균 단가 자리에 문구를 넣는다. 금액이 아니므로 금액 숨김도 걸지 않는다. */
  @Test
  void 보유가_없으면_평균_단가에_문구를_넣는다() throws IOException {
    String branch = noHoldingBranch();

    assertThat(branch).contains("MessageUtil.getMessage(\"" + KEY + "\")");
    assertThat(branch).contains("amount = false");
    assertThat(branch).doesNotContain("StockFormatUtil.displayWon(averageBuyPrice)");
  }

  /** 보유가 있으면 금액 그대로. */
  @Test
  void 보유가_있으면_금액_그대로_적는다() throws IOException {
    String jte = read(ITEM);
    int at = jte.indexOf("@else", jte.indexOf("@if(holdingQuantity <= 0)"));

    assertThat(at).isPositive();
    assertThat(jte.substring(at, jte.indexOf("@endif", at)))
        .contains("String.format(\"%,d\", StockFormatUtil.displayWon(averageBuyPrice))");
  }

  /** 문구는 보유가 없다는 뜻이어야 한다 - ko 값은 이스케이프라 디코드해서 본다. */
  @Test
  void 문구가_두_로케일에_있다() throws IOException {
    assertThat(value(KO)).as("한글 값").contains("보유").contains("없");
    assertThat(value(EN).toLowerCase()).as("영문 값").contains("holding");
  }

  /** '보유 없음' 가지 한 덩어리. 파일 전체로 보면 @else 쪽 카드와 섞인다. */
  private String noHoldingBranch() throws IOException {
    String jte = read(ITEM);
    int at = jte.indexOf("@if(holdingQuantity <= 0)");
    if (at < 0) {
      return "";
    }
    return jte.substring(at, jte.indexOf("@else", at));
  }

  /** 이스케이프를 푼 값. 없으면 빈 문자열이라 단언이 실패한다. */
  private String value(String path) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(Files.readString(Path.of(path), StandardCharsets.UTF_8)));
    return properties.getProperty(KEY, "");
  }
}
