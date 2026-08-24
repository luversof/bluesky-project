package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeProfitTimeSeriesPoint(
    Instant timestamp,
    BigDecimal cumulativeRealizedProfit,
    BigDecimal dailyRealizedProfit,
    long tradeCount,
    long buyCount,
    long tradeVolume,
    BigDecimal totalHoldingsValue,
    BigDecimal totalHoldingsCost,
    BigDecimal cumulativeTotalProfit,
    BigDecimal cumulativeDividend) {}
