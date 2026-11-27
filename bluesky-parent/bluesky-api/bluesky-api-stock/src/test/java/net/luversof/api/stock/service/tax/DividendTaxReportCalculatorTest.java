package net.luversof.api.stock.service.tax;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.HighDividendDesignation;
import net.luversof.api.stock.service.tax.DividendTaxReportCalculator.TaxDividend;
import net.luversof.api.stock.web.dto.response.DividendTaxReport;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Category;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Recommendation;

/** 종소세 신고 자료 계산. 수치는 모두 지어낸 값이고 기대값은 손으로 계산했다. */
class DividendTaxReportCalculatorTest {

  private static final UUID TAXABLE_ACCOUNT = UUID.randomUUID();
  private static final UUID DEFERRED_ACCOUNT = UUID.randomUUID();
  private static final UUID STOCK = UUID.randomUUID();
  private static final UUID ETF = UUID.randomUUID();

  private static TaxDividend dividend(
      UUID account, UUID item, String name, LocalDate payDate, long taxable) {
    BigDecimal t = BigDecimal.valueOf(taxable);
    return new TaxDividend(
        account,
        item,
        name,
        payDate,
        t,
        t,
        TaxRuleTable.floor(t.multiply(new BigDecimal("0.154"))));
  }

  private static HighDividendDesignation designation(UUID item, LocalDate from, LocalDate to) {
    HighDividendDesignation d = new HighDividendDesignation();
    d.setStockItemId(item);
    d.setBusinessYear(from.getYear());
    d.setPayDateFrom(from);
    d.setPayDateTo(to);
    return d;
  }

  @Test
  void 기본세율_구간_경계() {
    TaxRuleTable rule = TaxRuleTable.forYear(2026);
    assertThat(rule.basicTax(new BigDecimal("14000000"))).isEqualByComparingTo("840000");
    assertThat(rule.basicTax(new BigDecimal("14000001"))).isEqualByComparingTo("840000");
    assertThat(rule.basicTax(new BigDecimal("50000000"))).isEqualByComparingTo("6240000");
    assertThat(rule.basicTax(new BigDecimal("61000000"))).isEqualByComparingTo("8880000");
    assertThat(rule.basicTax(BigDecimal.ZERO)).isEqualByComparingTo("0");
  }

  @Test
  void 고배당_분리과세_구간은_몫마다_세율을_곱한다() {
    TaxRuleTable rule = TaxRuleTable.forYear(2026);
    assertThat(rule.highDividendTax(new BigDecimal("10000000"))).isEqualByComparingTo("1400000");
    // 2천만 x 14% + 1천만 x 20%
    assertThat(rule.highDividendTax(new BigDecimal("30000000"))).isEqualByComparingTo("4800000");
    assertThat(TaxRuleTable.forYear(2030).highDividendRule()).isFalse();
    assertThat(TaxRuleTable.forYear(2025).highDividendRule()).isFalse();
  }

  @Test
  void 기준_이하면_신고가_필요_없고_과세이연_계좌는_빠진다() {
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2026,
            false,
            List.of(
                dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2026, 4, 1), 8_000_000),
                dividend(DEFERRED_ACCOUNT, ETF, "KODEX 가상", LocalDate.of(2026, 5, 1), 5_000_000),
                dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2025, 12, 31), 9_000_000)),
            Set.of(DEFERRED_ACCOUNT),
            List.of(),
            null,
            null);

    assertThat(report.generalTaxable()).isEqualByComparingTo("8000000");
    assertThat(report.deferredGross()).isEqualByComparingTo("5000000");
    assertThat(report.withoutSeparation().comprehensive()).isFalse();
    assertThat(report.withoutSeparation().additionalNationalTax()).isEqualByComparingTo("0");
    assertThat(report.withoutSeparation().isaRestricted()).isFalse();
    assertThat(report.withSeparation()).isNull();
    assertThat(report.recommendation()).isEqualTo(Recommendation.NONE_NEEDED);
    assertThat(report.rows())
        .extracting(DividendTaxReport.Row::category)
        .containsExactlyInAnyOrder(Category.GENERAL, Category.DEFERRED);
  }

  @Test
  void 기준을_넘으면_비교과세로_추가_납부액을_낸다() {
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2025,
            false,
            List.of(dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2025, 4, 1), 30_000_000)),
            Set.of(),
            List.of(),
            new BigDecimal("50000000"),
            null);

    var s = report.withoutSeparation();
    assertThat(s.comprehensive()).isTrue();
    assertThat(s.excess()).isEqualByComparingTo("10000000");
    assertThat(s.grossUp()).isEqualByComparingTo("1000000");
    // 2,800,000 + 기본세율(61,000,000)=8,880,000 - Gross-up 1,000,000 - 기본세율(50,000,000)=6,240,000 - 원천
    // 4,200,000
    assertThat(s.additionalNationalTax()).isEqualByComparingTo("240000");
    assertThat(s.additionalTotalTax()).isEqualByComparingTo("264000");
    assertThat(s.isaRestricted()).isTrue();
    assertThat(report.recommendation()).isEqualTo(Recommendation.WITHOUT);
  }

  @Test
  void 고배당_분리과세를_신청하면_기준_아래로_내려가_유리하다() {
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2026,
            true,
            List.of(
                dividend(TAXABLE_ACCOUNT, ETF, "KODEX 가상", LocalDate.of(2026, 3, 1), 15_000_000),
                dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2026, 5, 1), 10_000_000)),
            Set.of(),
            List.of(designation(STOCK, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))),
            new BigDecimal("50000000"),
            null);

    assertThat(report.highDividendTaxable()).isEqualByComparingTo("10000000");
    var without = report.withoutSeparation();
    assertThat(without.financialIncome()).isEqualByComparingTo("25000000");
    // ETF 분배금은 Gross-up 대상이 아니라 초과분 500 만은 고배당 배당(법인 배당)에서 온다
    assertThat(without.grossUp()).isEqualByComparingTo("500000");
    // 2,800,000 + 7,560,000 - 500,000 - 6,240,000 - 3,500,000
    assertThat(without.additionalNationalTax()).isEqualByComparingTo("120000");

    var with = report.withSeparation();
    assertThat(with.financialIncome()).isEqualByComparingTo("15000000");
    assertThat(with.comprehensive()).isFalse();
    assertThat(with.highDividendSeparateTax()).isEqualByComparingTo("1400000");
    assertThat(with.additionalNationalTax()).isEqualByComparingTo("0");
    assertThat(with.isaRestricted()).isFalse();
    assertThat(report.recommendation()).isEqualTo(Recommendation.WITH);
  }

  @Test
  void 다른_종합소득을_모르면_세액은_비우고_판정만_한다() {
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2026,
            false,
            List.of(
                dividend(TAXABLE_ACCOUNT, ETF, "KODEX 가상", LocalDate.of(2026, 3, 1), 15_000_000),
                dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2026, 5, 1), 10_000_000)),
            Set.of(),
            List.of(designation(STOCK, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))),
            null,
            null);

    assertThat(report.withoutSeparation().comprehensive()).isTrue();
    assertThat(report.withoutSeparation().additionalNationalTax()).isNull();
    assertThat(report.withSeparation().additionalNationalTax()).isEqualByComparingTo("0");
    assertThat(report.recommendation()).isEqualTo(Recommendation.WITH);
  }

  @Test
  void 앱_밖_금융소득도_판정에_들어가고_빈_과세금액은_센다() {
    TaxDividend unknown =
        new TaxDividend(
            TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2026, 6, 1), BigDecimal.TEN, null, null);
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2026,
            false,
            List.of(
                dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2026, 4, 1), 15_000_000),
                unknown),
            Set.of(),
            List.of(),
            null,
            new BigDecimal("6000000"));

    assertThat(report.unknownTaxableCount()).isEqualTo(1);
    assertThat(report.withoutSeparation().financialIncome()).isEqualByComparingTo("21000000");
    assertThat(report.withoutSeparation().comprehensive()).isTrue();
  }

  @Test
  void 특례_기간_밖이면_지정이_있어도_일반_배당이다() {
    DividendTaxReport report =
        DividendTaxReportCalculator.compute(
            2025,
            false,
            List.of(dividend(TAXABLE_ACCOUNT, STOCK, "가상전자", LocalDate.of(2025, 5, 1), 1_000_000)),
            Set.of(),
            List.of(designation(STOCK, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))),
            null,
            null);

    assertThat(report.highDividendTaxable()).isEqualByComparingTo("0");
    assertThat(report.generalTaxable()).isEqualByComparingTo("1000000");
    assertThat(report.withSeparation()).isNull();
  }

  @Test
  void ETF_와_리츠는_Gross_up_대상이_아니다() {
    assertThat(DividendTaxReportCalculator.grossUpEligible("가상전자")).isTrue();
    assertThat(DividendTaxReportCalculator.grossUpEligible("KODEX 가상")).isFalse();
    assertThat(DividendTaxReportCalculator.grossUpEligible("가상리츠")).isFalse();
    assertThat(DividendTaxReportCalculator.grossUpEligible(null)).isFalse();
  }
}
