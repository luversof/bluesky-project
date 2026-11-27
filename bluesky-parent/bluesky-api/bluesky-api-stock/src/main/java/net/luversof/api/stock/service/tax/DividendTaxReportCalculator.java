package net.luversof.api.stock.service.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.luversof.api.stock.domain.HighDividendDesignation;
import net.luversof.api.stock.web.dto.response.DividendTaxReport;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.BasicTax;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Category;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Detail;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Recommendation;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Row;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Scenario;
import net.luversof.api.stock.web.dto.response.DividendTaxReport.Slice;

/**
 * 귀속연도 배당의 종합소득세 신고 자료 계산(DB 없음).
 *
 * <p>금융소득 = 과세 계좌 배당의 과세금액 + 앱 밖 금융소득. 2천만 원 이하면 14% 원천징수로 끝나고, 넘으면 비교과세(소득세법 제62조)로 {@code max(2천만
 * x 14% + 기본세율(다른 종합소득 + 초과분 + Gross-up) - Gross-up, 금융소득 x 14% + 기본세율(다른 종합소득))} 를 낸다. 추가 납부액은 여기서
 * 다른 종합소득만의 세액과 원천징수(14%)를 뺀 몫이다. 배당세액공제 한도를 넣으면 이 식이 된다.
 *
 * <p>고배당 분리과세는 신고 때 고르는 선택이라 "신청 안 함 / 신청함" 두 시나리오를 함께 낸다. 신청하면 고배당 배당은 판정과 종합과세에서 빠지고 따로 누진 구간세율로
 * 과세된다.
 *
 * <p>원천징수는 과세금액의 14% 로 본다(원장 세금 칸에는 지방소득세가 함께 들어 있어 국세만 떼어 낼 수 없다). 세액은 국세이고, {@code
 * additionalTotalTax} 만 지방소득세(국세의 10%)를 더한 값이다.
 */
public final class DividendTaxReportCalculator {

  /**
   * Gross-up 대상이 아닌 배당(ETF 분배금 · 리츠). 법인세를 낸 국내 법인의 배당만 대상이다.
   *
   * <p>shortcut: 종목 종류 칸이 없어 이름의 운용사 접두어로 ETF 를 가른다. 새 운용사가 생기면 여기에 더하거나, 종목 종류를 저장하게 되면 그 값으로 바꾼다.
   */
  static final Pattern NOT_GROSS_UP =
      Pattern.compile(
          "^(KODEX|TIGER|RISE|KBSTAR|PLUS|ACE|SOL|KoAct|TIMEFOLIO|HANARO|ARIRANG|KOSEF|"
              + "1Q|WON|BNK|TRUSTON|UNICORN|마이티|히어로즈|에셋플러스|파워)\\b.*|.*리츠.*");

  /** 계산에 쓰는 배당 한 건. 날짜는 서버 존(Asia/Seoul) 기준 지급일. */
  public record TaxDividend(
      UUID accountId,
      UUID stockItemId,
      String stockItemName,
      LocalDate payDate,
      BigDecimal grossAmount,
      BigDecimal taxableAmount,
      BigDecimal tax) {}

  private DividendTaxReportCalculator() {}

  public static DividendTaxReport compute(
      int year,
      boolean inProgress,
      List<TaxDividend> dividends,
      Set<UUID> deferredAccountIds,
      Collection<HighDividendDesignation> designations,
      BigDecimal otherIncomeTaxBase,
      BigDecimal otherFinancialIncome) {
    TaxRuleTable rule = TaxRuleTable.forYear(year);
    BigDecimal general = BigDecimal.ZERO;
    BigDecimal generalGrossUpEligible = BigDecimal.ZERO;
    BigDecimal high = BigDecimal.ZERO;
    BigDecimal deferredGross = BigDecimal.ZERO;
    BigDecimal withheld = BigDecimal.ZERO;
    long unknown = 0;
    Map<String, Row> rows = new LinkedHashMap<>();

    for (TaxDividend d : dividends == null ? List.<TaxDividend>of() : dividends) {
      if (d == null || d.payDate() == null || d.payDate().getYear() != year) {
        continue;
      }
      Category category;
      if (deferredAccountIds != null && deferredAccountIds.contains(d.accountId())) {
        category = Category.DEFERRED;
        deferredGross = deferredGross.add(nz(d.grossAmount()));
      } else {
        BigDecimal taxable = nz(d.taxableAmount());
        if (d.taxableAmount() == null) {
          unknown++;
        }
        withheld = withheld.add(nz(d.tax()));
        if (rule.highDividendRule()
            && HighDividendDesignation.isHighDividend(designations, d.stockItemId(), d.payDate())) {
          category = Category.HIGH_DIVIDEND;
          high = high.add(taxable);
        } else {
          category = Category.GENERAL;
          general = general.add(taxable);
          if (grossUpEligible(d.stockItemName())) {
            generalGrossUpEligible = generalGrossUpEligible.add(taxable);
          }
        }
      }
      String key = d.accountId() + "|" + d.stockItemId() + "|" + category;
      Row prev = rows.get(key);
      rows.put(
          key,
          new Row(
              d.accountId(),
              d.stockItemId(),
              d.stockItemName(),
              category,
              nz(prev == null ? null : prev.grossAmount()).add(nz(d.grossAmount())),
              category == Category.DEFERRED
                  ? null
                  : nz(prev == null ? null : prev.taxableAmount()).add(nz(d.taxableAmount())),
              nz(prev == null ? null : prev.tax()).add(nz(d.tax())),
              (prev == null ? 0 : prev.count()) + 1,
              category == Category.HIGH_DIVIDEND
                  || (category == Category.GENERAL && grossUpEligible(d.stockItemName()))));
    }

    BigDecimal otherFinancial = nz(otherFinancialIncome);
    Scenario without =
        scenario(
            rule,
            general.add(high).add(otherFinancial),
            generalGrossUpEligible.add(high),
            otherIncomeTaxBase,
            null);
    Scenario with =
        rule.highDividendRule() && high.signum() > 0
            ? scenario(
                rule, general.add(otherFinancial), generalGrossUpEligible, otherIncomeTaxBase, high)
            : null;

    return new DividendTaxReport(
        year,
        inProgress,
        rule.supported(),
        rule.highDividendRule(),
        general,
        high,
        deferredGross,
        withheld,
        otherIncomeTaxBase,
        otherFinancialIncome,
        unknown,
        without,
        with,
        recommend(without, with, high),
        new ArrayList<>(rows.values()));
  }

  static boolean grossUpEligible(String stockItemName) {
    return stockItemName != null && !NOT_GROSS_UP.matcher(stockItemName.trim()).matches();
  }

  /**
   * 한 시나리오.
   *
   * @param financial 판정 금액(종합과세 대상 금융소득)
   * @param grossUpEligible 그중 Gross-up 대상 배당. 2천만 원은 이자 · 비대상 배당부터 채우므로 초과분은 대상 배당부터 이룬다
   * @param separated 분리과세를 신청한 고배당 배당(신청 안 하면 null)
   */
  static Scenario scenario(
      TaxRuleTable rule,
      BigDecimal financial,
      BigDecimal grossUpEligible,
      BigDecimal otherIncomeTaxBase,
      BigDecimal separated) {
    boolean comprehensive = financial.compareTo(TaxRuleTable.THRESHOLD) > 0;
    BigDecimal excess =
        comprehensive ? financial.subtract(TaxRuleTable.THRESHOLD) : BigDecimal.ZERO;
    BigDecimal grossUpBase = comprehensive ? grossUpEligible.min(excess) : BigDecimal.ZERO;
    BigDecimal grossUp = TaxRuleTable.floor(grossUpBase.multiply(rule.grossUpRate()));

    BigDecimal separatedTax = null;
    BigDecimal separatedWithheld = null;
    BigDecimal separatedExtra = BigDecimal.ZERO;
    List<Slice> slices = List.of();
    if (separated != null && rule.supported()) {
      slices = rule.highDividendSlices(separated);
      separatedTax = rule.highDividendTax(separated);
      separatedWithheld = TaxRuleTable.floor(separated.multiply(TaxRuleTable.WITHHOLDING_RATE));
      separatedExtra = separatedTax.subtract(separatedWithheld);
    }

    BigDecimal financialExtra;
    BigDecimal thresholdTax = null;
    BasicTax combinedTax = null;
    BasicTax otherTax = null;
    BigDecimal financialWithheld = null;
    if (!rule.supported()) {
      financialExtra = null;
    } else if (!comprehensive) {
      financialExtra = BigDecimal.ZERO;
    } else if (otherIncomeTaxBase == null) {
      financialExtra = null;
    } else {
      thresholdTax =
          TaxRuleTable.floor(TaxRuleTable.THRESHOLD.multiply(TaxRuleTable.WITHHOLDING_RATE));
      combinedTax = rule.basicTaxDetail(otherIncomeTaxBase.add(excess).add(grossUp));
      otherTax = rule.basicTaxDetail(otherIncomeTaxBase);
      financialWithheld = TaxRuleTable.floor(financial.multiply(TaxRuleTable.WITHHOLDING_RATE));
      BigDecimal extra =
          thresholdTax
              .add(combinedTax.tax())
              .subtract(grossUp)
              .subtract(otherTax.tax())
              .subtract(financialWithheld);
      financialExtra = extra.max(BigDecimal.ZERO);
    }

    BigDecimal additional = financialExtra == null ? null : financialExtra.add(separatedExtra);
    BigDecimal localTax =
        additional == null
            ? null
            : TaxRuleTable.floor(additional.multiply(TaxRuleTable.LOCAL_TAX_RATE));
    BigDecimal additionalTotal = additional == null ? null : additional.add(localTax);
    return new Scenario(
        financial,
        comprehensive,
        excess,
        grossUp,
        separatedTax,
        additional,
        additionalTotal,
        comprehensive,
        new Detail(
            grossUpEligible,
            grossUpBase,
            rule.grossUpRate(),
            slices,
            separatedWithheld,
            separatedTax == null ? null : separatedExtra,
            thresholdTax,
            combinedTax,
            otherTax,
            financialWithheld,
            financialExtra,
            localTax));
  }

  static Recommendation recommend(Scenario without, Scenario with, BigDecimal high) {
    if (!without.comprehensive()) {
      return Recommendation.NONE_NEEDED;
    }
    if (with == null) {
      return Recommendation.WITHOUT;
    }
    if (without.additionalNationalTax() != null && with.additionalNationalTax() != null) {
      return with.additionalNationalTax().compareTo(without.additionalNationalTax()) < 0
          ? Recommendation.WITH
          : Recommendation.WITHOUT;
    }
    // 신청하면 기준 아래로 내려가고 고배당 몫도 14% 구간 안이면 원천징수로 끝난다 - 신청 안 하는 쪽보다 낼 게 많을 수 없다.
    if (!with.comprehensive() && high.compareTo(TaxRuleTable.THRESHOLD) <= 0) {
      return Recommendation.WITH;
    }
    return null;
  }

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
