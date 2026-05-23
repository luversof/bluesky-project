package net.luversof.api.stock.web.dto.request;

import java.util.UUID;

public class MonthlyDividendProfileRequest {

  private UUID stockItemId;

  private String symbol;

  private Boolean activeOnly;

  public UUID getStockItemId() {
    return stockItemId;
  }

  public void setStockItemId(UUID stockItemId) {
    this.stockItemId = stockItemId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Boolean getActiveOnly() {
    return activeOnly;
  }

  public void setActiveOnly(Boolean activeOnly) {
    this.activeOnly = activeOnly;
  }
}
