package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 월배당 프로필. 총보수(연, %) · 상장일은 2026-09-28 에 끝에 붙였다(위치로 만드는 곳이 많아 기존 순서를 흔들지 않는다). */
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
    BigDecimal totalExpenseRatioPct,
    LocalDate listingDate) {}
