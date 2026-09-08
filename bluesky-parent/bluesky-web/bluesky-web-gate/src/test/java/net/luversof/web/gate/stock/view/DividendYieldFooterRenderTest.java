package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.luversof.web.gate.stock.dto.view.DividendYieldGroupView;

/**
 * 배당 수익률 표를 <b>실제로 그려서</b> 합계행이 본문 행과 맞는지 본다.
 *
 * <p>이 표에서 실제로 어긋난 적이 있다 &mdash; 2026-08-24 확인: 일평균원금 기준 수익률의 분자가 행은 걸러진 세후액({@code
 * netAmountWithPrincipalCost}), 합계행은 걸러지지 않은 세후액이었다. 같은 열인데 규칙이 달랐고, 배당이 그 걸러지는 건뿐인 종목은 <b>행이 0.00%
 * 인데 합계에는 들어가</b> 있었다.
 *
 * <p>그때 붙인 검사({@code DividendYieldSelectionConsistencyTest})는 소스 문자열을 본다. 식을 다르게 <b>고쳐 적으면</b>
 * 빠져나간다. 여기서는 조각을 렌더해 나온 숫자를 파싱해 맞춘다.
 */
class DividendYieldFooterRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/dividend/dividendYieldAnalytics.jte";

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

  /** 서버가 행마다 내는 수익률. 걸러진 세후액을 분모로 나눈다(YieldAccumulator.toView 와 같은 규칙). */
  private static BigDecimal pct(BigDecimal numerator, BigDecimal denominator) {
    if (denominator == null || denominator.signum() == 0) {
      return null;
    }
    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }

  /**
   * 행 하나.
   *
   * @param netWithPrincipalCost 기준일 원금이 있는 배당만 모은 세후액. 총 세후액보다 작을 수 있다.
   */
  private static DividendYieldGroupView row(
      String label,
      String gross,
      String net,
      String taxable,
      String netWithPrincipalCost,
      String dailyPrincipal,
      String principal) {
    BigDecimal filtered = bd(netWithPrincipalCost);
    return new DividendYieldGroupView(
        UUID.randomUUID(),
        label,
        bd(gross),
        bd(net),
        bd(taxable),
        filtered,
        filtered,
        bd(dailyPrincipal),
        bd(principal),
        bd(principal),
        pct(filtered, bd(dailyPrincipal)),
        pct(filtered, bd(principal)),
        pct(filtered, bd(principal)),
        1L,
        Instant.parse("2026-08-19T00:00:00Z"));
  }

  private Map<String, Object> params(List<DividendYieldGroupView> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put(
        "percentFormat",
        (java.util.function.Function<BigDecimal, String>)
            value -> value == null ? "-" : value.setScale(2, RoundingMode.HALF_UP) + "%");
    model.put("countFormat", (java.util.function.Function<Long, String>) String::valueOf);
    model.put("safeAllNet", BigDecimal.ONE);
    model.put("portfolioYieldOnDailyAverageCostPct", BigDecimal.ZERO);
    model.put("portfolioYieldOnCostPct", BigDecimal.ZERO);
    model.put("portfolioYieldOnMarketPct", BigDecimal.ZERO);
    model.put("portfolioYieldAnnualizedPct", null);
    model.put("periodDayCount", 365L);
    model.put("periodStartPrincipal", null);
    model.put("periodEndPrincipal", null);
    model.put("periodPrincipalDelta", null);
    model.put("periodPrincipalDeltaPct", null);
    model.put("bestYieldStock", null);
    model.put("bestYieldAccount", null);
    model.put("yearlyYieldRows", List.of());
    model.put("stockYieldRows", rows);
    model.put("accountYieldRows", rows);
    model.put("stockItemList", List.of());
    for (String label :
        List.of(
            "accountLabel",
            "stockNameLabel",
            "tagLabel",
            "noDataLabel",
            "grossAmountLabel",
            "netAmountLabel",
            "taxLabel",
            "taxableAmountLabel",
            "totalLabel")) {
      model.put(label, label);
    }
    return model;
  }

  private String render(List<DividendYieldGroupView> rows) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params(rows), output);
    return output.toString();
  }

  /** 연도별 표까지 채워 렌더한다. 종목별 표는 {@code rows} 로, 연도별 표는 {@code yearly} 로. */
  private String renderWithYearly(
      List<DividendYieldGroupView> rows, List<DividendYieldGroupView> yearly) {
    Map<String, Object> model = params(rows);
    model.put("yearlyYieldRows", yearly);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 연도별 표의 전체 기간 줄 셀. 없으면 빈 목록. */
  private List<String> yearlyTotalCells(String html) {
    int start = html.indexOf("<tfoot data-dividend-yearly-total");
    if (start < 0) {
      return List.of();
    }
    List<String> cells = new ArrayList<>();
    Matcher matcher = CELL.matcher(html.substring(start, html.indexOf("</tfoot>", start)));
    while (matcher.find()) {
      cells.add(text(matcher.group(1)));
    }
    return cells;
  }

  private static final Pattern CELL = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);

  /** 셀 안의 태그를 걷어 내고 남은 글자. */
  private static String text(String cell) {
    return cell.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
  }

  /** tfoot 안의 셀 글자들. */
  private List<String> footerCells(String html) {
    List<String> cells = new ArrayList<>();
    int at = 0;
    while (true) {
      int start = html.indexOf("<tfoot", at);
      if (start < 0) {
        return cells;
      }
      int end = html.indexOf("</tfoot>", start);
      if (end < 0) {
        return cells;
      }
      Matcher matcher = CELL.matcher(html.substring(start, end));
      while (matcher.find()) {
        cells.add(text(matcher.group(1)));
      }
      at = end;
    }
  }

  @Test
  void 합계행이_본문_금액의_합과_같다() {
    List<DividendYieldGroupView> rows =
        List.of(
            row("가", "1200000", "1000000", "900000", "1000000", "10000000", "10000000"),
            // 이 행은 기준일 원금이 없는 배당이 섞여 있어 걸러진 세후액이 더 작다.
            row("나", "600000", "500000", "400000", "300000", "5000000", "5000000"));

    List<String> cells = footerCells(render(rows));

    assertThat(cells).as("합계행 셀을 찾지 못했다 - 검사가 무력해진다").isNotEmpty();
    assertThat(cells).contains("1,800,000"); // 총액 1,200,000 + 600,000
    assertThat(cells).contains("1,500,000"); // 세후 1,000,000 + 500,000
    assertThat(cells).contains("300,000"); // 세금 = 총액 - 세후
    assertThat(cells).contains("1,300,000"); // 과세표준 900,000 + 400,000
    assertThat(cells).contains("15,000,000"); // 일평균원금 10,000,000 + 5,000,000
  }

  @Test
  void 합계행의_일평균원금_수익률이_걸러진_분자를_쓴다() {
    List<DividendYieldGroupView> rows =
        List.of(
            row("가", "1200000", "1000000", "900000", "1000000", "10000000", "10000000"),
            row("나", "600000", "500000", "400000", "300000", "5000000", "5000000"));

    List<String> cells = footerCells(render(rows));

    // 걸러진 분자 1,300,000 / 일평균원금 15,000,000 = 8.67%
    // 걸러지지 않은 세후액 1,500,000 을 쓰면 10.00% 가 된다.
    assertThat(cells)
        .as("합계행의 일평균원금 수익률이 걸러지지 않은 세후액을 쓴다(행과 규칙이 달라진다)")
        .contains("8.67%")
        .doesNotContain("10.00%");
  }

  @Test
  void 배당이_전부_걸러지면_행도_합계도_0이다() {
    // 지급일 이전에 전량 매도한 종목: 세후액은 있지만 기준일 원금이 없어 분자에서 빠진다.
    List<DividendYieldGroupView> rows =
        List.of(row("전량매도", "120000", "100000", "120000", "0", "5000000", "0"));

    String html = render(rows);

    assertThat(html).as("걸러진 분자가 0 이면 행 수익률도 0 이어야 한다").contains("0.00%");
    // 세후액(100,000)은 그대로 보이지만 수익률 분자로는 쓰이지 않는다.
    assertThat(html).contains("100,000");
    assertThat(footerCells(html)).as("합계행도 같은 규칙이라 0.00% 여야 한다").contains("0.00%");
  }

  // ---------------------------------------------------------------- 연도별 표 전체 기간 줄 (2026-09-08)

  /**
   * 연도별 표에는 합계가 없었다(실측 2026-09-08: 7 개 해가 있는데 전체 기간 줄이 없어 "지금까지 배당을 얼마 받았나" 를 종목별 표 합계에서 찾아야 했다).
   *
   * <p>금액·건수는 연도 합이고, 원금과 수익률은 연도를 더한 값이 아니라 전체 기간의 값(종목별 표 합계행과 같은 값)이다. 연도별 일평균 원금을 더하면 뜻이 없다.
   */
  @Test
  void 연도가_둘_이상이면_전체_기간_줄이_붙고_금액은_연도_합이다() {
    List<DividendYieldGroupView> rows =
        List.of(row("A", "1000000", "846000", "1000000", "846000", "10000000", "12000000"));
    List<DividendYieldGroupView> yearly =
        List.of(
            row("2025", "600000", "507600", "600000", "507600", "9000000", "11000000"),
            row("2026", "400000", "338400", "400000", "338400", "11000000", "13000000"));

    List<String> cells = yearlyTotalCells(renderWithYearly(rows, yearly));

    assertThat(cells).as("전체 기간 줄이 없다").isNotEmpty();
    assertThat(cells.get(1)).as("세전 = 연도 합").isEqualTo("1,000,000");
    assertThat(cells.get(2)).as("세후 = 연도 합").isEqualTo("846,000");
    assertThat(cells.get(3)).as("세금 = 세전 − 세후").isEqualTo("154,000");
    assertThat(cells.get(9)).as("건수 = 연도 합").isEqualTo("2");
    assertThat(cells.get(5)).as("원금은 연도 합이 아니라 전체 기간 값(종목별 표 합계행과 같다)").isEqualTo("10,000,000");
    assertThat(cells.get(6)).as("수익률도 전체 기간 값").isEqualTo(footerCells(render(rows)).get(6));
  }

  /** 한 해뿐이면 그 줄이 곧 전체다. 연도별 성과 표와 같은 규칙. */
  @Test
  void 연도가_하나면_전체_기간_줄이_없다() {
    List<DividendYieldGroupView> rows =
        List.of(row("A", "1000000", "846000", "1000000", "846000", "10000000", "12000000"));
    List<DividendYieldGroupView> yearly =
        List.of(row("2026", "1000000", "846000", "1000000", "846000", "10000000", "12000000"));

    assertThat(yearlyTotalCells(renderWithYearly(rows, yearly))).isEmpty();
  }

  /**
   * 세금·과세금액이 0 이면 '-' 로 적는다.
   *
   * <p>실측 2026-09-08: 배당 내역 202 줄 중 세금 0 이 147 줄, 과세금액 0 이 119 줄이라 표가 0 으로 도배됐다. 다른 표(연도별 세금·비용, 매매
   * 쪼갬)는 이미 0 을 '-' 로 적고 있어 같은 값이 화면마다 다르게 보였다.
   */
  @Test
  void 세금과_과세금액이_0_이면_대시로_적는다() {
    String html =
        render(List.of(row("면세", "500000", "500000", "0", "500000", "10000000", "12000000")));
    int start = html.indexOf("<tr class=\"dividend-yield-stock-row\"");
    assertThat(start).isGreaterThan(0);
    List<String> cells = new ArrayList<>();
    Matcher matcher = CELL.matcher(html.substring(start, html.indexOf("</tr>", start)));
    while (matcher.find()) {
      cells.add(text(matcher.group(1)));
    }

    assertThat(cells.get(3)).as("세금 0").isEqualTo("-");
    assertThat(cells.get(4)).as("과세금액 0").isEqualTo("-");
    assertThat(cells.get(2)).as("세후액은 값 그대로").isEqualTo("500,000");
  }

  /** 분모(일평균 투입원금)가 없으면 수익률은 0.00% 가 아니라 '-' 다. 0% 는 '못 벌었다' 로 읽힌다. */
  @Test
  void 원금이_없으면_수익률은_대시다() {
    String html = render(List.of(row("원금없음", "500000", "423000", "500000", "423000", "0", "0")));
    int start = html.indexOf("<tr class=\"dividend-yield-stock-row\"");
    List<String> cells = new ArrayList<>();
    Matcher matcher = CELL.matcher(html.substring(start, html.indexOf("</tr>", start)));
    while (matcher.find()) {
      cells.add(text(matcher.group(1)));
    }

    assertThat(cells.get(6)).isEqualTo("-");
  }

  /** 배당 내역 목록도 같은 규칙이다. 그 조각은 파라미터가 많아 소스로 본다. */
  @Test
  void 배당_내역_목록의_세금_0_도_대시다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte"),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(source)
        .contains("item.tax().signum() != 0 ? decimalFormat.format(item.tax()) : \"-\"")
        .contains(
            "item.taxableAmount().signum() != 0 ? decimalFormat.format(item.taxableAmount()) : \"-\"")
        .doesNotContain("decimalFormat.format(item.tax()) : \"0\"")
        .doesNotContain("decimalFormat.format(item.taxableAmount()) : \"0\"");
  }
}
