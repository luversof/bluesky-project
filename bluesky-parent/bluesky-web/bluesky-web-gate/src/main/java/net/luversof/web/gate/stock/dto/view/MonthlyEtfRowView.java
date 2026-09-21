package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 월배당 ETF 목록의 한 줄 &mdash; api-stock 의 종목 단위 정보에 <b>내 보유 여부</b>만 덧붙인 것.
 *
 * <p>금액 · 수량 · 예상 배당금은 담지 않는다(사용자 요청 2026-09-21: 내가 받을 배당은 시뮬레이터, 종목 정보는 이 목록).
 */
public record MonthlyEtfRowView(
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
    /** 시세 이력의 첫 날(기간 수익률이 비면 이 날부터 있다고 적는다). */
    LocalDate priceHistoryStartDate,
    /** 지금 고른 기간의 가격 수익률. 이력이 모자라면 null. */
    BigDecimal periodPriceReturnPct,
    /** 지금 고른 기간의 합산(가격 + 분배금) 수익률. 이력이 모자라면 null. */
    BigDecimal periodTotalReturnPct,
    /** 그 기간 수익률의 기초 날짜(무엇과 견줬는지). */
    LocalDate periodBaseDate,
    /** 내 원장에 지금 보유 수량이 있는 종목인가(수량 · 금액은 적지 않는다). */
    boolean held) {}
