package net.luversof.app.google.stock.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.luversof.app.google.stock.databind.StockCurrencyDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleSheetDividend {

  @JsonProperty("지급일")
  private String 지급일;

  @JsonProperty("종목")
  private String 종목;

  @JsonProperty("계좌")
  private String 계좌;

  @JsonProperty("주식 수")
  private Integer 주식수;

  @JsonProperty("분배금액")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 분배금액;

  @JsonProperty("주당과세표준액")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 주당과세표준액;

  @JsonProperty("배당금")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 배당금;

  @JsonProperty("세금")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 세금;

  @JsonProperty("실지급")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 실지급;

  @JsonProperty("과세금액")
  @JsonDeserialize(using = StockCurrencyDeserializer.class)
  private BigDecimal 과세금액;

  public String get지급일() {
    return 지급일;
  }

  public void set지급일(String 지급일) {
    this.지급일 = 지급일;
  }

  public String get종목() {
    return 종목;
  }

  public void set종목(String 종목) {
    this.종목 = 종목;
  }

  public String get계좌() {
    return 계좌;
  }

  public void set계좌(String 계좌) {
    this.계좌 = 계좌;
  }

  public Integer get주식수() {
    return 주식수;
  }

  public void set주식수(Integer 주식수) {
    this.주식수 = 주식수;
  }

  public BigDecimal get분배금액() {
    return 분배금액;
  }

  public void set분배금액(BigDecimal 분배금액) {
    this.분배금액 = 분배금액;
  }

  public BigDecimal get주당과세표준액() {
    return 주당과세표준액;
  }

  public void set주당과세표준액(BigDecimal 주당과세표준액) {
    this.주당과세표준액 = 주당과세표준액;
  }

  public BigDecimal get배당금() {
    return 배당금;
  }

  public void set배당금(BigDecimal 배당금) {
    this.배당금 = 배당금;
  }

  public BigDecimal get세금() {
    return 세금;
  }

  public void set세금(BigDecimal 세금) {
    this.세금 = 세금;
  }

  public BigDecimal get실지급() {
    return 실지급;
  }

  public void set실지급(BigDecimal 실지급) {
    this.실지급 = 실지급;
  }

  public BigDecimal get과세금액() {
    return 과세금액;
  }

  public void set과세금액(BigDecimal 과세금액) {
    this.과세금액 = 과세금액;
  }

  @Override
  public String toString() {
    return "GoogleSheetsDividendItem [지급일="
        + 지급일
        + ", 종목="
        + 종목
        + ", 계좌="
        + 계좌
        + ", 주식수="
        + 주식수
        + ", 분배금액="
        + 분배금액
        + ", 주당과세표준액="
        + 주당과세표준액
        + ", 배당금="
        + 배당금
        + ", 세금="
        + 세금
        + ", 실지급="
        + 실지급
        + ", 과세금액="
        + 과세금액
        + "]";
  }
}
