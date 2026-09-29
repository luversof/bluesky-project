package net.luversof.api.stock.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlyDividendProfileUpsertRequest {

  private String symbol;

  private String sourceUrl;

  private String payoutWindow;

  private Integer displayOrder;

  private Boolean active;

  private String note;

  private LocalDate lastVerifiedDate;

  /** 총보수(연, %). 저장은 전체 덮어쓰기라 호출하는 쪽이 기존 값을 돌려보내야 한다(다른 필드와 같다). */
  private BigDecimal totalExpenseRatioPct;

  private LocalDate listingDate;

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

  public BigDecimal getTotalExpenseRatioPct() {
    return totalExpenseRatioPct;
  }

  public void setTotalExpenseRatioPct(BigDecimal totalExpenseRatioPct) {
    this.totalExpenseRatioPct = totalExpenseRatioPct;
  }

  public LocalDate getListingDate() {
    return listingDate;
  }

  public void setListingDate(LocalDate listingDate) {
    this.listingDate = listingDate;
  }
}
