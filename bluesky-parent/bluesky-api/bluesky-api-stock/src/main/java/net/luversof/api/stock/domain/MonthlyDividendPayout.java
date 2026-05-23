package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("MonthlyDividendPayout")
public class MonthlyDividendPayout {

  @Id
  @Column("id")
  private UUID id;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("recordDate")
  private LocalDate recordDate;

  @Column("payDate")
  private LocalDate payDate;

  @Column("distributionRatePct")
  private BigDecimal distributionRatePct;

  @Column("dividendAmountPerShare")
  private BigDecimal dividendAmountPerShare;

  @Column("taxableBasePerShare")
  private BigDecimal taxableBasePerShare;

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

  public UUID getStockItemId() {
    return stockItemId;
  }

  public void setStockItemId(UUID stockItemId) {
    this.stockItemId = stockItemId;
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
