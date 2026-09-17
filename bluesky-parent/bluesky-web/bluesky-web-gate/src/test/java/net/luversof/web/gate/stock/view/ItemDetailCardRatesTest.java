package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.response.StockCashFlowResponse;
import net.luversof.web.gate.stock.util.StockHoldingReturnUtil;

/**
 * 종목 상세 카드는 손익마다 <b>그 돈의 원금 대비</b> 비율을 적고, 보유 전체 연평균을 카드로 보여 준다.
 *
 * <p>사용자 요청 2026-09-17: "평가 손익, 실현 손익, 기간 배당이 있는데 수익률 퍼센트가 평가 손익에만 보인다", "보유 기간 대비 평균 연 수익률 표시도".
 * 고른 방식: 카드마다 그 원금 대비(평가 = 보유 원가 · 실현 = 판 주식 원가 · 배당 = 보유 원가) + 카드 줄에 연평균 카드. 세 기준이 달라 비율 옆에 기준을
 * 글자로 적는다. 합산 상자의 세 몫('넣어 둔 돈' 대비, 더하면 합산)과는 다른 숫자라 이름이 섞이면 안 된다.
 *
 * <p>실측 2026-09-17 TIGER 리츠부동산인프라(전체): 평가 -4,298,113 / 보유 원가 60,159,793 = -7.1% · 실현 874,593 / 판 주식
 * 원가 37,199,152 = +2.4% · 배당 5,587,497 / 보유 원가 = 9.3%.
 */
class ItemDetailCardRatesTest {

  private static final String CONTENT = "stock/htmx/stockItemDetailContent.jte";

  private static final LocalDate TODAY = LocalDate.parse("2026-09-17");

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREA);
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  private static String message(String key) {
    return MessageUtil.getMessage(key);
  }

  private static Map<String, Object> model(
      int quantity, String evaluationAmount, String evaluationProfit, String realizedCostBasis) {
    Map<String, Object> model = new HashMap<>();
    LocalDate firstBuy = LocalDate.parse("2025-10-20");
    model.put(
        "stockItem", new StockItem(UUID.randomUUID(), "329200", "TIGER REIT", "KRX", List.of()));
    model.put("holdingQuantity", quantity);
    model.put("evaluationAmount", new BigDecimal(evaluationAmount));
    model.put("evaluationProfit", new BigDecimal(evaluationProfit));
    model.put("realizedProfit", new BigDecimal("874593"));
    model.put("realizedCostBasis", new BigDecimal(realizedCostBasis));
    model.put("totalDividend", new BigDecimal("5587497"));
    model.put(
        "holdingReturn",
        StockHoldingReturnUtil.of(
            firstBuy,
            TODAY,
            List.of(new StockCashFlowResponse(firstBuy, new BigDecimal("-60159793"))),
            new BigDecimal(evaluationAmount),
            new BigDecimal(evaluationProfit),
            new BigDecimal("5587497")));
    model.put("holdingFirstBuyDate", firstBuy);
    return model;
  }

  private static Map<String, Object> held() {
    return model(13776, "55861680", "-4298113", "37199152");
  }

  private static String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CONTENT, model, output);
    return output.toString();
  }

  /** 라벨이 든 카드 한 장(라벨부터 그 카드 보조줄 끝까지). */
  private static String card(String html, String label) {
    int at = html.indexOf("stat-card-label\">" + label);
    assertThat(at).as(label + " 카드를 못 찾았다").isGreaterThan(0);
    int sub = html.indexOf("stat-card-sub", at);
    int next = html.indexOf("stat-card-label", at + 1);
    return html.substring(
        at,
        sub > 0 && (next < 0 || sub < next)
            ? html.indexOf("</div>", sub)
            : (next < 0 ? html.length() : next));
  }

  @Test
  void 세_카드가_각자의_원금_대비_비율과_기준을_적는다() {
    String html = render(held());

    assertThat(card(html, message("stock.profit.unrealized")))
        .contains("-7.1% · " + message("stock.item.detail.rate.basis.holding"));
    assertThat(card(html, message("stock.profit.realized")))
        .as("874,593 / 37,199,152 = 2.35% - 넣어 둔 돈 대비(+1.45%)가 아니다")
        .contains("+2.4% · " + message("stock.item.detail.rate.basis.sold"))
        .contains("text-profit");
    assertThat(card(html, message("stock.item.detail.period.dividend")))
        .as("5,587,497 / 60,159,793 = 9.29% - 배당은 음수가 없어 부호를 안 붙인다")
        .contains(">9.3% · " + message("stock.item.detail.rate.basis.holding"));
  }

  @Test
  void 판_적이_없으면_실현_비율_대신_사유를_적고_0_을_이득색으로_칠하지_않는다() {
    Map<String, Object> model = held();
    model.put("realizedProfit", BigDecimal.ZERO);
    model.put("realizedCostBasis", BigDecimal.ZERO);
    String realized = card(render(model), message("stock.profit.realized"));

    assertThat(realized)
        .contains(message("stock.item.detail.rate.none.sold"))
        .doesNotContain("%")
        .doesNotContain("text-profit");
  }

  @Test
  void 보유하지_않으면_배당_비율_대신_사유를_적는다() {
    String html = render(model(0, "0", "0", "37199152"));

    assertThat(card(html, message("stock.item.detail.period.dividend")))
        .contains(message("stock.item.detail.rate.none.holding"))
        .doesNotContain("%");
    assertThat(html).as("보유가 없으면 보유 기간 · 연평균 카드도 없다").doesNotContain("data-holding-return");
  }

  @Test
  void 연평균은_보유_전체_기준_카드로_보이고_합산_상자에는_상쇄율만_남는다() {
    String html = render(held());
    int cardAt = html.indexOf("data-holding-return");
    assertThat(cardAt).as("연평균 카드가 없다").isGreaterThan(0);
    String card =
        html.substring(
            html.lastIndexOf("<div", cardAt),
            html.indexOf("</div>", html.indexOf("stat-card-sub", cardAt)));

    StockHoldingReturnUtil.Row row = (StockHoldingReturnUtil.Row) held().get("holdingReturn");
    assertThat(card)
        .contains("class=\"contents\"")
        .contains(message("stock.asset.status.cell.annualized.name"))
        .as("기간 선택과 무관하다는 것을 낭독기에도 알린다")
        .contains(
            "<span class=\"sr-only\"> " + message("stock.item.detail.holding.basis") + "</span>")
        .contains(
            ">"
                + net.luversof.web.gate.stock.util.StockFormatUtil.signedPct(
                    row.annualizedPct().doubleValue(), 1)
                + "</div>")
        .contains(
            java.text.MessageFormat.format(
                message("stock.asset.status.cell.annualized.short"), "332"));
    assertThat(cardAt)
        .as("카드 줄(기간 배당 카드 뒤)에 있다")
        .isGreaterThan(
            html.indexOf("stat-card-label\">" + message("stock.item.detail.period.dividend")));

    int box = html.indexOf("data-combined-breakdown");
    int cards = html.indexOf("stat-card-label");
    assertThat(html.substring(box, cards))
        .as("합산 상자에서 보유 기간 줄은 빠졌다")
        .doesNotContain("data-holding-period")
        .contains("data-holding-coverage-line");
  }

  private static String flatten(String path) throws java.io.IOException {
    return java.nio.file.Files.readString(
            java.nio.file.Path.of(path), java.nio.charset.StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ");
  }

  /** 분모는 실현손익과 같은 기간 행(profits)의 매도에서 만든다 - 전체 기간 스냅샷에서 만들면 기간을 좁혔을 때 분자와 분모가 다른 매도를 말한다. */
  @Test
  void 컨트롤러가_같은_기간_매도에서_분모를_만든다() throws java.io.IOException {
    assertThat(
            flatten(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"))
        .contains(
            "BigDecimal realizedProfit = sumTradeProfit(profits, TradeProfit::realizedProfit);")
        .contains(
            "BigDecimal periodSellAmount = sumTradeProfit(profits, TradeProfit::totalSellAmount);")
        .contains(
            "periodSellAmount.signum() > 0 ? periodSellAmount.subtract(realizedProfit) : BigDecimal.ZERO;")
        .contains("model.addAttribute(\"realizedCostBasis\", realizedCostBasis);");
    assertThat(flatten("src/main/jte/stock/stockItemDetail.jte"))
        .contains("realizedCostBasis = realizedCostBasis");
  }
}
