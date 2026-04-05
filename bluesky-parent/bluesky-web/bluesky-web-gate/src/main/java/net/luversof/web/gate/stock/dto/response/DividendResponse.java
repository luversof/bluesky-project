package net.luversof.web.gate.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendResponse(
        UUID id,
        UUID accountId,
        UUID stockItemId,
        String stockItemName,
        String type,
        Integer quantity,
        BigDecimal amountPerShare,
        BigDecimal taxPerShare,
        BigDecimal grossAmount,
        BigDecimal fee,
        BigDecimal tax,
        BigDecimal taxableAmount,
        BigDecimal netAmount,
        Instant recordDate,
        Instant payDate) {}
