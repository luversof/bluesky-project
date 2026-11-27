package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 귀속연도 배당 종소세 신고 자료(api-stock {@code /api/dividendTaxReport} 응답 그대로). 구분 · 추천은 문자열로 받는다 - api 가 값을
 * 더해도 화면이 깨지지 않게.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    String recommendation,
    List<Row> rows) {

  public static final String NONE_NEEDED = "NONE_NEEDED";
  public static final String WITH = "WITH";
  public static final String WITHOUT = "WITHOUT";

  /** 화면 기준으로 쓸 시나리오: 신청이 유리하면 신청, 아니면 신청 안 함. */
  public Scenario chosen() {
    return WITH.equals(recommendation) && withSeparation != null
        ? withSeparation
        : withoutSeparation;
  }

  /** 내역 줄의 세전 합. category 가 null 이면 전체(총 배당). */
  public BigDecimal grossOf(String category) {
    return sum(category, Row::grossAmount);
  }

  /** 내역 줄의 과세금액 합. category 가 null 이면 전체. */
  public BigDecimal taxableOf(String category) {
    return sum(category, Row::taxableAmount);
  }

  private BigDecimal sum(String category, java.util.function.Function<Row, BigDecimal> value) {
    if (rows == null) {
      return BigDecimal.ZERO;
    }
    return rows.stream()
        .filter(r -> category == null || category.equals(r.category()))
        .map(value)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Scenario(
      BigDecimal financialIncome,
      boolean comprehensive,
      BigDecimal excess,
      BigDecimal grossUp,
      BigDecimal highDividendSeparateTax,
      BigDecimal additionalNationalTax,
      BigDecimal additionalTotalTax,
      boolean isaRestricted) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Row(
      UUID accountId,
      UUID stockItemId,
      String stockItemName,
      String category,
      BigDecimal grossAmount,
      BigDecimal taxableAmount,
      BigDecimal tax,
      long count) {}
}
