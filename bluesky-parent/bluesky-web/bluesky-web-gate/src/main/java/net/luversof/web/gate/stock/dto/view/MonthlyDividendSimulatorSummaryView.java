package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

public record MonthlyDividendSimulatorSummaryView(
    int itemCount,
    BigDecimal totalLatestMonthlyDividend,
    BigDecimal totalLatestMonthlyDividendMidMonth,
    BigDecimal totalLatestMonthlyDividendMonthEnd,
    BigDecimal totalExpectedMonthlyDividend,
    BigDecimal totalExpectedAnnualDividend,
    BigDecimal totalExpectedTaxableBaseAmount,
    BigDecimal totalExpectedAnnualTaxableBaseAmount,
    BigDecimal totalBuyAmount,
    BigDecimal totalCurrentMarketValue,
    BigDecimal portfolioExpectedAnnualYieldPct,
    MonthlyDividendSnapshotResponse bestChoice) {}
