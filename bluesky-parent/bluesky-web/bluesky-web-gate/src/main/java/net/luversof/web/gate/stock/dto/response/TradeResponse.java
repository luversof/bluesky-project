package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.luversof.web.gate.stock.constant.TradeType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeResponse(
    UUID id,
    UUID accountId,
    UUID stockItemId,
    String stockItemName,
    TradeType type,
    int quantity,
    BigDecimal price,
    BigDecimal fee,
    BigDecimal tax,
    BigDecimal amount,
    BigDecimal realizedProfit,
    Instant tradeDate) {}
