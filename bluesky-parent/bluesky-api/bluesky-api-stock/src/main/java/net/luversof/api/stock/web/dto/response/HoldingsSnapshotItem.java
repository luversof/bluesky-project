package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingsSnapshotItem(
    UUID stockItemId,
    String name,
    String symbol,
    BigDecimal quantity,
    BigDecimal avgCost,
    BigDecimal priceAtDate,
    BigDecimal value,
    BigDecimal unrealizedProfit) {}
