package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 종목 상세 주가 차트 한 점(2026-10-02): 시가 · 고가 · 저가 · 종가(원주가를 분할로만 맞춘 값)와 그 날의 평균 단가(보유가 없던 날 null), 그 날이
 * 분배락일이면 주당 분배금(아니면 null - 월배당 지급 이력이 있는 종목만).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockPriceChartPoint(
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal averageCost,
    BigDecimal distribution) {

  /** 분배금 없이(옛 API 응답 · 시험). */
  public StockPriceChartPoint(
      LocalDate tradeDate,
      BigDecimal open,
      BigDecimal high,
      BigDecimal low,
      BigDecimal close,
      BigDecimal averageCost) {
    this(tradeDate, open, high, low, close, averageCost, null);
  }
}
