package net.luversof.web.gate.bookkeeping.domain;

import java.time.ZonedDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
public record Bookkeeping(
	UUID id,
	UUID userId,
	String name,
	ZonedDateTime createDate,
	BookeepingExtraData extraData
) {
	
	@Data
	public static class BookeepingExtraData {
		private int baseDate = 1;
	}

}
