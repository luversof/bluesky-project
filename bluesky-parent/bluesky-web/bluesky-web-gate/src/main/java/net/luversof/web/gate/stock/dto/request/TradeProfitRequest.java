package net.luversof.web.gate.stock.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class TradeProfitRequest {
	UUID userId;
	List<UUID> accountIdList;
	List<UUID> stockItemIdList;
	Instant startDate;
	Instant endDate;
	TradeProfitRequestGroup groupBy;
}