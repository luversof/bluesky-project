package net.luversof.web.gate.stock.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class DividendRequest {
	private UUID userId;
	private List<UUID> accountIdList;
	private List<UUID> stockItemIdList;
	private Instant startDate;
	private Instant endDate;
}
