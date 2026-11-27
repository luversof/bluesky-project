package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 귀속연도 배당의 종합소득세 신고 자료.
 *
 * <p>세액은 국세 기준이다. {@code additionalTotalTax} 만 지방소득세(국세의 10%)를 더했다. 계산할 수 없는 값(다른 종합소득 과세표준을 모르는데
 * 종합과세 대상이거나, 세율표가 없는 해)은 null 이다.
 *
 * @param inProgress 올해라 아직 진행 중인 추정
 * @param ruleSupported 이 해의 세율표가 있다(없으면 세액은 모두 null)
 * @param highDividendRule 이 해 지급분에 고배당 분리과세 특례가 있다
 * @param generalTaxable 과세 계좌 일반 배당의 과세금액 합
 * @param highDividendTaxable 과세 계좌 고배당 배당의 과세금액 합
 * @param deferredGross 과세이연 계좌(연금 · ISA) 배당 세전 합 - 신고에서 빠진다
 * @param withheldTax 과세 계좌 배당에서 떼인 세금 합(원장 값, 지방소득세 포함)
 * @param unknownTaxableCount 과세 계좌인데 과세금액이 빈 배당 수(0 으로 셌다)
 * @param withoutSeparation 고배당 분리과세를 신청하지 않을 때
 * @param withSeparation 신청할 때(고배당 배당이 없거나 특례가 없는 해면 null)
 * @param recommendation 유리한 쪽(판단할 수 없으면 null)
 */
public record DividendTaxReport(
    int year,
    boolean inProgress,
    boolean ruleSupported,
    boolean highDividendRule,
    BigDecimal generalTaxable,
    BigDecimal highDividendTaxable,
    BigDecimal deferredGross,
    BigDecimal withheldTax,
    BigDecimal otherIncomeTaxBase,
    BigDecimal otherFinancialIncome,
    long unknownTaxableCount,
    Scenario withoutSeparation,
    Scenario withSeparation,
    Recommendation recommendation,
    List<Row> rows) {

  public enum Category {
    GENERAL,
    HIGH_DIVIDEND,
    DEFERRED
  }

  public enum Recommendation {
    /** 종합과세 대상이 아니다 - 신고할 것도, 분리과세를 신청할 실익도 없다. */
    NONE_NEEDED,
    WITHOUT,
    WITH
  }

  /**
   * @param financialIncome 판정 금액
   * @param comprehensive 2천만 원을 넘어 종합과세 대상
   * @param excess 2천만 원 초과분
   * @param grossUp 초과분에 붙는 Gross-up
   * @param highDividendSeparateTax 분리과세한 고배당 배당의 산출세액(신청하지 않으면 null)
   * @param additionalNationalTax 원천징수 뒤 더 낼 국세
   * @param additionalTotalTax 위에 지방소득세를 더한 값
   * @param isaRestricted 종합과세 대상이라 이후 3 년 ISA 가입 · 연장 때 특례를 받을 수 없다
   */
  public record Scenario(
      BigDecimal financialIncome,
      boolean comprehensive,
      BigDecimal excess,
      BigDecimal grossUp,
      BigDecimal highDividendSeparateTax,
      BigDecimal additionalNationalTax,
      BigDecimal additionalTotalTax,
      boolean isaRestricted) {}

  /** 계좌 · 종목 · 구분별 합. 과세이연 줄은 과세금액이 null 이다. */
  public record Row(
      UUID accountId,
      UUID stockItemId,
      String stockItemName,
      Category category,
      BigDecimal grossAmount,
      BigDecimal taxableAmount,
      BigDecimal tax,
      long count) {}
}
