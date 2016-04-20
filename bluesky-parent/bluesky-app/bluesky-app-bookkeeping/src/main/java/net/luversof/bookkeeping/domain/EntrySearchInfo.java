package net.luversof.bookkeeping.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Entry 조회시 필요한 파라메터를 관리하기 위한 클래스
 * @author bluesky
 *
 */
@Data
public class EntrySearchInfo {
	@NotNull(groups = EntrySearchInfoSelect.class)
	@Min(value = 1, groups = EntrySearchInfoSelect.class)
	private long bookkeepingId;
	
	@NotNull(groups = EntrySearchInfoSelect.class)
	private LocalDateTime startDateTime;
	
	@NotNull(groups = EntrySearchInfoSelect.class)
	private LocalDateTime endDateTime;
	
	public interface EntrySearchInfoSelect {}
}
