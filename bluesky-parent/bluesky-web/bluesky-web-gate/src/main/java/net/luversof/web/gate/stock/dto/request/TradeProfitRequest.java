package net.luversof.web.gate.stock.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TradeProfitRequest(
	UUID userId,
	List<UUID> accountIdList,
	List<UUID> stockItemIdList,
	OffsetDateTime startDate,
	OffsetDateTime endDate,
	TradeProfitRequestGroup groupBy
	) {
}