package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeProfitTimeSeriesPoint(
    Instant timestamp,
    BigDecimal cumulativeRealizedProfit,
    BigDecimal dailyRealizedProfit,
    long tradeCount,
    long tradeVolume,
    BigDecimal totalHoldingsValue,
    BigDecimal totalHoldingsCost,
    BigDecimal cumulativeTotalProfit,
    BigDecimal cumulativeDividend) {}
