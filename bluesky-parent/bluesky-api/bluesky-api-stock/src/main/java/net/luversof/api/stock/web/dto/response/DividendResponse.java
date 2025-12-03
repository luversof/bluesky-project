package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DividendResponse(
    UUID id,
    UUID accountId,
    UUID stockItemId,
    String stockItemName,
    String type,
    Integer quantity,
    BigDecimal price,
    BigDecimal fee,
    BigDecimal tax,
    Instant recordDate,
    Instant payDate
) {
}
