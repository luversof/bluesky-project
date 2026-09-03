package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
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
import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.util.StockTradePeriodUtil;
import net.luversof.web.gate.stock.util.StockTradePeriodUtil.TradePeriod;

/**
 * 매매 화면의 <b>달/해 단위 쪼갬</b> 표를 렌더해서 본다.
 *
 * <p>매매 화면은 고른 기간 전체를 카드 몇 장으로 답하고 그 아래는 거래 하나하나였다. 그 사이가 비어 있어 "어느 해에 얼마나 사고팔았나" 는 월별 매매 금액 막대 차트를
 * 눈으로 훑어야만 알 수 있었다 &mdash; 차트는 모양은 주지만 수를 주지 않는다.
 */
class TradePeriodBreakdownRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/trade/tradePeriodBreakdown.jte";
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

  private static TradeResponse trade(
      String date, TradeType type, String amount, String fee, String tax, String realized) {
    return new TradeResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "테스트종목",
        type,
        1,
        bd("1000"),
        bd(fee),
        bd(tax),
        bd(amount),
        realized == null ? null : bd(realized),
        LocalDate.parse(date).atTime(9, 0).atZone(KST).toInstant());
  }

  /** 판 달과 사기만 한 달. 실데이터에도 둘 다 있다. */
  private static List<TradeResponse> trades() {
    return List.of(
        trade("2026-08-27", TradeType.SELL, "3000000", "3000", "1800", "500000"),
        trade("2026-08-03", TradeType.BUY, "1000000", "1000", "0", null),
        trade("2026-07-15", TradeType.BUY, "2000000", "2000", "0", null));
  }

  private String render(List<TradePeriod> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("tradePeriods", rows);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  private String renderTrades(List<TradeResponse> trades) {
    return render(StockTradePeriodUtil.of(trades, KST));
  }

  @Test
  void 구간마다_건수와_매수_매도와_비용을_한_줄로_모은다() {
    String html = renderTrades(trades());

    assertThat(html)
        .as("카드는 기간 전체만 답하고 목록은 거래 하나하나라 그 사이가 비어 있었다")
        .contains("data-trade-breakdown=\"MONTH\"")
        .contains("2026-08")
        .contains("2026-07");

    int august = html.indexOf("2026-08");
    int july = html.indexOf("2026-07");
    String augustRow = html.substring(august, july);
    assertThat(augustRow)
        .as("매수 1,000,000 / 매도 3,000,000 / 수수료 3,000+1,000 / 거래세 1,800")
        .contains("1,000,000")
        .contains("3,000,000")
        .contains("4,000")
        .contains("1,800");
  }

  /** 판 것이 없는 구간은 실현손익이 0 이 아니라 '없음' 이다. 0 원과 뜻이 다르다. */
  @Test
  void 판_것이_없으면_실현손익을_적지_않는다() {
    String html = renderTrades(trades());
    String julyRow = html.substring(html.indexOf("2026-07"));

    assertThat(html.substring(html.indexOf("2026-08"), html.indexOf("2026-07")))
        .as("판 달에는 실현손익을 적는다")
        .contains("+&#8361;500,000");
    assertThat(julyRow).as("사기만 한 달에 0 원을 적으면 본전이라는 뜻이 된다").doesNotContain("&#8361;0<");
  }

  /**
   * 표 이름을 자산 성장의 성과 표와 <b>같은 결</b>로 맞춘다.
   *
   * <p>두 화면이 같은 단위를 다른 이름으로 부르면 나란히 놓고 견줄 수 없다. 그래서 같은 메시지 키를 쓴다.
   */
  @Test
  void 달_단위면_월별_성과_해_단위면_연도별_성과라_부른다() {
    assertThat(renderTrades(trades()))
        .contains(MessageUtil.getMessage("stock.asset.growth.monthly.title"));

    List<TradeResponse> longSpan =
        List.of(
            trade("2026-08-27", TradeType.SELL, "3000000", "3000", "1800", "500000"),
            trade("2020-01-06", TradeType.BUY, "1000000", "1000", "0", null));
    assertThat(renderTrades(longSpan))
        .contains(MessageUtil.getMessage("stock.asset.growth.yearly.title"))
        .contains("data-trade-breakdown=\"YEAR\"");
  }

  /** 구간이 하나뿐이면 위의 요약 카드를 그대로 되풀이할 뿐이라 그리지 않는다. */
  @Test
  void 구간이_하나뿐이면_그리지_않는다() {
    String html =
        renderTrades(List.of(trade("2026-08-27", TradeType.BUY, "1000000", "1000", "0", null)));

    assertThat(html.trim()).isEmpty();
  }

  @Test
  void 자료가_없으면_아무것도_그리지_않는다() {
    assertThat(render(List.of()).trim()).isEmpty();
  }

  /** 조각만 만들고 화면에 붙이지 않으면 없는 것과 같다. */
  @Test
  void 매매_화면이_표를_부른다() throws IOException {
    String page =
        Files.readString(Path.of("src/main/jte/stock/htmx/tradeList.jte"), StandardCharsets.UTF_8);

    assertThat(page)
        .as("조각만 만들고 화면에 붙이지 않으면 없는 것과 같다")
        .contains("tradePeriodBreakdown")
        .as("화면이 이미 받아 둔 전체 매매 목록으로 집계해야 원격을 한 번 더 부르지 않는다")
        .contains("StockTradePeriodUtil.of(allTradeList, zone)");
  }

  /**
   * 합계 줄. 여덟 값이 모두 그냥 더하면 되는 값이다.
   *
   * <p>연도별 세금·비용 표와 <b>같은 메시지 키</b>를 써서 두 표가 같은 줄을 다른 이름으로 부르지 않게 한다.
   */
  @Test
  void 합계_줄에_모든_구간을_더한다() {
    String html = renderTrades(trades());

    assertThat(html)
        .contains("data-trade-breakdown-total")
        .contains(MessageUtil.getMessage("stock.asset.growth.cost.sum.row"));

    String foot = html.substring(html.indexOf("data-trade-breakdown-total"));
    assertThat(foot)
        .as("매수 1,000,000 + 2,000,000 = 3,000,000 / 매도 3,000,000")
        .contains("3,000,000")
        .as("수수료 3,000 + 1,000 + 2,000 = 6,000")
        .contains("6,000")
        .as("실현손익은 판 달의 500,000 하나뿐이다")
        .contains("+&#8361;500,000");
  }

  /** 고른 기간에 판 것이 하나도 없으면 합계의 실현손익도 0 이 아니라 '없음' 이다. */
  @Test
  void 판_것이_하나도_없으면_합계에도_실현손익을_적지_않는다() {
    String html =
        renderTrades(
            List.of(
                trade("2026-08-27", TradeType.BUY, "1000000", "1000", "0", null),
                trade("2026-07-15", TradeType.BUY, "2000000", "2000", "0", null)));

    String foot = html.substring(html.indexOf("data-trade-breakdown-total"));
    assertThat(foot)
        .as("한 번도 안 판 기간에 0 원을 적으면 본전이라는 뜻이 된다")
        .doesNotContain("text-profit")
        .doesNotContain("text-loss");
    assertThat(foot).contains("<span class=\"text-base-content/30\">-</span>");
  }

  /**
   * 합계 줄도 <b>줄과 같은 규칙</b>으로 0 을 적는다.
   *
   * <p>처음에는 합계만 금액을 그대로 찍어 거래세 0 이 &#8361;0 으로 나갔다. 위 줄들은 모두 '-' 인데 마지막 줄만 0 원이라 한 열이 위아래로 다르게 읽혔다.
   */
  @Test
  void 합계_줄도_0_인_열은_금액_대신_줄표를_쓴다() {
    // 매수만 한 기간이라 매도 금액과 증권거래세가 0 이다.
    String html =
        renderTrades(
            List.of(
                trade("2026-08-27", TradeType.BUY, "1000000", "1000", "0", null),
                trade("2026-07-15", TradeType.BUY, "2000000", "2000", "0", null)));

    String foot = html.substring(html.indexOf("data-trade-breakdown-total"));
    assertThat(foot).as("줄은 '-' 인데 합계만 0 원이면 한 열이 위아래로 다르게 읽힌다").doesNotContain("&#8361;0<");
    assertThat(foot).as("0 이 아닌 열은 그대로 적는다 - 수수료 1,000 + 2,000").contains("3,000");
  }
}
