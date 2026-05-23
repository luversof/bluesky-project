package net.luversof.web.gate.stock.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlyDividendPayoutUpsertRequest {

  private String symbol;

  private LocalDate recordDate;

  private LocalDate payDate;

  private BigDecimal distributionRatePct;

  private BigDecimal dividendAmountPerShare;

  private BigDecimal taxableBasePerShare;

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public LocalDate getRecordDate() {
    return recordDate;
  }

  public void setRecordDate(LocalDate recordDate) {
    this.recordDate = recordDate;
  }

  public LocalDate getPayDate() {
    return payDate;
  }

  public void setPayDate(LocalDate payDate) {
    this.payDate = payDate;
  }

  public BigDecimal getDistributionRatePct() {
    return distributionRatePct;
  }

  public void setDistributionRatePct(BigDecimal distributionRatePct) {
    this.distributionRatePct = distributionRatePct;
  }

  public BigDecimal getDividendAmountPerShare() {
    return dividendAmountPerShare;
  }

  public void setDividendAmountPerShare(BigDecimal dividendAmountPerShare) {
    this.dividendAmountPerShare = dividendAmountPerShare;
  }

  public BigDecimal getTaxableBasePerShare() {
    return taxableBasePerShare;
  }

  public void setTaxableBasePerShare(BigDecimal taxableBasePerShare) {
    this.taxableBasePerShare = taxableBasePerShare;
  }
}
