package net.luversof.web.gate.stock.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 고배당기업 지정 한 줄(api-stock 응답 그대로). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HighDividendDesignationResponse(
    UUID id,
    UUID stockItemId,
    String stockItemSymbol,
    String stockItemName,
    int businessYear,
    LocalDate payDateFrom,
    LocalDate payDateTo,
    String note) {}
