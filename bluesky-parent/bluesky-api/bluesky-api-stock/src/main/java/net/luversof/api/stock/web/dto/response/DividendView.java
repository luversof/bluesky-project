package net.luversof.api.stock.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendView(
        UUID id,
        UUID accountId,
        String accountName,
        UUID stockItemId,
        String stockItemName,
        BigDecimal grossAmount,
        BigDecimal tax,
        BigDecimal netAmount,
        Instant recordDate,
        Instant payDate) {}
