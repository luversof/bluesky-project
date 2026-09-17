package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import net.luversof.web.gate.stock.controller.StockPortfolioHtmxControllerAccess;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.response.AssetStatusStockAccountView;
import net.luversof.web.gate.stock.util.StockCombinedProfitUtil;

/**
 * 자산 현황 '종목별 현황' 의 종목 줄에서 그 종목을 가진 계좌를 펼쳐 본다.
 *
 * <p>사용자 요청 2026-09-17: "종목별 현황에도 보유 계좌 보기 기능이 있으면 좋겠다(계좌에 종목 보기 기능이 있는 것처럼)". 계좌 표 펼침과 같은 계좌 x 종목
 * 손익 행을 뒤집어 모으므로 두 펼침의 수량 · 금액이 어긋나지 않는다. 모양 · 여닫기 · 이름 규칙도 계좌 표 펼침과 같다.
 */
class AssetStatusStockAccountsTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetStatus.jte";

  private static final String QUOTE = String.valueOf((char) 34);

  private static final UUID STOCK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  private static final UUID OTHER_STOCK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

  private static final UUID ISA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  private static final UUID BROKER = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

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

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 계좌 x 종목 손익 행 하나(계좌 표 펼침의 재료와 같은 모양). */
  private static TradeProfit holding(
      UUID account,
      String accountName,
      UUID stock,
      int quantity,
      String evaluation,
      String profit) {
    return new TradeProfit(
        stock,
        "SAMSUNG",
        account,
        accountName,
        null,
        bd("10000"),
        0,
        null,
        null,
        null,
        quantity,
        bd("11000"),
        bd(evaluation),
        bd(profit),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void 종목마다_가진_계좌를_평가액_순으로_모으고_종목_안_비중을_낸다() {
    Map<UUID, List<AssetStatusStockAccountView>> result =
        StockPortfolioHtmxControllerAccess.buildStockHoldingAccountViews(
            List.of(
                holding(ISA, "ISA", STOCK, 40, "440000", "40000"),
                holding(BROKER, "BROKER", STOCK, 60, "660000", "60000"),
                holding(BROKER, "BROKER", OTHER_STOCK, 10, "900000", "-100000"),
                holding(ISA, "ISA", OTHER_STOCK, 0, "0", "0")),
            bd("2000000"));

    assertThat(result.get(STOCK))
        .extracting(AssetStatusStockAccountView::accountName)
        .containsExactly("BROKER", "ISA");
    AssetStatusStockAccountView broker = result.get(STOCK).get(0);
    assertThat(broker.holdingQuantity()).isEqualTo(60);
    assertThat(broker.stockWeightPct()).as("660,000 / 1,100,000").isEqualByComparingTo("60.00");
    assertThat(broker.totalWeightPct()).as("660,000 / 2,000,000").isEqualByComparingTo("33.00");
    assertThat(result.get(OTHER_STOCK))
        .as("다 판 계좌(수량 0)는 뺀다")
        .extracting(AssetStatusStockAccountView::accountName)
        .containsExactly("BROKER");
    assertThat(
            result.get(STOCK).stream().mapToInt(AssetStatusStockAccountView::holdingQuantity).sum())
        .as("계좌 수량의 합이 종목 수량이다")
        .isEqualTo(100);
  }

  private static String render(Map<UUID, List<AssetStatusStockAccountView>> accounts) {
    TradeProfit stock =
        TradeProfit.ofStockStatus(
            STOCK,
            "SAMSUNG",
            bd("10000"),
            100,
            bd("11000"),
            bd("1100000"),
            bd("100000"),
            bd("0"),
            bd("1000000"));
    var breakdown = StockCombinedProfitUtil.byStockItem(List.of(stock), Map.of());
    Map<String, Object> model = new HashMap<>();
    model.put("accountTotalMap", new LinkedHashMap<UUID, TradeProfit>());
    model.put("accountProfitBasisMap", new LinkedHashMap<UUID, BigDecimal>());
    model.put("accountHoldingMap", new LinkedHashMap<>());
    model.put("stockAggregated", List.of(stock));
    model.put("stockProfitBreakdown", breakdown);
    model.put("totalEvaluationAmount", bd("1100000"));
    model.put("totalEvaluationProfit", bd("100000"));
    model.put("stockHoldingAccountMap", accounts);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  private static List<AssetStatusStockAccountView> twoAccounts() {
    return List.of(
        new AssetStatusStockAccountView(
            BROKER,
            "BROKER",
            60,
            bd("10000"),
            bd("660000"),
            bd("600000"),
            bd("60000"),
            bd("10.00"),
            bd("60.004"),
            bd("60.00")),
        new AssetStatusStockAccountView(
            ISA,
            "ISA",
            40,
            bd("10000"),
            bd("440000"),
            bd("400000"),
            bd("40000"),
            bd("10.00"),
            bd("39.996"),
            bd("40.00")));
  }

  @Test
  void 종목_줄의_펼침_버튼이_보유_계좌_줄을_가리킨다() {
    String html = render(Map.of(STOCK, twoAccounts()));
    String rowId = "asset-status-stock-detail-" + STOCK;
    String show = MessageUtil.getMessage("stock.analytics.stock.accounts.show");

    assertThat(html)
        .contains("data-detail-row-id=" + QUOTE + rowId + QUOTE)
        .contains("data-stock-detail-toggle=" + QUOTE + rowId + QUOTE)
        .contains("aria-controls=" + QUOTE + rowId + QUOTE)
        .as("이름에 종목이 들어간다")
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.action.aria"),
                show,
                "SAMSUNG"))
        .contains(">" + show + "</span>")
        .contains("(2)");
    int detail = html.indexOf("id=" + QUOTE + rowId + QUOTE);
    assertThat(detail).as("펼칠 줄이 없다").isGreaterThan(0);
    String detailRow =
        html.substring(
            html.lastIndexOf("<tr", detail), html.indexOf("data-stock-detail-row", detail) + 40);
    assertThat(detailRow).as("처음에는 접혀 있다").contains("class=" + QUOTE + "hidden ");
  }

  @Test
  void 보유_계좌_줄은_계좌_링크_수량_비중을_적는다() {
    String html = render(Map.of(STOCK, twoAccounts()));
    int start = html.indexOf("data-stock-detail-row");
    String detail = html.substring(start, html.indexOf("</table>", start));

    assertThat(detail)
        .contains(
            java.text.MessageFormat.format(
                MessageUtil.getMessage("stock.analytics.stock.accounts.detail.named"), "SAMSUNG"))
        .contains("data-stock-detail-account=" + QUOTE + BROKER + QUOTE)
        .contains("data-stock-detail-account=" + QUOTE + ISA + QUOTE)
        .contains("accountId=" + BROKER)
        .contains(">60</td>")
        .contains(">660,000</td>")
        .as("종목 안 비중은 함께 배분해 합이 100.0% 다")
        .contains(MessageUtil.getMessage("stock.analytics.stock.accounts.weight.stock") + " 60.0%")
        .contains(MessageUtil.getMessage("stock.analytics.stock.accounts.weight.stock") + " 40.0%");
    assertThat(detail.indexOf("BROKER")).as("평가액 큰 계좌가 먼저").isLessThan(detail.indexOf(">ISA<"));
  }

  @Test
  void 가진_계좌가_없으면_버튼도_줄도_없다() {
    String html = render(Map.of());

    assertThat(html)
        .doesNotContain("data-stock-detail-toggle=")
        .doesNotContain("data-stock-detail-row");
  }

  /** 컨트롤러가 계좌 표 펼침과 같은 재료(계좌 x 종목 손익 행, 보유분만)로 만들어 화면에 넘긴다. */
  @Test
  void 컨트롤러가_계좌_표와_같은_재료로_넘긴다() throws IOException {
    String controller =
        Files.readString(
                Path.of(
                    "src/main/java/net/luversof/web/gate/stock/controller/StockPortfolioHtmxController.java"),
                StandardCharsets.UTF_8)
            .replaceAll("\\s+", " ");
    assertThat(controller)
        .contains(
            "stockHoldingAccountMap = buildStockHoldingAccountViews(enrichedList, totalEvaluationAmount);")
        .contains("model.addAttribute(\"stockHoldingAccountMap\", stockHoldingAccountMap);")
        .as("보유분만 남긴 뒤에 만든다 - 다 판 계좌가 끼면 수량 0 줄이 생긴다")
        .containsPattern(
            "enrichedList\\.removeIf\\(tp -> tp\\.holdingQuantity\\(\\) == 0\\);.*buildStockHoldingAccountViews\\(enrichedList");
  }

  /** 여닫기는 계좌 표와 같은 스크립트가 한다 - 원본과 빌드 산출물 둘 다(메이븐은 프론트엔드를 빌드하지 않는다). */
  @Test
  void 같은_스크립트가_종목_표의_펼침도_여닫는다() throws IOException {
    String source =
        Files.readString(
                Path.of("src/main/frontend/src/stock/assetStatus.ts"), StandardCharsets.UTF_8)
            .replaceAll("\\s+", "");
    assertThat(source)
        .contains(
            "querySelectorAll<HTMLElement>('[data-account-detail-toggle],[data-stock-detail-toggle]')")
        .contains("(this.dataset.accountDetailToggle||this.dataset.stockDetailToggle)");
    String built =
        Files.readString(
                Path.of("src/main/resources/static/js/stock/assetStatus.js"),
                StandardCharsets.UTF_8)
            .replaceAll("\\s+", "");
    assertThat(built)
        .contains("[data-account-detail-toggle],[data-stock-detail-toggle]")
        .contains("stockDetailToggle");
  }
}
