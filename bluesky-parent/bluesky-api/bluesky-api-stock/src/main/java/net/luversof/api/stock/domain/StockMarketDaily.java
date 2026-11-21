package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 시장 일별 시세. K200F_DAY = KOSPI200 선물 최근월물 주간 종가, K200F_NIGHT = 같은 종목의 그날 저녁 시작한 야간 거래 종가(다음 날 05:00
 * 마감). NXT_PRE_종목코드 = 그 종목의 NXT 장전 가격(premarketCode).
 */
@Table("StockMarketDaily")
public class StockMarketDaily {

  public static final String K200F_DAY = "K200F_DAY";

  public static final String K200F_NIGHT = "K200F_NIGHT";

  /** 종목의 NXT(넥스트레이드) 장전 가격 코드: 그날 08:50 마감 직전 체결가(그림자 집계, 2026-11-21). contract 에는 종목 코드를 둔다. */
  public static String premarketCode(String symbol) {
    return "NXT_PRE_" + symbol;
  }

  @Id
  @Column("id")
  private UUID id;

  @Column("code")
  private String code;

  @Column("tradeDate")
  private LocalDate tradeDate;

  @Column("closePrice")
  private BigDecimal closePrice;

  @Column("contract")
  private String contract;

  @LastModifiedDate
  @Column("updatedDate")
  private Instant updatedDate;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public LocalDate getTradeDate() {
    return tradeDate;
  }

  public void setTradeDate(LocalDate tradeDate) {
    this.tradeDate = tradeDate;
  }

  public BigDecimal getClosePrice() {
    return closePrice;
  }

  public void setClosePrice(BigDecimal closePrice) {
    this.closePrice = closePrice;
  }

  public String getContract() {
    return contract;
  }

  public void setContract(String contract) {
    this.contract = contract;
  }

  public Instant getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Instant updatedDate) {
    this.updatedDate = updatedDate;
  }
}
