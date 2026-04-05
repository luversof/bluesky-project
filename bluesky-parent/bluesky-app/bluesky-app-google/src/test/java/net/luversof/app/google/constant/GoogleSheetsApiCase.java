package net.luversof.app.google.constant;

public enum GoogleSheetsApiCase {
  GoogleSheetStockItem(0, "주식 검색!A1:F", GoogleSpreadSheetInfoType.STOCK_STOCKITEM),
  GoogleSheetDividend(0, "주식 배당 기록!A1:J", GoogleSpreadSheetInfoType.STOCK_DIVIDEND),
  GoogleSheetTrade(1, "주식 매매 기록!A1:V", GoogleSpreadSheetInfoType.STOCK_TRADE);

  private int enabled;
  private String range;
  private GoogleSpreadSheetInfoType type;

  private GoogleSheetsApiCase(int enabled, String range, GoogleSpreadSheetInfoType type) {
    this.enabled = enabled;
    this.range = range;
    this.type = type;
  }

  public boolean isEnabled() {
    return enabled == 1;
  }

  public String getRange() {
    return range;
  }

  public GoogleSpreadSheetInfoType getType() {
    return type;
  }
}
