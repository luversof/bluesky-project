package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.MessageFormat;
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

  private static String message(String code) {
    return MessageUtil.getMessage(code);
  }

  /** 매수원가 1,000,000 · 합산 +200,000. 보유 기간만 바꿔 가며 연평균을 본다. */
  private static TradeProfit steady() {
    return TradeProfit.ofStockStatus(
        STOCK,
        "꾸준종목",
        bd("10000"),
        100,
        bd("12000"),
        bd("1200000"),
        bd("200000"),
        bd("0"),
        bd("1000000"));
  }

  private String render(List<TradeProfit> stocks, Map<UUID, BigDecimal> dividends) {
    return render(stocks, dividends, Map.of(), null);
  }

  private String render(
      List<TradeProfit> stocks,
      Map<UUID, BigDecimal> dividends,
      Map<UUID, LocalDate> firstBuyDates,
      LocalDate holdingBasisDate) {
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
    model.put("firstBuyDateByStockItem", firstBuyDates);
    model.put("holdingBasisDate", holdingBasisDate);
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
    // 집계 표는 본문 첫 칸도 th scope="row" 다(2026-09-11) - td 만 세면 한 칸이 빠진다.
    int row = count(body, "<td ") + count(body, "<th ");
    String foot = table.substring(table.indexOf("<tfoot"));
    // 합계 줄의 첫 칸은 th scope="row" 다(2026-09-11) - td 만 세면 한 칸이 빠진다.
    int total = count(foot, "<td") + count(foot, "<th");

    assertThat(head).as("열을 더했는데 머리글이 따라오지 않았다").isEqualTo(12);
    assertThat(row).as("본문 칸 수가 머리글과 다르다").isEqualTo(head);
    assertThat(total).as("합계 칸 수가 머리글과 다르다").isEqualTo(head);
  }

  /** 보유가 없을 때의 빈 줄도 늘어난 열 수만큼 걸쳐야 한다. */
  @Test
  void 빈_표의_colspan_도_열_수를_따라간다() {
    String table = stockTable(render(List.of(), Map.of()));

    assertThat(table).as("colspan 이 열 수보다 작으면 빈 줄이 표를 덜 덮는다").contains("colspan=\"12\"");
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

    assertThat(body).as("한 번도 안 판 종목에 0 원을 적으면 본전이라는 뜻이 된다").contains(">-</span>");
  }

  /** 계좌 id 는 이 표와 무관하지만, 모델을 비워도 렌더가 죽지 않아야 한다. */
  @Test
  void 자료가_없어도_표를_그린다() {
    assertThat(render(List.of(), Map.of()))
        .contains(MessageUtil.getMessage("stock.message.no.holdings"));
    assertThat(ACCOUNT).isNotNull();
  }

  // ------------------------------------------------ 보유 기간 · 연평균 · 배당 상쇄율 (2026-09-14)

  /**
   * 2026-09-14 까지 이 세 값을 말하던 것은 표 위의 별도 카드였다. 그 카드가 찍던 금액 세 개는 바로 아래 표에 이미 열로 있었고, 막대는 줄 안에서 정규화돼 둘
   * 중 하나가 항상 100% 였다 - 옆 배지의 숫자와 같은 말이었다. 카드를 지우고 세 값을 각자 자기 값이 있는 칸 안으로 옮겼다.
   */
  @Test
  void 보유_기간을_종목_이름_아래에_적는다() {
    String table =
        stockTable(
            render(
                List.of(stock()),
                Map.of(STOCK, bd("5385714")),
                Map.of(STOCK, LocalDate.parse("2020-03-04")),
                LocalDate.parse("2026-09-14")));

    assertThat(table)
        .as("보유 기간이 없다")
        .contains("data-holding-period")
        .contains("data-holding-days=\"2385\"");
    assertThat(table)
        .as("6 년 6 개월 (2020-03-04 -> 2026-09-14)")
        .contains(MessageFormat.format(message("stock.asset.status.cell.holding.years"), "6", "6"));
    assertThat(table)
        .as("숫자만 있으면 무슨 값인지 모른다 - 이름과 근거가 낙독기에 닿아야 한다")
        .contains(message("stock.asset.status.col.holding.period"))
        .contains(
            MessageFormat.format(message("stock.asset.status.cell.holding.since"), "2020-03-04"));
  }

  /**
   * 최초 매수일을 모르면(집계에 없는 종목) 기간을 지어내지 않는다.
   *
   * <p>열이 된 뒤로는 칸 자체는 남는다 - 빈 칸은 "0 일"로 읽히므로 사유를 적는다.
   */
  @Test
  void 최초_매수일을_모르면_사유를_적는다() {
    String table =
        stockTable(render(List.of(stock()), Map.of(), Map.of(), LocalDate.parse("2026-09-14")));

    assertThat(table).contains(message("stock.asset.status.col.holding.period.unknown"));
    assertThat(table)
        .as("기간을 모르는데 연수/개월을 지어내면 안 된다")
        .doesNotContain(
            MessageFormat.format(message("stock.asset.status.cell.holding.years"), "6", "6"));
  }

  /** 보유 기간은 표시 문자열이 아니라 일수로 정렬해야 한다 - '6개월'이 '6년'보다 뒤에 오면 안 된다. */
  @Test
  void 보유_기간_열은_일수로_정렬한다() {
    String table =
        stockTable(
            render(
                List.of(stock()),
                Map.of(STOCK, bd("5385714")),
                Map.of(STOCK, LocalDate.parse("2020-03-04")),
                LocalDate.parse("2026-09-14")));

    assertThat(table).as("머리칸에 정렬 단추가 없으면 이 열로 정렬할 수 없다").contains("data-sort-key=\"holdingDays\"");
    assertThat(table).as("정렬 값은 행의 일수여야 한다").contains("data-holding-days=\"2385\"");
  }

  /** 실측의 한 종목(평가 -12,444,645 · 배당 5,385,714)은 배당이 손실의 43% 를 덮었다. */
  @Test
  void 배당_상쇄율을_평가손익_칸에_적는다() {
    String table =
        stockTable(
            render(
                List.of(stock()),
                Map.of(STOCK, bd("5385714")),
                Map.of(STOCK, LocalDate.parse("2020-03-04")),
                LocalDate.parse("2026-09-14")));

    assertThat(table).contains("data-coverage-pct=\"43\"");
    assertThat(table)
        .contains(MessageFormat.format(message("stock.asset.status.cell.coverage"), "43"));
  }

  /** 평가익 종목은 덮을 손실이 없다 - 배당을 받았어도 상쇄율을 적지 않는다. */
  @Test
  void 평가익_종목에는_상쇄율을_적지_않는다() {
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
    String table =
        stockTable(
            render(
                List.of(gaining),
                Map.of(STOCK, bd("5385714")),
                Map.of(STOCK, LocalDate.parse("2020-03-04")),
                LocalDate.parse("2026-09-14")));

    assertThat(table).doesNotContain("data-dividend-coverage");
  }

  /**
   * 연평균은 복리다. 매수원가 1,000,000 · 합산 +200,000 을 꼬박 1 년 들고 있었으면 연 20.0% 이다.
   *
   * <p>단순 환산이면 같은 값이 나오므로 이 줄만으로는 두 식을 가를 수 없다 - 아래 짧은 보유 검사가 그 일을 한다.
   */
  @Test
  void 연평균을_합산_손익_칸에_적는다() {
    String table =
        stockTable(
            render(
                List.of(steady()),
                Map.of(),
                Map.of(STOCK, LocalDate.parse("2025-09-14")),
                LocalDate.parse("2026-09-14")));

    assertThat(table).contains("data-annualized-return");
    assertThat(table)
        .contains(MessageFormat.format(message("stock.asset.status.cell.annualized"), "+20.0%"));
    assertThat(table).as("1 년을 채웠으면 확대된 값이 아니다").contains("data-short-term=\"false\"");
  }

  /**
   * 보유 117 일짜리 +20.0% 를 복리로 펼치면 연 76.6% 다(단순 환산이면 62.4%). 식을 바꿔 놓으면 이 검사가 깨진다.
   *
   * <p>실측 2026-09-14: 보유 9 종목 중 7 종목이 1 년 미만이라 이 확대가 표의 기본값에 가깝다. 근거를 함께 적는다.
   */
  @Test
  void 보유_1년_미만은_연평균에_근거를_붙인다() {
    String table =
        stockTable(
            render(
                List.of(steady()),
                Map.of(),
                Map.of(STOCK, LocalDate.parse("2026-05-20")),
                LocalDate.parse("2026-09-14")));

    assertThat(table)
        .as("복리가 아니면 62.4% 가 나온다")
        .contains(MessageFormat.format(message("stock.asset.status.cell.annualized"), "+76.6%"));
    assertThat(table).contains("data-short-term=\"true\"");
    assertThat(table)
        .contains(MessageFormat.format(message("stock.asset.status.cell.annualized.short"), "117"));
  }

  /** 원금보다 더 잃으면 복리로 환산할 수 없다(음수의 분수 거듭제곱). 억지로 -100% 를 적으면 '딱 전액 손실' 로 읽힌다. */
  @Test
  void 원금보다_더_잃으면_연평균을_비운다() {
    String table =
        stockTable(
            render(
                List.of(stock()),
                Map.of(STOCK, bd("5385714")),
                Map.of(STOCK, LocalDate.parse("2020-03-04")),
                LocalDate.parse("2026-09-14")));

    assertThat(table).doesNotContain("data-annualized-return");
  }
}
