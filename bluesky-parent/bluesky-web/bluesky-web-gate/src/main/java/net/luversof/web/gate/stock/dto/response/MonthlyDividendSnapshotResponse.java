package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyDividendSnapshotResponse(
    UUID id,
    UUID userId,
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    LocalDate asOfDate,
    BigDecimal latestMonthlyDividendPerShare,
    BigDecimal averageMonthlyDividendPerShare1y,
    BigDecimal averageTaxableBaseRatio1y,
    Integer heldQuantity,
    BigDecimal averageBuyPrice,
    BigDecimal currentPrice,
    BigDecimal currentMarketValue,
    BigDecimal expectedMonthlyDividend,
    BigDecimal expectedMonthlyYieldPct,
    BigDecimal expectedAnnualYieldPct,
    BigDecimal expectedMonthlyYieldOnCostPct,
    BigDecimal expectedAnnualYieldOnCostPct,
    BigDecimal expectedTaxableBaseAmount,
    BigDecimal totalReturnOnCostPct,
    BigDecimal expectedCombinedReturnPct,
    Instant updatedDate) {}
