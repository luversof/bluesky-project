package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("MonthlyDividendSnapshot")
public class MonthlyDividendSnapshot {

  @Id
  @Column("id")
  private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("asOfDate")
  private LocalDate asOfDate;

  @Column("latestMonthlyDividendPerShare")
  private BigDecimal latestMonthlyDividendPerShare;

  @Column("averageMonthlyDividendPerShare1y")
  private BigDecimal averageMonthlyDividendPerShare1y;

  @Column("averageTaxableBaseRatio1y")
  private BigDecimal averageTaxableBaseRatio1y;

  @Column("heldQuantity")
  private Integer heldQuantity;

  @Column("averageBuyPrice")
  private BigDecimal averageBuyPrice;

  @Column("createdDate")
  private Instant createdDate;

  @Column("updatedDate")
  private Instant updatedDate;

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

  public UUID getStockItemId() {
    return stockItemId;
  }

  public void setStockItemId(UUID stockItemId) {
    this.stockItemId = stockItemId;
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

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }
}
