package net.luversof.web.gate.bookkeeping.domain;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
