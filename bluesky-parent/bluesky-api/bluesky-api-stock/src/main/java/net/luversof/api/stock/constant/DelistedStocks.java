package net.luversof.api.stock.constant;

public enum DelistedStocks {
  쌍방울("KRX", "102280");

  private String market;
  private String symbol;

  DelistedStocks(String market, String symbol) {
    this.market = market;
    this.symbol = symbol;
  }

  public String getMarket() {
    return market;
  }

  public String getSymbol() {
    return symbol;
  }
}
