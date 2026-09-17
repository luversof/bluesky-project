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
import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;
import net.luversof.web.gate.stock.service.MonthlyDividendCalculator;

/**
 * 월배당 비교표의 평단 기준 칸은 현재가 기준보다 높은지 낮은지를 한눈에 말한다.
 *
 * <p>사용자 요청 2026-09-17: "수익률의 현재가 기준에 비해 평단 기준이 높은지 낮은지를 인지하기 쉽게". 고른 방식은 차이 표시 &mdash; 연 평균 기준 차이를
 * 기호(▲/▼) · 부호 · 손익 색으로 함께 적는다. 색만으로 말하면 색각 · 고대비 환경에서 사라지고, 차이의 크기도 안 보인다. 두 수익률은 같은 배당을 평단 원가와 현재
 * 평가액으로 나눈 것이라 방향은 하나로 정해져 한 줄만 적는다.
 */
class MonthlyDividendOnCostGapTest {

  private static final String FRAGMENT = "stock/fragments/monthlyDividendSimulator.jte";

  private static final String QUOTE = String.valueOf((char) 34);

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

  private static MonthlyDividendSnapshotResponse row(
      String symbol, String annualYield, String annualYieldOnCost) {
    BigDecimal ten = BigDecimal.TEN;
    return new MonthlyDividendSnapshotResponse(
        UUID.nameUUIDFromBytes(symbol.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        null,
        UUID.nameUUIDFromBytes(
            ("item-" + symbol).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        symbol,
        symbol + " NAME",
        LocalDate.parse("2026-09-02"),
        ten,
        ten,
        new BigDecimal("50"),
        100,
        ten,
        ten,
        new BigDecimal("1000"),
        new BigDecimal("1000"),
        BigDecimal.ONE,
        annualYield == null ? null : new BigDecimal(annualYield),
        BigDecimal.ONE,
        annualYieldOnCost == null ? null : new BigDecimal(annualYieldOnCost),
        new BigDecimal("100"),
        BigDecimal.ONE,
        BigDecimal.ONE,
        null);
  }

  private static String render(List<MonthlyDividendSnapshotResponse> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("monthlyDividendRows", rows);
    model.put(
        "monthlyDividendSummary", new MonthlyDividendCalculator().buildSimulatorSummary(rows));
    model.put("monthlyDividendKeyword", "");
    model.put("monthlyDividendHasSavedRows", !rows.isEmpty());
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(FRAGMENT, model, output);
    return output.toString();
  }

  private static String rowHtml(String html, String symbol) {
    int at = html.indexOf("data-symbol=" + QUOTE + symbol + QUOTE);
    assertThat(at).as(symbol + " 행을 못 찾았다 - 검사가 헛돈다").isGreaterThan(0);
    return html.substring(at, html.indexOf("</tr>", at));
  }

  private static String gap(String rowHtml) {
    int at = rowHtml.indexOf("data-on-cost-gap=");
    assertThat(at).as("차이 표시가 없다").isGreaterThan(0);
    int start = rowHtml.lastIndexOf("<div", at);
    return rowHtml.substring(start, rowHtml.indexOf("</div>", at) + "</div>".length());
  }

  private static String message(String key, String amount) {
    return java.text.MessageFormat.format(MessageUtil.getMessage(key), amount);
  }

  private static List<MonthlyDividendSnapshotResponse> rows() {
    return List.of(
        row("UPP", "10.00", "12.40"),
        row("DWN", "13.20", "11.20"),
        row("EQL", "5.00", "5.004"),
        row("NUL", "5.00", null));
  }

  @Test
  void 평단_기준이_높으면_위_기호와_수익_색으로_차이를_적는다() {
    String html = gap(rowHtml(render(rows()), "UPP"));

    assertThat(html)
        .contains("data-on-cost-gap=" + QUOTE + "higher" + QUOTE)
        .contains("data-on-cost-gap-pct=" + QUOTE + "2.40" + QUOTE)
        .contains("text-profit")
        .as("보이는 글자는 기호 + 부호 + 차이")
        .contains(
            "<span aria-hidden="
                + QUOTE
                + "true"
                + QUOTE
                + ">▲ "
                + message("stock.simulator.monthly.table.cell.on.cost.gap.value", "+2.40")
                + "</span>")
        .as("낭독기에는 문장으로 - 기호는 읽히지 않는다")
        .contains(
            "<span class="
                + QUOTE
                + "sr-only"
                + QUOTE
                + ">"
                + message("stock.simulator.monthly.table.cell.on.cost.gap.higher", "2.40")
                + "</span>");
  }

  @Test
  void 평단_기준이_낮으면_아래_기호와_손실_색으로_차이를_적는다() {
    String html = gap(rowHtml(render(rows()), "DWN"));

    assertThat(html)
        .contains("data-on-cost-gap=" + QUOTE + "lower" + QUOTE)
        .contains("data-on-cost-gap-pct=" + QUOTE + "-2.00" + QUOTE)
        .contains("text-loss")
        .contains(
            ">▼ "
                + message("stock.simulator.monthly.table.cell.on.cost.gap.value", "-2.00")
                + "</span>")
        .contains(message("stock.simulator.monthly.table.cell.on.cost.gap.lower", "2.00"));
  }

  /** 표에 찍히는 자릿수(소수 둘째)로 같으면 같다 - 0 에는 부호도 기호도 붙이지 않는다(0.004 차이를 ▲ 로 적으면 "높다" 로 읽힌다). */
  @Test
  void 표시_자릿수로_같으면_기호_없이_같다고_적는다() {
    String html = gap(rowHtml(render(rows()), "EQL"));

    assertThat(html)
        .contains("data-on-cost-gap=" + QUOTE + "same" + QUOTE)
        .contains("text-base-content/60")
        .doesNotContain("text-profit")
        .doesNotContain("text-loss")
        .doesNotContain("▲")
        .doesNotContain("▼")
        .contains(
            ">"
                + message("stock.simulator.monthly.table.cell.on.cost.gap.value", "0.00")
                + "</span>")
        .contains(MessageUtil.getMessage("stock.simulator.monthly.table.cell.on.cost.gap.same"));
  }

  @Test
  void 수익률을_모르면_차이를_적지_않는다() {
    assertThat(rowHtml(render(rows()), "NUL")).doesNotContain("data-on-cost-gap");
  }

  /** 차이는 평단 기준 칸 안에 있어야 한다 - 옆 합산 수익률 칸에 붙으면 무엇과 무엇의 차이인지 읽히지 않는다. */
  @Test
  void 차이는_평단_기준_칸_안에_있고_기준을_팁에서_밝힌다() {
    String rendered = render(rows());
    String row = rowHtml(rendered, "UPP");
    int onCostAnnual = row.indexOf("12.40%");
    int gapAt = row.indexOf("data-on-cost-gap=");
    int cellEnd = row.indexOf("</td>", onCostAnnual);

    assertThat(onCostAnnual).as("평단 기준 연 수익률 글자를 못 찾았다").isGreaterThan(0);
    assertThat(gapAt).isBetween(onCostAnnual, cellEnd);
    assertThat(rendered).contains(MessageUtil.getMessage("stock.simulator.monthly.criteria.six"));
  }
}
