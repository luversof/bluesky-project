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
        List.of(row("전량매도", "120000", "102040", "120000", "0", "5000000", "0"));

    String html = render(rows);

    assertThat(html).as("걸러진 분자가 0 이면 행 수익률도 0 이어야 한다").contains("0.00%");
    // 세후액(102,040)은 그대로 보이지만 수익률 분자로는 쓰이지 않는다.
    assertThat(html).contains("102,040");
    assertThat(footerCells(html)).as("합계행도 같은 규칙이라 0.00% 여야 한다").contains("0.00%");
  }
}
