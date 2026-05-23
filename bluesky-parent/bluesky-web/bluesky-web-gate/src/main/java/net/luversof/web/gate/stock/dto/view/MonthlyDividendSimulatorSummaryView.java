package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse;

public record MonthlyDividendSimulatorSummaryView(
    int itemCount,
    BigDecimal totalExpectedMonthlyDividend,
    BigDecimal totalExpectedAnnualDividend,
    BigDecimal portfolioExpectedAnnualYieldPct,
    MonthlyDividendSnapshotResponse bestChoice) {}
