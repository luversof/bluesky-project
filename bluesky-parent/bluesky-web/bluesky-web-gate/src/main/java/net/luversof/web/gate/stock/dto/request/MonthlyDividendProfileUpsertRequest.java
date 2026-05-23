package net.luversof.web.gate.stock.dto.request;

import java.time.LocalDate;

public class MonthlyDividendProfileUpsertRequest {

  private String symbol;

  private String sourceUrl;

  private String payoutWindow;

  private Integer displayOrder;

  private Boolean active;

  private String note;

  private LocalDate lastVerifiedDate;

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
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
}
