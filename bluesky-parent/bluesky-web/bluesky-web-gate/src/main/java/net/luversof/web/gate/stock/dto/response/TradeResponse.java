package net.luversof.web.gate.stock.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import net.luversof.web.gate.stock.constant.TradeType;

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
