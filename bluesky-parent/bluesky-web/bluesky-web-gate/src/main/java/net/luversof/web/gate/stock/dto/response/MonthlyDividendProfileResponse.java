package net.luversof.web.gate.stock.dto.response;

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
    Instant updatedDate) {}
