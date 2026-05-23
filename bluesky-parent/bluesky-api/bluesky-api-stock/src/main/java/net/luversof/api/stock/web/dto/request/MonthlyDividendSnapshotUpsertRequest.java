package net.luversof.api.stock.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class MonthlyDividendSnapshotUpsertRequest {

  private UUID userId;

  private String symbol;

  private LocalDate asOfDate;

  private BigDecimal latestMonthlyDividendPerShare;

  private BigDecimal averageMonthlyDividendPerShare1y;

  private BigDecimal averageTaxableBaseRatio1y;

  private Integer heldQuantity;

  private BigDecimal averageBuyPrice;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public void setAsOfDate(LocalDate asOfDate) {
    this.asOfDate = asOfDate;
  }

  public BigDecimal getLatestMonthlyDividendPerShare() {
    return latestMonthlyDividendPerShare;
  }

  public void setLatestMonthlyDividendPerShare(BigDecimal latestMonthlyDividendPerShare) {
    this.latestMonthlyDividendPerShare = latestMonthlyDividendPerShare;
  }

  public BigDecimal getAverageMonthlyDividendPerShare1y() {
    return averageMonthlyDividendPerShare1y;
  }

  public void setAverageMonthlyDividendPerShare1y(BigDecimal averageMonthlyDividendPerShare1y) {
    this.averageMonthlyDividendPerShare1y = averageMonthlyDividendPerShare1y;
  }

  public BigDecimal getAverageTaxableBaseRatio1y() {
    return averageTaxableBaseRatio1y;
  }

  public void setAverageTaxableBaseRatio1y(BigDecimal averageTaxableBaseRatio1y) {
    this.averageTaxableBaseRatio1y = averageTaxableBaseRatio1y;
  }

  public Integer getHeldQuantity() {
    return heldQuantity;
  }

  public void setHeldQuantity(Integer heldQuantity) {
    this.heldQuantity = heldQuantity;
  }

  public BigDecimal getAverageBuyPrice() {
    return averageBuyPrice;
  }

  public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
    this.averageBuyPrice = averageBuyPrice;
  }
}
