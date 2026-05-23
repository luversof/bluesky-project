package net.luversof.web.gate.stock.dto.request;

import java.util.List;

public class MonthlyDividendProfileReorderRequest {

  private List<String> symbols;

  public List<String> getSymbols() {
    return symbols;
  }

  public void setSymbols(List<String> symbols) {
    this.symbols = symbols;
  }
}
