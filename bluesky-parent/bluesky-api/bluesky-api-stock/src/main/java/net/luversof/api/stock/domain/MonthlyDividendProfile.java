package net.luversof.api.stock.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("MonthlyDividendProfile")
public class MonthlyDividendProfile {

  @Id
  @Column("id")
  private UUID id;

  @Column("stockItem_id")
  private UUID stockItemId;

  @Column("sourceUrl")
  private String sourceUrl;

  @Column("payoutWindow")
  private String payoutWindow;

  @Column("displayOrder")
  private Integer displayOrder;

  @Column("active")
  private Boolean active;

  @Column("note")
  private String note;

  @Column("lastVerifiedDate")
  private LocalDate lastVerifiedDate;

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

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public String getPayoutWindow() {
    return payoutWindow;
  }

  public void setPayoutWindow(String payoutWindow) {
    this.payoutWindow = payoutWindow;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public LocalDate getLastVerifiedDate() {
    return lastVerifiedDate;
  }

  public void setLastVerifiedDate(LocalDate lastVerifiedDate) {
    this.lastVerifiedDate = lastVerifiedDate;
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
