package net.luversof.web.gate.bookkeeping.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record Bookkeeping(
	UUID id,
	UUID userId,
	String name,
	Instant createDate,
	Map<String, Object> jsonConfig
) {
	
//	@Data
//	public static class BookeepingExtraData {
//		private int baseDate = 1;
//	}

}
