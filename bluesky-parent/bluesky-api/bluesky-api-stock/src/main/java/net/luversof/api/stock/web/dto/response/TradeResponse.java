package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import net.luversof.api.stock.constant.TradeType;

public record TradeResponse(
        UUID id,
        UUID accountId,
        UUID stockItemId,
        String stockItemName, // Name might need to be fetched separate or joined.
        TradeType type,
        int quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal tax,
        BigDecimal amount, // price * quantity
        BigDecimal realizedProfit, // For SELL trades
        Instant tradeDate) {}
