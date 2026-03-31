package net.luversof.api.stock.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TradeSearchRequest(
        UUID userId,
        List<UUID> accountIdList,
        List<UUID> stockItemIdList,
        Instant startDate,
        Instant endDate) {}
