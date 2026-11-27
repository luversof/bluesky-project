package net.luversof.web.gate.stock.dto.request;

import java.time.LocalDate;
import java.util.UUID;

/** 고배당기업 지정 저장. {@code id} 가 있으면 그 행을 고치고, 없으면 새로 만든다. */
public record HighDividendDesignationUpsertRequest(
    UUID id,
    String symbol,
    Integer businessYear,
    LocalDate payDateFrom,
    LocalDate payDateTo,
    String note) {}
