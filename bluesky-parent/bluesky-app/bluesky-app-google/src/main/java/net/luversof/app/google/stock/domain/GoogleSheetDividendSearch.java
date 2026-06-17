package net.luversof.app.google.stock.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.luversof.app.google.stock.databind.StockCurrencyDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** "배당주 검색" 시트의 종목코드/보유 주식수/매수 평단가 행 매핑. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetDividendSearch {

  @JsonProperty("종목코드")
  private String 종목코드;

  @JsonProperty("보유 주식수")
  private Integer 보유_주식수;

  @JsonProperty("매수 평단가")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 매수_평단가;

  public String get종목코드() {
    return 종목코드;
  }

  public void set종목코드(String 종목코드) {
    this.종목코드 = 종목코드;
  }

  public Integer get보유_주식수() {
    return 보유_주식수;
  }

  public void set보유_주식수(Integer 보유_주식수) {
    this.보유_주식수 = 보유_주식수;
  }

  public BigDecimal get매수_평단가() {
    return 매수_평단가;
  }

  public void set매수_평단가(BigDecimal 매수_평단가) {
    this.매수_평단가 = 매수_평단가;
  }

  @Override
  public String toString() {
    return "GoogleSheetDividendSearch [종목코드="
        + 종목코드
        + ", 보유_주식수="
        + 보유_주식수
        + ", 매수_평단가="
        + 매수_평단가
        + "]";
  }
}
