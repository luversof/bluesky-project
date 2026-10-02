package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하루치 시가 · 고가 · 저가 · 종가(모두 수정 주가)와 원주가 - 종목 상세 캔들 차트 전용 투영(2026-10-02). 원주가가 없거나 열이 없는 DB 면
 * rawClose 는 null 이다.
 */
public record StockOhlcRow(
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal rawClose) {}
