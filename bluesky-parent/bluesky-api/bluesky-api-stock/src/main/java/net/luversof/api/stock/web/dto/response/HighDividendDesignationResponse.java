package net.luversof.api.stock.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/** 고배당기업 지정 한 줄. */
public record HighDividendDesignationResponse(
    UUID id,
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    int businessYear,
    LocalDate payDateFrom,
    LocalDate payDateTo,
    String note) {}
