package net.luversof.web.gate.stock.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 귀속연도 세금 입력값 저장(api-stock {@code /api/taxYearInput}). 비운 값은 null(모름).
 *
 * <p>폼에서 바인딩하고 컨트롤러가 {@code userId} 를 세션 사용자로 덮어쓴다(RequestUserScopingTest).
 */
public class TaxYearInputUpsertRequest {

  private UUID userId;

  private Integer year;

  private BigDecimal otherIncomeTaxBase;

  private BigDecimal otherFinancialIncome;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public BigDecimal getOtherIncomeTaxBase() {
    return otherIncomeTaxBase;
  }

  public void setOtherIncomeTaxBase(BigDecimal otherIncomeTaxBase) {
    this.otherIncomeTaxBase = otherIncomeTaxBase;
  }

  public BigDecimal getOtherFinancialIncome() {
    return otherFinancialIncome;
  }

  public void setOtherFinancialIncome(BigDecimal otherFinancialIncome) {
    this.otherFinancialIncome = otherFinancialIncome;
  }
}
