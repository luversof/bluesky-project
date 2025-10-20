package net.luversof.web.gate.stock.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Account(
	UUID id, 
	UUID userId, 
	String name, 
	Instant createdDate, 
	Map<String, Object> jsonConfig) {
}
