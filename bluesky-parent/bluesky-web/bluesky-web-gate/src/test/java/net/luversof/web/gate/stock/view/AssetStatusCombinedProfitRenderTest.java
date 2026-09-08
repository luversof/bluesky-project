package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.util.StockCombinedProfitUtil;

/**
 * 자산 현황의 종목별 표에 <b>실현손익 · 누적배당 · 합산손익</b>이 나오는지 렌더해서 본다.
 *
 * <p>세 값은 종목 상세에만 있었다. 그래서 "이 종목으로 지금까지 얼마 벌었나" 를 보려면 종목을 하나씩 열어야 했고, 종목끼리 견주는 것은 아예 되지 않았다.
 *
 * <p>표에 열을 더하는 일은 <b>머리글·본문·합계·빈 상태의 칸 수가 함께 움직여야</b> 한다. 한 곳만 놓치면 표가 어긋나는데 렌더는 조용히 된다. 그래서 칸 수를 직접
 * 센다.
 */
class AssetStatusCombinedProfitRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/assetStatus.jte";
  private static final UUID STOCK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

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

  /** 실측 2026-09-03 의 한 종목: 평가 -12,444,645 · 실현 906,369 · 배당 5,385,714 -> 합산 -6,152,562. */
  private static TradeProfit stock() {
    return TradeProfit.ofStockStatus(
        STOCK,
        "테스트종목",
        bd("20000"),
        100,
        bd("19000"),
        bd("1900000"),
        bd("-12444645"),
        bd("906369"),
        bd("2000000"));
  }

  private String render(List<TradeProfit> stocks, Map<UUID, BigDecimal> dividends) {
    var breakdown = StockCombinedProfitUtil.byStockItem(stocks, dividends);
    Map<String, Object> model = new HashMap<>();
    model.put("accountTotalMap", new LinkedHashMap<UUID, TradeProfit>());
    model.put("accountProfitBasisMap", new LinkedHashMap<UUID, BigDecimal>());
    model.put("manualPrincipalAccountIds", java.util.Set.of());
    model.put("accountHoldingMap", new LinkedHashMap<>());
    model.put("stockItemList", List.of());
    model.put("stockAggregated", stocks);
    model.put("stockProfitBreakdown", breakdown);
    model.put("stockProfitBreakdownTotal", StockCombinedProfitUtil.total(breakdown));
    model.put("totalEvaluationAmount", bd("1900000"));
    model.put("totalEvaluationProfit", bd("-12444645"));
    model.put("priceBasisDate", LocalDate.parse("2026-09-02"));
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 종목별 표만 잘라 본다. 계좌별 표에도 비슷한 열이 있어 통째로 세면 헛돈다. */
  private String stockTable(String html) {
    int start = html.indexOf("data-asset-status-stock-table");
    assertThat(start).as("종목별 표를 찾지 못했다 - 검사가 무력해진다").isGreaterThan(0);
    return html.substring(start, html.indexOf("</table>", start));
  }

  private static int count(String source, String regex) {
    Matcher matcher = Pattern.compile(regex).matcher(source);
    int found = 0;
    while (matcher.find()) {
      found++;
    }
    return found;
  }

  @Test
  void 종목마다_실현손익과_누적배당과_합산손익을_적는다() {
    String table = stockTable(render(List.of(stock()), Map.of(STOCK, bd("5385714"))));

    assertThat(table)
        .as("세 값이 종목 상세에만 있어 종목끼리 견줄 수가 없었다")
        .contains(MessageUtil.getMessage("stock.profit.realized"))
        .contains(MessageUtil.getMessage("stock.asset.status.col.dividend.total"))
        .contains(MessageUtil.getMessage("stock.asset.status.col.combined.profit"));

    assertThat(table)
        .as("실현 906,369 · 배당 5,385,714 · 합산 -12,444,645+906,369+5,385,714 = -6,152,562")
        .contains("+906,369")
        .contains("5,385,714")
        .contains("-6,152,562");
  }

  /** 정렬은 data 속성으로 한다. 열만 넣고 속성을 빠뜨리면 머리글은 눌리는데 아무 일도 안 일어난다. */
  @Test
  void 새_열도_정렬할_수_있다() {
    String table = stockTable(render(List.of(stock()), Map.of(STOCK, bd("5385714"))));

    assertThat(table)
        .contains("data-sort-key=\"realizedProfit\"")
        .contains("data-sort-key=\"dividendTotal\"")
        .contains("data-sort-key=\"combinedProfit\"")
        .contains("data-realized-profit=\"906369\"")
        .contains("data-dividend-total=\"5385714\"")
        .contains("data-combined-profit=\"-6152562\"");
  }

  /**
   * 머리글 · 본문 · 합계의 칸 수가 같아야 한다.
   *
   * <p>열을 더할 때 한 곳만 놓치면 표가 통째로 어긋나는데 렌더는 조용히 된다. 빈 상태의 {@code colspan} 도 같이 움직여야 한다.
   */
  @Test
  void 머리글과_본문과_합계의_칸_수가_같다() {
    String table = stockTable(render(List.of(stock()), Map.of(STOCK, bd("5385714"))));

    int head = count(table.substring(0, table.indexOf("</thead>")), "<th ");
    String body = table.substring(table.indexOf("<tbody>"), table.indexOf("</tbody>"));
    int row = count(body, "<td ");
    String foot = table.substring(table.indexOf("<tfoot"));
    int total = count(foot, "<td");

    assertThat(head).as("열을 더했는데 머리글이 따라오지 않았다").isEqualTo(11);
    assertThat(row).as("본문 칸 수가 머리글과 다르다").isEqualTo(head);
    assertThat(total).as("합계 칸 수가 머리글과 다르다").isEqualTo(head);
  }

  /** 보유가 없을 때의 빈 줄도 늘어난 열 수만큼 걸쳐야 한다. */
  @Test
  void 빈_표의_colspan_도_열_수를_따라간다() {
    String table = stockTable(render(List.of(), Map.of()));

    assertThat(table).as("colspan 이 열 수보다 작으면 빈 줄이 표를 덜 덮는다").contains("colspan=\"11\"");
  }

  /**
   * 이 표에는 <b>보유 중인 종목만</b> 나온다는 것을 밝힌다.
   *
   * <p>실현손익·배당을 열로 넣으면서 밝히지 않으면 합계를 전체 누적으로 오해한다 &mdash; 실측 2026-09-03: 보유 9 종목의 실현 140,350,295 ·
   * 배당 59,067,537 인데, 이미 다 판 34 종목에 실현 85,279,840 · 배당 6,582,497 이 더 있다.
   */
  @Test
  void 보유_중인_종목만_나온다는_것을_밝힌다() {
    String html = render(List.of(stock()), Map.of(STOCK, bd("5385714")));

    assertThat(html)
        .as("밝히지 않으면 합계를 전체 누적으로 오해한다")
        .contains("data-asset-status-held-only-note")
        .contains(MessageUtil.getMessage("stock.asset.status.combined.held.only"));
  }

  /** 판 적도 배당도 없는 종목은 0 이 아니라 '없음' 이다. 0 원과 뜻이 다르다. */
  @Test
  void 실현도_배당도_없으면_금액_대신_줄표를_쓴다() {
    TradeProfit neverSold =
        TradeProfit.ofStockStatus(
            STOCK,
            "테스트종목",
            bd("20000"),
            100,
            bd("19000"),
            bd("1900000"),
            bd("-100000"),
            BigDecimal.ZERO,
            bd("2000000"));

    String table = stockTable(render(List.of(neverSold), Map.of()));
    String body = table.substring(table.indexOf("<tbody>"), table.indexOf("</tbody>"));

    assertThat(body).as("한 번도 안 판 종목에 0 원을 적으면 본전이라는 뜻이 된다").contains("text-base-content/30\">-<");
  }

  /** 계좌 id 는 이 표와 무관하지만, 모델을 비워도 렌더가 죽지 않아야 한다. */
  @Test
  void 자료가_없어도_표를_그린다() {
    assertThat(render(List.of(), Map.of()))
        .contains(MessageUtil.getMessage("stock.message.no.holdings"));
    assertThat(ACCOUNT).isNotNull();
  }

  // ---------------------------------------------------------------- 배당이 평가손실을 얼마나 메웠나 (2026-09-08)

  /** 실측의 한 종목(평가 -12,444,645 · 배당 5,385,714)은 배당이 손실의 43% 를 덮었다. 표의 세 열을 빼지 않아도 보여야 한다. */
  @Test
  void 배당_상쇄_카드는_손실의_몇_퍼센트를_덮었는지_보여준다() {
    String html = render(List.of(stock()), Map.of(STOCK, bd("5385714")));
    int at = html.indexOf("data-dividend-coverage-row");
    assertThat(at).as("상쇄 카드가 없다").isGreaterThan(0);
    String row =
        html.substring(at, html.indexOf("</div>\n", html.indexOf("data-coverage-pct", at)) + 6);

    assertThat(row)
        .contains("data-coverage-state=\"partial\"")
        .contains("data-coverage-pct=\"43\"");
    assertThat(html.substring(at, at + 3000))
        .contains("테스트종목")
        .contains("5,385,714")
        .contains("12,444,645");
    assertThat(html.substring(at, at + 3000))
        .as("손실 막대가 100, 배당 막대가 43")
        .contains("style=\"width:100%\"")
        .contains("style=\"width:43%\"");
  }

  /** 평가익 종목은 덮을 손실이 없다. */
  @Test
  void 배당_상쇄_카드는_손실_없는_종목을_그렇게_표시한다() {
    TradeProfit gaining =
        TradeProfit.ofStockStatus(
            STOCK,
            "이익종목",
            bd("20000"),
            100,
            bd("30000"),
            bd("3000000"),
            bd("1000000"),
            bd("0"),
            bd("2000000"));
    String html = render(List.of(gaining), Map.of(STOCK, bd("5385714")));

    assertThat(html).contains("data-coverage-state=\"none\"");
  }

  /** 손실도 배당도 없으면 카드 자체가 없다. */
  @Test
  void 배당_상쇄_카드는_할_말이_없으면_나오지_않는다() {
    TradeProfit quiet =
        TradeProfit.ofStockStatus(
            STOCK,
            "조용",
            bd("20000"),
            100,
            bd("20000"),
            bd("2000000"),
            bd("0"),
            bd("0"),
            bd("2000000"));
    String html = render(List.of(quiet), Map.of());

    assertThat(html).doesNotContain("data-dividend-coverage");
  }
}
