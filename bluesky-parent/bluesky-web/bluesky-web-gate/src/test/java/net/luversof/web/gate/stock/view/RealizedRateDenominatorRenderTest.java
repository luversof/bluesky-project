package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.controller.StockTradeHtmxController.AccountRealizedRow;
import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 실현손익률의 <b>분모</b>가 매도 원가 식을 그대로 쓰는지 렌더해서 본다.
 *
 * <p>매도 원가 = {@code 매도금액 − 증권거래세 − 기록된 실현손익} 이다. 기록된 실현손익은 증권사가 세금까지 뺀 뒤의 값이라 세금을 빼지 않으면 원가가 그만큼
 * 부풀고 수익률은 그만큼 낮게 보인다.
 *
 * <p><b>실제로 그 사고가 있었다</b> &mdash; 2026-08-22 실측: 이 화면만 세금을 빠뜨려 한국투자증권 위탁 1,825,097 원 · ISA 60,870
 * 원(합 1,885,967 원)만큼 매도 원가가 과대 계상됐다.
 *
 * <p>그런데 그 식이 <b>두 곳에 따로</b> 있다 &mdash; 계좌 표는 {@code StockTradeHtmxController} 가 Java 로 계산해 {@code
 * AccountRealizedRow.soldCost} 로 넘기고, 종목 표는 <b>템플릿이 직접</b> ({@code
 * item.totalSellAmount().subtract(itemSellTax).subtract(itemNet)}) 만든다.
 *
 * <p>불변식 {@code 매매 화면의 두 실현손익 표가 같다} 는 <b>api-stock 의 데이터</b>를 같은 식으로 재계산해 축끼리 맞춰 볼 뿐, 게이트의 두 구현이 그
 * 식을 계속 쓰는지는 보지 않는다. 그래서 템플릿 쪽에서 다시 세금을 빠뜨려도 아무 검사도 깨지지 않는다. 그 구멍을 여기서 막는다.
 */
class RealizedRateDenominatorRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/trade/tradeRealizedSections.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 종목 표가 읽는 값만 채운 행. */
  private static TradeProfit stock(
      String name, String buyAmount, String sellAmount, String sellTax, String realized) {
    return new TradeProfit(
        UUID.fromString("00000000-0000-0000-0000-000000000009"),
        name,
        null,
        null,
        bd(buyAmount),
        BigDecimal.ZERO,
        0,
        BigDecimal.ZERO,
        bd(sellAmount),
        bd(realized),
        0,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(sellTax),
        bd(buyAmount),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        bd(realized),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        LocalDate.parse("2026-08-19"));
  }

  private String render(List<TradeProfit> stocks, List<AccountRealizedRow> accounts) {
    Map<String, Object> model = new HashMap<>();
    model.put("accountRealizedList", accounts);
    model.put("stockRealizedList", stocks);
    model.put("realizedWinCount", 1L);
    model.put("realizedStockCount", stocks.size());
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put("accountLabel", "계좌");
    model.put("stockLabel", "종목");
    model.put("realizedProfitLabel", "실현손익");

    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  @Test
  void 종목_실현손익률의_분모가_매도금액에서_세금과_실현손익을_뺀_값이다() {
    // 매도 1,000,000 · 거래세 3,000 · 기록 실현손익 97,000
    //   원가 = 1,000,000 − 3,000 − 97,000 = 900,000  ->  97,000 / 900,000 = +10.78%
    //   세금을 빠뜨리면 원가 903,000        ->  97,000 / 903,000 = +10.74%
    String html = render(List.of(stock("가종목", "900000", "1000000", "3000", "97000")), List.of());

    assertThat(html).as("종목 행을 그리지 못했다 - 검사가 무력해진다").contains("가종목");
    assertThat(html)
        .as("매도 원가에서 증권거래세를 빠뜨리면 수익률이 낮게 보인다 (실측 사고: 원가 1,885,967원 과대)")
        .contains("+10.78%")
        .doesNotContain("+10.74%");
    // 행이 내보내는 원가 값도 같은 식이어야 브라우저 선택 합계가 맞는다.
    assertThat(html).contains("data-sold-cost=\"900000\"");
  }

  /**
   * 두 표가 <b>매도원가</b>를 보여 주고, 매수금액은 보여 주지 않는다.
   *
   * <p>이 절의 다른 값은 전부 '판 것' 기준인데 매수금액만 '산 것 전체' 기준이었다. 그래서 두 표를 전체 선택해 합계를 비교하면 매수금액만 서로 달랐다(실측
   * 2026-08-24: 계좌별 1,779,858,067 vs 종목별 1,658,970,083, 차이 120,887,984 = 6.79%. 매도금액·실현손익·거래세는 정확히
   * 일치한다).
   *
   * <p>같은 종목을 한 계좌에서만 팔고 다른 계좌에선 들고 있으면 어떤 필터로도 두 값을 맞출 수 없다 &mdash; 계좌 쪽으로 좁히면 이번엔 -148,810,763 만큼
   * 벌어진다. 그래서 맞추는 대신, 옆 칸 수익률의 실제 분모인 매도원가로 바꿨다.
   */
  @Test
  void 두_표가_매도원가를_보여_주고_매수금액은_보여_주지_않는다() {
    // 매도 1,000,000 · 거래세 3,000 · 실현 97,000 -> 매도원가 900,000. 매수금액은 일부러 다른 값(900,001)
    // 으로 둬, 매수금액이 그려지면 검사가 알아채게 한다.
    String html =
        render(
            List.of(stock("가종목", "900001", "1000000", "3000", "97000")),
            List.of(
                new AccountRealizedRow(
                    "가계좌",
                    bd("1000000"),
                    bd("97000"),
                    bd("900000"),
                    bd("97000"),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"))));

    assertThat(html).as("두 표를 그리지 못했다 - 검사가 무력해진다").contains("가종목").contains("가계좌");
    assertThat(html)
        .as("매도원가 열이 없다 - 수익률의 분모가 화면에 없어진다")
        .contains(MessageUtil.getMessage("stock.realized.column.sold.cost"));
    assertThat(html)
        .as("두 표가 서로 다른 모집단을 재는 매수금액을 다시 보여 준다")
        .doesNotContain(MessageUtil.getMessage("stock.trade.label.buy.amount"))
        .doesNotContain("900,001");
    // 두 표 모두 같은 매도원가 900,000 을 그린다.
    assertThat(html.split("900,000", -1).length - 1)
        .as("두 표 중 한쪽만 매도원가를 그렸다")
        .isGreaterThanOrEqualTo(2);
  }

  @Test
  void 계좌_실현손익률도_같은_분모를_쓴다() {
    // 계좌 표는 컨트롤러가 계산한 soldCost 를 그대로 쓴다. 같은 입력이면 같은 비율이 나와야 한다.
    AccountRealizedRow account =
        new AccountRealizedRow(
            "가계좌",
            bd("1000000"),
            bd("97000"),
            bd("900000"),
            bd("97000"),
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    String html = render(List.of(), List.of(account));

    assertThat(html).as("계좌 행을 그리지 못했다").contains("가계좌");
    assertThat(html).as("계좌 표와 종목 표의 수익률 정의가 갈라졌다").contains("+10.78%");
  }
}
