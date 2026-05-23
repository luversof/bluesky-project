package net.luversof.api.stock.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
