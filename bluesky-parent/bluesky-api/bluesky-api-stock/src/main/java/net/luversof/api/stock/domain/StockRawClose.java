package net.luversof.api.stock.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 하루치 원주가(수정 전 종가)와 수정 종가 - 손익 시뮬레이션의 원주가 평가 전용 투영(2026-10-02).
 *
 * <p>원주가가 아직 안 채워진 날은 rawClose 가 null 이다. 수정 종가는 분할 · 병합이 있던 날의 배율을 정확히 내는 데만 쓴다.
 */
public record StockRawClose(
    UUID stockItemId, LocalDate tradeDate, BigDecimal rawClose, BigDecimal adjustedClose) {}
