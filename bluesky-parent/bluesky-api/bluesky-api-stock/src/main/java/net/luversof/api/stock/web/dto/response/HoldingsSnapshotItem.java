package net.luversof.api.stock.web.dto.response;

import java.math.BigDecimal;

public record HoldingsSnapshotItem(
        String name,
        String symbol,
        long quantity,
        BigDecimal avgCost,
        BigDecimal priceAtDate,
        BigDecimal value,
        BigDecimal unrealizedProfit) {}
