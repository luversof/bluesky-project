package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.luversof.web.gate.stock.util.StockFormatUtil;
import net.luversof.web.gate.stock.util.StockHoldingReturnUtil;

/**
 * 종목 상세는 합산 손익의 <b>세 몫에도 비율</b>을 붙이고, 기간과 무관한 <b>보유 기간 · 연평균 수익률</b>을 함께 적는다.
 *
 * <p>사용자 요청 2026-09-17: "종목 상세 조회에서 보유 손익 + 배당 손익 표시(수익률 퍼센트도), 보유 기간 대비 손익 표시". 화면에는 합산 손익
 * +300.61% 하나만 비율이 있었고 평가 변동 · 실현 · 배당은 금액뿐이었다. 보유 기간과 연평균은 자산 현황의 종목 줄에만 있었다.
 *
 * <p>세 비율은 합산 비율과 같은 분모('넣어 둔 돈' = 기초 평가액 + max(기간 순유입, 0), api-stock 과 같은 정의)라 더하면 합산 비율이 된다. 연평균은
 * 자산 현황과 <b>같은 계산 · 같은 입력</b>이다(사용자 결정 2026-09-17: 연환산 수익률, 이어서 복리 환산 대신 XIRR &mdash; 매수 · 매도 · 배당이
 * 오간 날짜와 지금 평가액) &mdash; 두 화면이 다른 식을 쓰면 같은 종목이 두 연평균을 갖는다.
 */
class ItemDetailHoldingReturnTest {

  private static final String CONTENT = "stock/htmx/stockItemDetailContent.jte";

  private static final LocalDate TODAY = LocalDate.parse("2026-09-17");

  /** 2026-09-17 삼성전자 실측값('전체' 기간). */
  private static final BigDecimal EVALUATION_AMOUNT = new BigDecimal("1278400500");

  private static final BigDecimal EVALUATION_PROFIT = new BigDecimal("915875421");

  private static final BigDecimal REALIZED = new BigDecimal("138569333");

  private static final BigDecimal DIVIDEND = new BigDecimal("35340449");

  private static final BigDecimal CAPITAL_BASE = new BigDecimal("362525079");

  /**
   * 같은 원가를 반씩 2020-03-04 · 2024-03-04 에 넣었다고 친 흐름. XIRR 28.5%(파이썬 이분법으로 따로 계산) - 한 번에 넣었다면 21.2%.
   */
  private static final List<net.luversof.web.gate.stock.dto.response.StockCashFlowResponse>
      TWO_TRANCHES =
          List.of(
              new net.luversof.web.gate.stock.dto.response.StockCashFlowResponse(
                  LocalDate.parse("2020-03-04"), new BigDecimal("-181262539.5")),
              new net.luversof.web.gate.stock.dto.response.StockCashFlowResponse(
                  LocalDate.parse("2024-03-04"), new BigDecimal("-181262539.5")));

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

  private static Map<String, Object> model(
      LocalDate firstBuy, int quantity, BigDecimal capitalBase) {
    BigDecimal combined = EVALUATION_PROFIT.add(REALIZED).add(DIVIDEND);
    Map<String, Object> model = new HashMap<>();
    model.put("stockItem", new StockItem(UUID.randomUUID(), "005930", "SAMSUNG", "KRX", List.of()));
    model.put("holdingQuantity", quantity);
    model.put("evaluationAmount", EVALUATION_AMOUNT);
    model.put("evaluationProfit", EVALUATION_PROFIT);
    model.put("realizedProfit", REALIZED);
    model.put("totalDividend", DIVIDEND);
    model.put("periodUnrealizedDelta", EVALUATION_PROFIT);
    model.put("periodProfitRatePct", combined.doubleValue() * 100.0 / CAPITAL_BASE.doubleValue());
    model.put("periodCapitalBase", capitalBase);
    model.put(
        "holdingReturn",
        StockHoldingReturnUtil.of(
            firstBuy, TODAY, TWO_TRANCHES, EVALUATION_AMOUNT, EVALUATION_PROFIT, DIVIDEND));
    model.put("holdingFirstBuyDate", firstBuy);
    return model;
  }

  private static String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CONTENT, model, output);
    return output.toString();
  }

  private static double pct(String text) {
    return Double.parseDouble(
        text.replace("%", "").replace("+", "").replace("(", "").replace(")", "").trim());
  }

  @Test
  void 세_몫의_비율을_더하면_합산_비율이다() {
    String html = render(model(LocalDate.parse("2020-03-04"), 5043, CAPITAL_BASE));

    Matcher matcher =
        Pattern.compile("data-component-rate=\"(unrealized|realized|dividend)\">([^<]+)</span>")
            .matcher(html);
    Map<String, Double> rates = new HashMap<>();
    while (matcher.find()) {
      rates.put(matcher.group(1), pct(matcher.group(2)));
    }
    assertThat(rates).as("세 몫 모두 비율이 붙는다").containsOnlyKeys("unrealized", "realized", "dividend");
    assertThat(rates.get("dividend"))
        .as("배당 몫 = 배당 / 넣어 둔 돈")
        .isCloseTo(9.75, org.assertj.core.data.Offset.offset(0.01));

    Matcher total =
        Pattern.compile("text-lg font-bold [^\"]*\">\\s*([+-][0-9.]+%)\\s*</span>").matcher(html);
    assertThat(total.find()).as("합산 비율을 못 찾았다").isTrue();
    double sum = rates.values().stream().mapToDouble(Double::doubleValue).sum();
    assertThat(sum)
        .as("셋의 합이 합산 비율과 같아야 화면에서 검산된다")
        .isCloseTo(pct(total.group(1)), org.assertj.core.data.Offset.offset(0.02));
  }

  @Test
  void 분모가_없으면_몫_비율을_적지_않는다() {
    assertThat(render(model(LocalDate.parse("2020-03-04"), 5043, null)))
        .doesNotContain("data-component-rate");
  }

  /** 보유 기간 · 연평균 카드(2026-09-17 합산 상자 아래 작은 줄에서 카드로 옮김, 사용자 선택) - 표식부터 보조줄 끝까지. */
  private static String holdingLine(String html) {
    int at = html.indexOf("data-holding-return");
    assertThat(at).as("보유 기간 카드가 없다").isGreaterThan(0);
    int sub = html.indexOf("stat-card-sub", at);
    assertThat(sub).as("보유 기간 카드의 보조줄이 없다").isGreaterThan(at);
    return html.substring(at, html.indexOf("</div>", sub));
  }

  @Test
  void 보유_기간과_연평균을_자산_현황과_같은_계산으로_적는다() {
    LocalDate firstBuy = LocalDate.parse("2020-03-04");
    Map<String, Object> model = model(firstBuy, 5043, CAPITAL_BASE);
    StockHoldingReturnUtil.Row row = (StockHoldingReturnUtil.Row) model.get("holdingReturn");
    String line = holdingLine(render(model));

    assertThat(line)
        .contains("data-short-term=\"false\"")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.status.cell.holding.years"), "6", "6"))
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.status.cell.holding.since"), "2020-03-04"))
        .as("카드 값은 비율만 적는다(라벨이 연평균 수익률)")
        .contains(">" + StockFormatUtil.signedPct(row.annualizedPct().doubleValue(), 1) + "</div>")
        .contains(MessageUtil.getMessage("stock.item.detail.holding.basis"));
    assertThat(row.annualizedPct())
        .as("나중에 넣은 절반은 2024-03-04 부터 센다 - 한 번에 넣었다고 보면 21.2%")
        .isEqualByComparingTo("28.5");
  }

  @Test
  void 일년_미만_보유는_무엇을_환산했는지_밝힌다() {
    String line = holdingLine(render(model(LocalDate.parse("2026-05-07"), 249, CAPITAL_BASE)));

    assertThat(line)
        .contains("data-short-term=\"true\"")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.asset.status.cell.annualized.short"), "133"));
  }

  @Test
  void 보유하지_않거나_최초_매수일을_모르면_보유_기간을_적지_않는다() {
    assertThat(render(model(LocalDate.parse("2020-03-04"), 0, CAPITAL_BASE)))
        .doesNotContain("data-holding-return");
    assertThat(render(model(null, 5043, CAPITAL_BASE))).doesNotContain("data-holding-return");
  }

  private static String flatten(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
  }

  /** 자산 현황과 같은 입력이라야 두 화면의 연평균이 같다 - 흐름은 종목별 하루치 순현금흐름, 끝값은 지금 평가액, 배당은 종목별 누적(세후) 합계. */
  @Test
  void 컨트롤러가_자산_현황과_같은_입력을_쓴다() throws IOException {
    String controller =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java");
    assertThat(controller)
        .contains("tradeClient.findFirstBuyDateByStockItem(holdingReturnParams)")
        .contains("dividendClient.findDividendTotalByStockItem(holdingReturnParams)")
        .contains("tradeClient.findCashFlowsByStockItem(holdingReturnParams)")
        .contains(
            "holdingReturnParams.add( \"timeZone\", net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone).getId());")
        .contains("StockHoldingReturnUtil.of( holdingFirstBuyDate,")
        .contains("holdingCashFlows, evaluationAmount, evaluationProfit, holdingDividendTotal));")
        .contains("\"periodCapitalBase\"");

    String assetStatus = flatten("src/main/jte/stock/htmx/fragments/assetStatus.jte");
    assertThat(assetStatus)
        .as("자산 현황이 같은 유틸을 쓰는지 - 한쪽만 바뀌면 두 화면이 갈린다")
        .contains(
            "StockHoldingReturnUtil.of(firstBuyDate, holdingBasisDate, cashFlowsByStockItem == null ? null : cashFlowsByStockItem.get(item.stockItemId()), item.evaluationAmount(), item.evaluationProfit(), breakdown.dividendTotal())");

    String portfolio =
        flatten(
            "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java");
    assertThat(portfolio)
        .as("자산 현황도 같은 조건(계좌 필터 · 존)으로 흐름을 받는다")
        .contains("tradeClient.findCashFlowsByStockItem(profitParams)")
        .contains("\"cashFlowsByStockItem\",");

    String shell = flatten("src/main/jte/stock/stockItemDetail.jte");
    assertThat(shell)
        .contains("periodCapitalBase = periodCapitalBase,")
        .contains("holdingReturn = holdingReturn,")
        .contains("holdingFirstBuyDate = holdingFirstBuyDate");
    List<String> missing = new ArrayList<>();
    for (String bundle : List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      if (!Files.readString(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8)
          .contains("stock.item.detail.holding.basis")) {
        missing.add(bundle);
      }
    }
    assertThat(missing).isEmpty();
  }
}
