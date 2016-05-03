package net.luversof.bookkeeping.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.bookkeeping.util.BookkeepingUtils;

/**
 * Entry 입력시 기존 입력 정보 참고에 필요한 파라메터를 관리하기 위한 클래스
 * 월단위로 무조건 고정하여 한달씩 보여준다.
 * @author bluesky
 *
 */
@Data
public class EntrySearchInfo {
	@NotNull(groups = EntrySearchInfoSelect.class)
	@Min(value = 1, groups = EntrySearchInfoSelect.class)
	private long bookkeepingId;
	
	@NotNull(groups = EntrySearchInfoSelect.class)
	private LocalDate targetLocalDate;
	
	private int baseDate;
	
	public interface EntrySearchInfoSelect {}
	
	public LocalDateTime getStartDateTime() {
		return BookkeepingUtils.getStartDateTime(targetLocalDate, baseDate);
	}
	
	public LocalDateTime getEndDateTime() {
		return BookkeepingUtils.getEndDateTime(targetLocalDate, baseDate);
	}
}
