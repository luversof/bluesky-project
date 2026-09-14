package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 배당 표의 세금 0 은 <b>모든 표에서 같게</b> 적는다.
 *
 * <p>배당 이력({@code dividendTable}) 과 수익률({@code dividendYieldAnalytics}) 표는 2026-09-08 부터 0 을 "-" 로
 * 적고 왜 0 인지를 함께 단다. 그런데 <b>상세 두 화면</b>(종목 · 계좌)의 배당 표만 "0" 그대로였다 - 실측 2026-09-13: 배당 202 건 중 <b>147
 * 건(72.8%)</b>이 세금 0 이라, 같은 배당이 화면마다 다르게 보였다.
 *
 * <p>사유는 {@code title} 과 {@code aria-label} 에 <b>함께</b> 단다. title 에만 달면 마우스 전용이 된다 - 이 저장소에서 두 번
 * 저지른 실수라 규칙으로 굳혔다.
 */
class DetailDividendZeroTaxTest {

  private static final String ITEM = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String ACCOUNT = "src/main/jte/stock/htmx/accountDetailContent.jte";
  private static final String SIBLING =
      "src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte";
  private static final String KEY = "stock.dividend.zero.amount.title";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 0 인지 먼저 판단한다 - null 도 0 으로 본다(API 가 null 을 줄 수 있다). */
  @Test
  void 두_화면이_세금_0_을_판단한다() throws IOException {
    String expected =
        "!{ boolean dividendTaxZero = dividend.tax() == null || dividend.tax().signum() == 0; }";

    assertThat(read(ITEM)).contains(expected);
    assertThat(read(ACCOUNT)).contains(expected);
  }

  /** 0 이면 "-", 아니면 금액. */
  @Test
  void 세금_0_은_대시로_적는다() throws IOException {
    String expected =
        "${dividendTaxZero ? \"-\" : String.format(\"%,d\","
            + " StockFormatUtil.displayWon(dividend.tax()))}";

    assertThat(read(ITEM)).contains(expected);
    assertThat(read(ACCOUNT)).contains(expected);
  }

  /** 사유가 마우스 전용이 되면 안 된다 - title 과 aria-label 둘 다. */
  @Test
  void 사유를_보조기술도_읽는다() throws IOException {
    String title = "title=\"${dividendTaxZero ? zeroAmountTitle : null}\"";
    String aria = "aria-label=\"${dividendTaxZero ? zeroAmountTitle : null}\"";

    assertThat(read(ITEM)).contains(title).contains(aria);
    assertThat(read(ACCOUNT)).contains(title).contains(aria);
  }

  /** 사유 문구는 형제 표와 같은 키를 쓴다 - 따로 만들면 다시 갈린다. */
  @Test
  void 형제_표와_같은_키를_쓴다() throws IOException {
    String decl = "String zeroAmountTitle = MessageUtil.getMessage(\"" + KEY + "\");";

    assertThat(read(ITEM)).contains(decl);
    assertThat(read(ACCOUNT)).contains(decl);
    assertThat(read(SIBLING)).as("형제 표도 같은 키를 쓴다").contains(KEY);
  }

  /** 0 이 아닌 세금은 그대로 금액으로 적는다. */
  @Test
  void 세금이_있으면_금액_그대로() throws IOException {
    assertThat(read(ITEM)).contains("StockFormatUtil.displayWon(dividend.tax())");
    assertThat(read(ACCOUNT)).contains("StockFormatUtil.displayWon(dividend.tax())");
  }
}
