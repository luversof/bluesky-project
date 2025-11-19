package net.luversof.api.stock.web.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class DividendSearchRequest {

	private UUID userId;
	private List<UUID> accountIdList;
	private List<UUID> stockItemIdList;
	private Instant startDate;
	private Instant endDate;
}
