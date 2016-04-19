package net.luversof.bookkeeping.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import lombok.Data;

/**
 * Entry 조회시 필요한 파라메터를 관리하기 위한 클래스
 * @author bluesky
 *
 */
@Data
public class EntrySearchInfo {
	private long bookkeepingId;
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime startDateTime;
	@JsonDeserialize(using=LocalDateTimeDeserializer.class)
	private LocalDateTime endDateTime;
}
