package net.luversof.web.gate.stock.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record Account(
	UUID id, 
	UUID userId, 
	String name, 
	OffsetDateTime createdDate, 
	Map<String, Object> jsonConfig) {
}
