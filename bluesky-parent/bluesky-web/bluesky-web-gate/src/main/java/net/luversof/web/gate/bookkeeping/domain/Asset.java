package net.luversof.web.gate.bookkeeping.domain;

import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record Asset(
	UUID id,
	UUID bookkeepingId,
	UUID assetTypeId, 
	String name,
	Map<String, Object> jsonConfig 
) {

}
