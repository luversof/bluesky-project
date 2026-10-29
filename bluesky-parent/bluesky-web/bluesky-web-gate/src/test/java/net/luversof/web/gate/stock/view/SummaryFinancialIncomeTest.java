package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.PeriodSummary;
import net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView;

/** 대시보드 올해 줄 배당 카드의 금융소득 종합과세 기준 비율(2026-10-20 자율 개선). 조회가 실패하면(null) 그 말만 빠진다. */
class SummaryFinancialIncomeTest {

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static String render(FinancialIncomeThresholdView financialIncome) {
    return render(financialIncome, null);
  }

  private static String render(
      FinancialIncomeThresholdView financialIncome, PeriodSummary previousPeriodSummary) {
    return render(financialIncome, previousPeriodSummary, BigDecimal.ZERO);
  }

  private static String render(
      FinancialIncomeThresholdView financialIncome,
      PeriodSummary previousPeriodSummary,
      BigDecimal realizedThisYear) {
    BigDecimal z = BigDecimal.ZERO;
    return render(
        financialIncome,
        previousPeriodSummary,
        new PeriodSummary(
            z, z, z, z, realizedThisYear, 0, 0, z, z, z, z, new BigDecimal("12000000"), 40));
  }

  private static String render(
      FinancialIncomeThresholdView financialIncome,
      PeriodSummary previousPeriodSummary,
      PeriodSummary periodSummary) {
    Map<String, Object> params = new HashMap<>();
    params.put("previousPeriodSummary", previousPeriodSummary);
    params.put("totalAsset", new BigDecimal("1500000000"));
    params.put("totalUnrealizedProfit", new BigDecimal("900000000"));
    params.put("totalRealizedProfit", new BigDecimal("200000000"));
    params.put("totalDividend", new BigDecimal("60000000"));
    params.put("winRate", 0.62);
    params.put("displayPrincipal", new BigDecimal("620000000"));
    params.put("displayCurrentEvaluationProfit", new BigDecimal("910000000"));
    params.put("combinedAdjustmentAmount", BigDecimal.ZERO);
    params.put("holdingFeeAdjustment", BigDecimal.ZERO);
    params.put("manualPrincipalAdjustment", BigDecimal.ZERO);
    params.put("periodSummary", periodSummary);
    params.put("periodSummaryFrom", LocalDate.parse("2026-01-01"));
    params.put("periodSummaryTo", LocalDate.parse("2026-10-20"));
    params.put("financialIncome", financialIncome);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/summary.jte", params, output);
    return output.toString();
  }

  @Test
  void 올해_배당_카드에_기준_대비_비율을_붙인다() {
    FinancialIncomeThresholdView view =
        new FinancialIncomeThresholdView(
            2026,
            LocalDate.parse("2026-10-20"),
            new BigDecimal("8182185"),
            0,
            new BigDecimal("13705309"),
            FinancialIncomeThresholdView.THRESHOLD,
            90,
            BigDecimal.ZERO,
            72,
            BigDecimal.ZERO,
            0);
    String expected =
        java.text.MessageFormat.format(
            MessageUtil.getMessage("stock.summary.financial.income.pct"), "40.9");

    assertThat(render(view)).contains(expected);
    assertThat(render(null)).as("조회 실패면 그 말만 빠진다").doesNotContain(expected);
  }

  /** 작년 같은 기간(2026-10-28): 실현 손익 · 받은 배당 카드 둘째 줄. 배당은 증감률도. 없으면(null) 그 줄이 없다. */
  @Test
  void 작년_같은_기간을_실현_손익과_배당_카드에_붙인다() {
    BigDecimal z = BigDecimal.ZERO;
    PeriodSummary previous =
        new PeriodSummary(
            z,
            z,
            z,
            z,
            new BigDecimal("-3000000"),
            0,
            0,
            z,
            z,
            z,
            z,
            new BigDecimal("10000000"),
            30);
    String html = render(null, previous);

    assertThat(html)
        .contains("stat-card-sub text-loss\">")
        .contains("<span class=\"amount-value\">-300만</span>")
        .contains("<span class=\"amount-value\">1,000만</span> (+20.0%)");
    assertThat(render(null, null)).doesNotContain("-300만").doesNotContain("(+20.0%)");
  }

  /** 작년 줄 색은 작년 부호를 따른다 - 올해가 손실이어도 작년 이익은 이익 색(검토 지적 2026-10-28: 카드 색을 물려받아 손실 색이었다). */
  @Test
  void 작년_실현_손익_색은_작년_부호를_따른다() {
    BigDecimal z = BigDecimal.ZERO;
    PeriodSummary previousGain =
        new PeriodSummary(z, z, z, z, new BigDecimal("4570000"), 0, 0, z, z, z, z, z, 0);
    String html = render(null, previousGain, new BigDecimal("-1000000"));

    assertThat(html)
        .contains("stat-card-sub text-profit\">작년 같은 기간 <span class=\"amount-value\">+457만</span>");
  }

  /** 거래 비용 카드: 매매(매수+매도) 금액 대비 비율을 보조 줄에, 수수료 · 세금 나눔은 툴팁에(2026-10-28 12차 자율). 매매가 없으면 비율은 빠진다. */
  @Test
  void 거래_비용_카드에_매매_금액_대비_비율을_붙인다() {
    BigDecimal z = BigDecimal.ZERO;
    String html =
        render(
            null,
            null,
            new PeriodSummary(
                new BigDecimal("200000000"),
                new BigDecimal("100000000"),
                new BigDecimal("400000"),
                new BigDecimal("200000"),
                z,
                3,
                2,
                z,
                z,
                z,
                z,
                z,
                0));

    assertThat(html).contains("0.20%").contains("400,000").contains("200,000");
    assertThat(render(null))
        .as("매매가 없으면 비율 줄이 없다")
        .doesNotContain("(buy + sell)")
        .doesNotContain("(매수 + 매도)");
  }
}
