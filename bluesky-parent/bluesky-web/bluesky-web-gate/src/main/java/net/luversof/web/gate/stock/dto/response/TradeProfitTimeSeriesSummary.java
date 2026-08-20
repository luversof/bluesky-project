package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 시계열 기간 요약(api-stock 이 다운샘플 이전 일별 시리즈로 계산해 내려준다). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitTimeSeriesSummary(
    BigDecimal openingValue,
    BigDecimal closingValue,
    Double growthRatePct,
    Double timeWeightedReturnPct,
    BigDecimal periodProfit,
    BigDecimal principalDelta,
    BigDecimal unrealizedStart,
    BigDecimal unrealizedEnd,
    Double unrealizedEndPct,
    BigDecimal recoveredAmount,
    BigDecimal netNewProfit,
    Double maxDrawdownPct,
    LocalDate maxDrawdownPeakDate,
    LocalDate maxDrawdownTroughDate,
    Double currentDrawdownPct) {}
