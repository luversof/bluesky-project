package net.luversof.web.gate.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Dividend(
	UUID id, 
	UUID accountId, 
	UUID stockItemId, 
	BigDecimal price, 
	BigDecimal tax, 
	Instant recordDate, 
	Instant payDate) {
}
