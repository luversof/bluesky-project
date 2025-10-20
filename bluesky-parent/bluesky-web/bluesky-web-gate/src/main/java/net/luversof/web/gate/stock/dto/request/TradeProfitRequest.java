package net.luversof.web.gate.stock.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TradeProfitRequest(
	UUID userId,
	List<UUID> accountIdList,
	List<UUID> stockItemIdList,
	Instant startDate,
	Instant endDate,
	TradeProfitRequestGroup groupBy
	) {
}