package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 종목 상세 주가 차트 한 점(2026-10-02): 시가 · 고가 · 저가 · 종가(원주가를 분할로만 맞춘 값)와 그 날의 평균 단가(보유가 없던 날 null). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockPriceChartPoint(
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal averageCost) {}
