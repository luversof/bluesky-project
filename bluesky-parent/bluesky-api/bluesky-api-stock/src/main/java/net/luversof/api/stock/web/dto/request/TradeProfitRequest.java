package net.luversof.api.stock.web.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 주식 손익 계산 요청 DTO
 */
public record TradeProfitRequest(
	UUID accountId,
	UUID stockItemId,
	OffsetDateTime fromDate,
	OffsetDateTime toDate
	) {
}
