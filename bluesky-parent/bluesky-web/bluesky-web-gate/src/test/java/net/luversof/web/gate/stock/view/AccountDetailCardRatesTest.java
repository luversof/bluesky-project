package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.dto.response.StockCashFlowResponse;
import net.luversof.web.gate.stock.util.StockFormatUtil;
import net.luversof.web.gate.stock.util.StockHoldingReturnUtil;

/**
 * 계좌 상세 카드도 손익마다 <b>그 돈의 원금 대비</b> 비율을 적고, 이 계좌 전체 거래의 연평균을 카드로 보여 준다.
 *
 * <p>2026-09-17 종목 상세에 같은 카드를 넣은 뒤 사용자가 계좌 상세에도 고른 방식이다. 계좌에서만 생기는 갈림이 하나 있다 &mdash; 카드 금액(기록
 * 실현손익)은 여러 계좌를 합친 원가를 따라서 같은 매도가 두 비율을 갖는다. 사용자 선택: <b>카드 금액 기준 비율</b> + 차이가 큰 계좌의 '이 계좌 기준' 줄에
 * <b>그 기준의 비율</b>. 그래서 어느 비율이든 바로 옆 금액과 기준이 같다.
 *
 * <p>실측 2026-09-17 한국투자증권 연금저축1(전체 기간): 평가 -1,591,181 / 보유 원가 15,213,471 = -10.5% · 실현 +415,053 / 판
 * 주식 원가 7,788,782 = +5.3% · 이 계좌 기준 +2,063,739 / 6,139,752 = +33.6% · 배당 1,183,752 / 보유 원가 = 7.8%.
 */
class AccountDetailCardRatesTest {

  private static final String CONTENT = "stock/htmx/accountDetailContent.jte";

  private static final LocalDate TODAY = LocalDate.parse("2026-09-17");

  private static final LocalDate FIRST_TRADE = LocalDate.parse("2025-03-28");

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

  /** 연금저축1 과 같은 모양(실측 값). */
  private static Map<String, Object> pension() {
    Map<String, Object> model = new HashMap<>();
    model.put(
        "account",
        new Account(UUID.randomUUID(), UUID.randomUUID(), "한국투자증권 연금저축1", null, Map.of()));
    model.put("holdingCount", 4);
    model.put("evaluationAmount", new BigDecimal("13622290"));
    model.put("evaluationProfit", new BigDecimal("-1591181"));
    model.put("realizedProfit", new BigDecimal("415053"));
    model.put("realizedProfitOwnBasis", new BigDecimal("2063739"));
    model.put("realizedCostBasis", new BigDecimal("7788782"));
    model.put("realizedOwnCostBasis", new BigDecimal("6139752"));
    model.put("totalDividend", new BigDecimal("1183752"));
    model.put("accountFirstTradeDate", FIRST_TRADE);
    model.put(
        "accountReturn",
        StockHoldingReturnUtil.of(
            FIRST_TRADE,
            TODAY,
            List.of(
                new StockCashFlowResponse(FIRST_TRADE, new BigDecimal("-21352966")),
                new StockCashFlowResponse(
                    LocalDate.parse("2026-02-10"), new BigDecimal("9387087"))),
            new BigDecimal("13622290"),
            new BigDecimal("-1591181"),
            BigDecimal.ZERO));
    return model;
  }

  private static String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(CONTENT, model, output);
    return output.toString();
  }

  /** 라벨이 든 카드 한 장(라벨부터 다음 카드 라벨 앞까지). */
  private static String card(String html, String label) {
    int at = html.indexOf("stat-card-label\">" + label);
    assertThat(at).as(label + " 카드를 못 찾았다").isGreaterThan(0);
    int next = html.indexOf("stat-card-label", at + 1);
    return html.substring(at, next < 0 ? html.length() : next);
  }

  /** 카드 안 보조줄 알약들(글자만, 순서대로). */
  private static List<String> subs(String card) {
    List<String> out = new java.util.ArrayList<>();
    int at = card.indexOf("class=\"stat-card-sub");
    while (at >= 0) {
      int open = card.indexOf('>', at) + 1;
      int close = card.indexOf("</div>", open);
      out.add(card.substring(open, close).replaceAll("<[^>]+>", ""));
      at = card.indexOf("class=\"stat-card-sub", close);
    }
    return out;
  }

  @Test
  void 세_카드가_각자의_원금_대비_비율과_기준을_적는다() {
    String html = render(pension());

    assertThat(subs(card(html, message("stock.profit.unrealized"))))
        .containsExactly("-10.5% · " + message("stock.item.detail.rate.basis.holding"));
    String realized = card(html, message("stock.profit.realized"));
    assertThat(subs(realized).get(0))
        .as("415,053 / 7,788,782 = 5.33% - 카드 금액과 같은 기준(이 계좌 기준 +33.6% 가 아니다)")
        .isEqualTo("+5.3% · " + message("stock.item.detail.rate.basis.sold"));
    assertThat(realized).contains("text-profit");
    assertThat(subs(card(html, message("stock.item.detail.period.dividend"))))
        .as("1,183,752 / 15,213,471 = 7.78% - 배당은 음수가 없어 부호를 안 붙인다")
        .containsExactly("7.8% · " + message("stock.item.detail.rate.basis.holding"));
  }

  @Test
  void 차이가_크면_이_계좌_기준_줄에_그_기준의_비율을_붙인다() {
    String realized = card(render(pension()), message("stock.profit.realized"));
    List<String> lines = subs(realized);

    assertThat(lines).as("두 비율은 두 줄이다 - 한 알약에 이으면 어느 비율이 어느 금액의 것인지 갈리지 않는다").hasSize(2);
    assertThat(lines.get(1))
        .as("2,063,739 / 6,139,752 = 33.61%")
        .isEqualTo(
            java.text.MessageFormat.format(message("stock.trade.realized.basis.gap"), "+2,063,739")
                + " · +33.6%");
    assertThat(realized)
        .as("금액 숨김이 걸리도록 금액만 따로 감싼다")
        .contains("<span class=\"amount-value\">+2,063,739</span>");
  }

  /** 위탁 계좌와 같은 모양: 두 기준 차이가 값의 0.01% 라 '이 계좌 기준' 줄이 없다 - 비율 줄 하나다. */
  @Test
  void 차이가_작으면_비율_줄만_있다() {
    Map<String, Object> model = pension();
    model.put("realizedProfit", new BigDecimal("190075456"));
    model.put("realizedProfitOwnBasis", new BigDecimal("190053537"));
    model.put("realizedCostBasis", new BigDecimal("794943974"));
    model.put("realizedOwnCostBasis", new BigDecimal("793057336"));

    assertThat(subs(card(render(model), message("stock.profit.realized"))))
        .containsExactly("+23.9% · " + message("stock.item.detail.rate.basis.sold"));
  }

  @Test
  void 판_적이_없으면_실현_비율_대신_사유를_적고_0_을_이득색으로_칠하지_않는다() {
    Map<String, Object> model = pension();
    model.put("realizedProfit", BigDecimal.ZERO);
    model.put("realizedProfitOwnBasis", BigDecimal.ZERO);
    model.put("realizedCostBasis", BigDecimal.ZERO);
    model.put("realizedOwnCostBasis", BigDecimal.ZERO);
    String realized = card(render(model), message("stock.profit.realized"));

    assertThat(subs(realized)).containsExactly(message("stock.item.detail.rate.none.sold"));
    assertThat(realized).doesNotContain("text-profit");
  }

  /** 동양증권과 같은 모양: 다 팔아 보유가 없다. 보유 원가가 없으니 배당 비율도 연평균 카드도 없다(종목 상세와 같다). */
  @Test
  void 보유가_없으면_배당_비율_대신_사유를_적고_연평균_카드도_없다() {
    Map<String, Object> model = pension();
    model.put("holdingCount", 0);
    model.put("evaluationAmount", BigDecimal.ZERO);
    model.put("evaluationProfit", BigDecimal.ZERO);
    String html = render(model);

    assertThat(subs(card(html, message("stock.item.detail.period.dividend"))))
        .containsExactly(message("stock.item.detail.rate.none.holding"));
    assertThat(html).doesNotContain("data-account-return");
  }

  @Test
  void 연평균은_이_계좌_전체_거래_기준_카드로_기간_배당_뒤에_있다() {
    String html = render(pension());
    int cardAt = html.indexOf("data-account-return");
    assertThat(cardAt).as("연평균 카드가 없다").isGreaterThan(0);
    String card =
        html.substring(
            html.lastIndexOf("<div", cardAt),
            html.indexOf("</div>", html.indexOf("stat-card-sub", cardAt)));
    StockHoldingReturnUtil.Row row = (StockHoldingReturnUtil.Row) pension().get("accountReturn");

    assertThat(row.annualizedPct()).as("표본이 연평균을 낼 수 있어야 검사가 뜻이 있다").isNotNull();
    assertThat(card)
        .contains("class=\"contents\"")
        .contains(message("stock.asset.status.cell.annualized.name"))
        .as("기간 카드들과 범위가 다르다는 것을 낭독기에도 알린다 - 종목 상세의 '보유 전체 기준' 이 아니다")
        .contains(
            "<span class=\"sr-only\"> " + message("stock.account.detail.return.basis") + "</span>")
        .contains(">" + StockFormatUtil.signedPct(row.annualizedPct().doubleValue(), 1) + "</div>")
        .as("1 년이 넘었으니 환산 안내 대신 시작일")
        .contains(
            java.text.MessageFormat.format(
                message("stock.asset.status.cell.holding.since"),
                "<span class=\"whitespace-nowrap date-whole\">"
                    + FIRST_TRADE.toString()
                    + "</span>"))
        .contains(
            java.text.MessageFormat.format(
                message("stock.asset.status.cell.holding.years"), "1", "5"));
    assertThat(cardAt)
        .as("카드 줄(기간 배당 카드 뒤)에 있다")
        .isGreaterThan(
            html.indexOf("stat-card-label\">" + message("stock.item.detail.period.dividend")));
  }

  /**
   * 공백을 모두 지운다. 서식 도구는 식 중간에서 줄을 나눈다 &mdash; 실측 2026-09-17: {@code .subtract(} 앞에서 줄이 나뉘자 공백을 하나로
   * 누른 비교가 멀쩡한 코드에서 실패했다. 기대 문자열도 같은 규칙으로 누른다.
   */
  private static String squash(String text) {
    return text.replaceAll("\\s+", "");
  }

  private static String source(String path) throws IOException {
    return squash(Files.readString(Path.of(path), StandardCharsets.UTF_8));
  }

  /**
   * 분모 둘은 실현손익과 같은 기간 행(profits)에서, 연평균은 이 계좌 흐름에서 기간 없이 만든다. 소스 한 파일에 종목 상세의 같은 식이 있어 계좌 메서드 안만
   * 본다.
   */
  @Test
  void 컨트롤러가_계좌_메서드_안에서_두_분모와_계좌_흐름을_만든다() throws IOException {
    String source =
        source(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java");
    String account = source.substring(source.indexOf(squash("public String accountDetailPage(")));
    String params =
        account.substring(
            account.indexOf(squash("MultiValueMap<String, String> accCashFlowParams")),
            account.indexOf(squash("var accCashFlowFuture")));

    assertThat(account)
        .contains(
            squash(
                "BigDecimal periodSellAmount = sumTradeProfit(profits, TradeProfit::totalSellAmount);"))
        .contains(
            squash(
                "\"realizedCostBasis\", periodSellAmount.signum() > 0 ? periodSellAmount.subtract(realizedProfit) : BigDecimal.ZERO);"))
        .as("이 계좌 기준 분모는 수수료 · 세금을 뺀 총수령액에서 이 계좌 기준(Net) 실현손익을 뺀다")
        .contains(
            squash(
                "sumTradeProfit(profits, TradeProfit::totalSellProceeds).subtract(realizedProfitOwnBasis)"))
        .contains(squash("tradeClient.findCashFlowsByStockItem(accCashFlowParams)"))
        .contains(squash("StockHoldingReturnUtil.combine("))
        .contains(squash("StockHoldingReturnUtil.firstDate(accountCashFlows)"));
    assertThat(params)
        .contains(squash("accCashFlowParams.add(\"accountIdList\", resolvedId.toString());"))
        .as("연평균은 기간 선택과 무관한 전체 거래 값이다")
        .doesNotContain("startDate")
        .doesNotContain("endDate");
    assertThat(source("src/main/jte/stock/accountDetail.jte"))
        .contains(squash("realizedCostBasis = realizedCostBasis"))
        .contains(squash("realizedOwnCostBasis = realizedOwnCostBasis"))
        .contains(squash("accountReturn = accountReturn"))
        .contains(squash("accountFirstTradeDate = accountFirstTradeDate"));
  }
}
