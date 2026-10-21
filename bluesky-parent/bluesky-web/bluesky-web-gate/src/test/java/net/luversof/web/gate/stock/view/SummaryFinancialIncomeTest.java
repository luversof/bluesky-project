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
    Map<String, Object> params = new HashMap<>();
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
    BigDecimal z = BigDecimal.ZERO;
    params.put(
        "periodSummary",
        new PeriodSummary(z, z, z, z, z, 0, 0, z, z, z, z, new BigDecimal("12000000"), 40));
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
}
