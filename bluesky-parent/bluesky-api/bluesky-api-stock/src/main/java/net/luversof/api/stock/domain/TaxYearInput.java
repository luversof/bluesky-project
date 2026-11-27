package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 사용자가 귀속연도마다 넣는 세금 계산 입력값. 앱이 모르는 값이라 사람이 넣는다.
 *
 * <ul>
 *   <li>{@code otherIncomeTaxBase}: 배당 말고 다른 종합소득의 과세표준. 종합과세 때 추가 납부액이 이 값에 따라 달라진다.
 *   <li>{@code otherFinancialIncome}: 앱 밖 금융소득(예금 이자 등). 금융소득 2천만 원 판정에 들어간다.
 * </ul>
 *
 * <p>둘 다 비울 수 있다 - null 은 "모름" 이고 0 과 다르다.
 */
@Table("TaxYearInput")
public class TaxYearInput {

  @Id
  @Column("id")
  private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("year")
  private int year;

  @Column("otherIncomeTaxBase")
  private BigDecimal otherIncomeTaxBase;

  @Column("otherFinancialIncome")
  private BigDecimal otherFinancialIncome;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
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
