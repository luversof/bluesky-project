package net.luversof.web.gate.stock.dto.request;

public class MonthlyDividendPayoutImportRequest {

  private String symbol;

  private String bulkInput;

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getBulkInput() {
    return bulkInput;
  }

  public void setBulkInput(String bulkInput) {
    this.bulkInput = bulkInput;
  }
}
