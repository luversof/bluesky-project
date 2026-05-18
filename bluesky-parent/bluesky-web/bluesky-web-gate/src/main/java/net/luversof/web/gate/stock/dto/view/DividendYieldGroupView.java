package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.Instant;

public record DividendYieldGroupView(
    String label,
    BigDecimal totalGrossAmount,
    BigDecimal totalNetAmount,
    BigDecimal totalTaxableAmount,
    BigDecimal averageDailyPrincipalCost,
    BigDecimal averagePrincipalCost,
    BigDecimal averagePrincipalMarketValue,
    BigDecimal yieldOnDailyAverageCostPct,
    BigDecimal yieldOnCostPct,
    BigDecimal yieldOnMarketPct,
    long dividendCount,
    Instant lastDividendDate) {}
