package net.luversof.web.gate.stock.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyDividendReferenceSummaryView(
    String stockItemSymbol,
    int payoutCount,
    BigDecimal latestDividendAmountPerShare,
    BigDecimal averageDividendAmountPerShare1y,
    BigDecimal averageTaxableBaseRatio1y,
    LocalDate latestRecordDate,
    LocalDate latestPayDate) {}
