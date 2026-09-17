package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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
import net.luversof.web.gate.stock.controller.StockDividendHtmxController.DividendChange;

/**
 * 전기 대비 '변동 요인' 표는 제 가로 스크롤 상자 안에 있다.
 *
 * <p>이 표만 상자가 없었다 &mdash; 실측 2026-09-17(17 화면 x 폭 · 글꼴 5 조합에서 상자 없이 넘친 표는 이것 하나): 글꼴 200% 에서 320px
 * 문서 219px, 640px 표 321px, 1024px 표 385px 넘쳤다. 이번달 · 1년처럼 전기가 있는 보기에서만 나오는 표라 전체 기간으로 훑던 탐침이 못 봤다.
 * 종목 이름 칸의 {@code max-w-0 truncate} 는 표 자동 폭에서 듣지 않아(칸 max-width 무시) 이름이 줄지 않는다 &mdash; 그래서 줄이는 대신
 * 상자 안에서 스크롤한다. 스크롤 상자는 전역 규칙으로 {@code position: relative} 라 안쪽 sr-only 가 문서를 밀지 않는다.
 */
class DividendChangeTableScrollTest {

  private static final String FRAGMENT = "stock/htmx/fragments/dividend/dividendSummaryCards.jte";

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

  private static String render(boolean hasComparison, List<DividendChange> changes) {
    BigDecimal zero = BigDecimal.ZERO;
    Map<String, Object> model = new HashMap<>();
    model.put("decimalFormat", new DecimalFormat("#,##0"));
    model.put("safeAllNet", new BigDecimal("1000"));
    model.put("safeAllGross", new BigDecimal("1000"));
    model.put("safeAllTaxable", zero);
    model.put("safeAllTax", zero);
    model.put("allDeduction", zero);
    model.put("allOtherDeduction", zero);
    model.put("safePrevNet", new BigDecimal("800"));
    model.put("hasComparison", hasComparison);
    model.put("diffPositive", true);
    model.put("diffPct", "25.0");
    model.put("diffAmount", new BigDecimal("200"));
    model.put("totalItemsCountText", "");
    model.put("prevStartDate", LocalDate.parse("2026-08-01"));
    model.put("prevEndDate", LocalDate.parse("2026-08-31"));
    model.put("dividendChangeContributors", changes);
    model.put("taxLabel", "");
    model.put("taxableAmountLabel", "");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(FRAGMENT, model, output);
    return output.toString();
  }

  private static List<DividendChange> changes() {
    return List.of(
        new DividendChange(
            "TIGER 코리아배당다우존스위클리커버드콜",
            new BigDecimal("1886082"),
            new BigDecimal("0"),
            new BigDecimal("1886082"),
            UUID.randomUUID()),
        new DividendChange(
            "RISE 200위클리커버드콜",
            new BigDecimal("100"),
            new BigDecimal("300"),
            new BigDecimal("-200"),
            UUID.randomUUID()));
  }

  @Test
  void 변동_요인_표는_가로_스크롤_상자_바로_안에_있다() {
    String html = render(true, changes());
    int box = html.indexOf("data-dividend-change-table-scroll");
    assertThat(box).as("스크롤 상자가 없다").isGreaterThan(0);

    String boxTag = html.substring(html.lastIndexOf("<div", box), html.indexOf('>', box) + 1);
    assertThat(boxTag).as("상자가 가로 스크롤을 건다").contains("overflow-x-auto");

    int table = html.indexOf("<table", box);
    int rows = html.indexOf("data-dividend-change-row", box);
    int tableEnd = html.indexOf("</table>", box);
    assertThat(html.substring(html.indexOf('>', box) + 1, table).trim())
        .as("상자와 표 사이에 다른 것이 없다")
        .isEmpty();
    assertThat(rows).as("변동 요인 줄이 그 표 안에 있다").isGreaterThan(table).isLessThan(tableEnd);
    assertThat(html.substring(tableEnd + "</table>".length()).trim())
        .as("표를 닫으면 곧 상자를 닫는다")
        .startsWith("</div>");
  }

  @Test
  void 전기가_없으면_표도_상자도_없다() {
    assertThat(render(false, changes()))
        .doesNotContain("data-dividend-change-table-scroll")
        .doesNotContain("data-dividend-change-row");
  }
}
