package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

public record MonthlyDividendSimulatorSummaryView(
    int itemCount,
    BigDecimal totalLatestMonthlyDividend,
    BigDecimal totalExpectedMonthlyDividend,
    BigDecimal totalExpectedAnnualDividend,
    BigDecimal totalExpectedTaxableBaseAmount,
    BigDecimal totalBuyAmount,
    BigDecimal totalCurrentMarketValue,
    BigDecimal portfolioExpectedAnnualYieldPct,
    MonthlyDividendSnapshotResponse bestChoice) {}
