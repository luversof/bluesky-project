package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyDividendPayoutResponse(
    UUID id,
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    LocalDate recordDate,
    LocalDate payDate,
    BigDecimal distributionRatePct,
    BigDecimal dividendAmountPerShare,
    BigDecimal taxableBasePerShare,
    Instant updatedDate) {}
