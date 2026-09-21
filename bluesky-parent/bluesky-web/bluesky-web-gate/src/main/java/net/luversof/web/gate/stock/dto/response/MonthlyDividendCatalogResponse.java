package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 월배당 ETF 한 종목의 종목 단위 정보(api-stock DTO 와 이름 · 필드가 같다).
 *
 * <p>보유 수량 · 평단 같은 사용자 값은 들어 있지 않다 &mdash; 보유 여부 표시는 게이트가 원장에서 따로 붙인다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyDividendCatalogResponse(
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    String payoutWindow,
    String sourceUrl,
    LocalDate lastVerifiedDate,
    boolean active,
    Integer displayOrder,
    int payoutCount,
    LocalDate latestRecordDate,
    LocalDate latestPayDate,
    BigDecimal latestDividendPerShare,
    BigDecimal averageDividendPerShare1y,
    BigDecimal averageTaxableBaseRatio1y,
    BigDecimal averageTaxableBasePerShare1y,
    BigDecimal currentPrice,
    LocalDate currentPriceDate,
    BigDecimal monthlyYieldPct,
    BigDecimal annualYieldPct,
    /** 시세 이력의 첫 날. 기간 수익률이 비어 있을 때 "언제부터 있는지" 를 적기 위해 쓴다. */
    LocalDate priceHistoryStartDate,
    /** 1 · 3 · 6 · 12 개월 수익률. 이력이 그 기간을 못 덮으면 그 기간은 빠진다. */
    List<PeriodReturnView> periodReturns) {

  /** 한 기간의 가격 · 합산 수익률. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PeriodReturnView(
      int months, LocalDate baseDate, BigDecimal priceReturnPct, BigDecimal totalReturnPct) {}
}
