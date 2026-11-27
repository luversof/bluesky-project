package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendTaxReport;
import net.luversof.web.gate.stock.dto.response.DividendTaxReport.Scenario;
import net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView;

/** 종소세 신고 자료 구역의 귀속연도 선택과, 금융소득 기준 카드가 신고 자료의 판정 금액을 쓰는지. 수치는 지어낸 값. */
class DividendTaxReportViewTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 28);

  @Test
  void 귀속연도는_기본이_직전_연도이고_범위_밖이면_직전_연도로_돌린다() {
    assertThat(DividendTaxReportSupport.resolveYear(null, TODAY)).isEqualTo(2025);
    assertThat(DividendTaxReportSupport.resolveYear(2026, TODAY)).isEqualTo(2026);
    assertThat(DividendTaxReportSupport.resolveYear(2023, TODAY)).isEqualTo(2023);
    assertThat(DividendTaxReportSupport.resolveYear(2022, TODAY)).isEqualTo(2025);
    assertThat(DividendTaxReportSupport.resolveYear(2027, TODAY)).isEqualTo(2025);
  }

  private static Scenario scenario(String financial) {
    return new Scenario(
        new BigDecimal(financial),
        false,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        null,
        null,
        false,
        null);
  }

  private static DividendTaxReport report(String recommendation, Scenario without, Scenario with) {
    return new DividendTaxReport(
        2026,
        true,
        true,
        true,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        null,
        2,
        without,
        with,
        recommendation,
        List.of());
  }

  private static FinancialIncomeThresholdView view() {
    return new FinancialIncomeThresholdView(
        2026,
        TODAY,
        new BigDecimal("100"),
        0,
        new BigDecimal("200"),
        FinancialIncomeThresholdView.THRESHOLD,
        90,
        BigDecimal.ZERO,
        33,
        BigDecimal.ZERO,
        0,
        false);
  }

  @Test
  void 분리과세가_유리하면_카드는_신청_기준_판정_금액을_쓴다() {
    FinancialIncomeThresholdView result =
        FinancialIncomeThresholdSupport.applyJudgement(
            view(),
            report("WITH", scenario("30000000"), scenario("12000000")),
            report("WITHOUT", scenario("9000000"), null));

    assertThat(result.ytdTaxable()).isEqualByComparingTo("12000000");
    assertThat(result.lastYearTaxable()).isEqualByComparingTo("9000000");
    assertThat(result.unknownTaxableCount()).isEqualTo(2);
    assertThat(result.separationApplied()).isTrue();
  }

  @Test
  void 종목별_구성은_계좌를_합치고_과세이연은_빼고_대상_먼저_큰_차례다() {
    java.util.UUID a1 = java.util.UUID.randomUUID();
    java.util.UUID a2 = java.util.UUID.randomUUID();
    java.util.UUID etf = java.util.UUID.randomUUID();
    java.util.UUID stock = java.util.UUID.randomUUID();
    var rows =
        List.of(
            new DividendTaxReport.Row(
                a1,
                etf,
                "가상 ETF",
                "GENERAL",
                BigDecimal.TEN,
                new BigDecimal("500"),
                null,
                1,
                false),
            new DividendTaxReport.Row(
                a1,
                stock,
                "가상전자",
                "HIGH_DIVIDEND",
                BigDecimal.TEN,
                new BigDecimal("100"),
                null,
                1,
                true),
            new DividendTaxReport.Row(
                a2,
                stock,
                "가상전자",
                "HIGH_DIVIDEND",
                BigDecimal.TEN,
                new BigDecimal("50"),
                null,
                1,
                true),
            new DividendTaxReport.Row(
                a2, etf, "가상 ETF", "DEFERRED", BigDecimal.TEN, null, null, 1, false));
    var report =
        new DividendTaxReport(
            2026,
            false,
            true,
            true,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            null,
            0,
            scenario("0"),
            null,
            null,
            rows);

    assertThat(report.taxableStocks())
        .extracting(
            DividendTaxReport.StockTotal::stockItemName,
            s -> s.taxableAmount().intValue(),
            DividendTaxReport.StockTotal::grossUpEligible)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("가상전자", 150, true),
            org.assertj.core.groups.Tuple.tuple("가상 ETF", 500, false));
  }

  @Test
  void 신고_자료가_없으면_직접_더한_값을_그대로_둔다() {
    FinancialIncomeThresholdView result =
        FinancialIncomeThresholdSupport.applyJudgement(view(), null, null);

    assertThat(result.ytdTaxable()).isEqualByComparingTo("100");
    assertThat(result.separationApplied()).isFalse();
  }

  @Test
  void 신청_안_함이면_신청_안_함_기준이고_작년_자료가_없으면_작년_값을_둔다() {
    FinancialIncomeThresholdView result =
        FinancialIncomeThresholdSupport.applyJudgement(
            view(), report("WITHOUT", scenario("25000000"), scenario("10000000")), null);

    assertThat(result.ytdTaxable()).isEqualByComparingTo("25000000");
    assertThat(result.lastYearTaxable()).isEqualByComparingTo("200");
    assertThat(result.separationApplied()).isFalse();
  }
}
