package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyDividendProfileResponse(
    UUID id,
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    String sourceUrl,
    String payoutWindow,
    Integer displayOrder,
    boolean active,
    String note,
    LocalDate lastVerifiedDate,
    Instant updatedDate,
    /** 총보수(연, %) · 상장일(2026-09-28). 모르면 null. */
    BigDecimal totalExpenseRatioPct,
    LocalDate listingDate) {}
