package net.luversof.bookkeeping.domain;

import java.time.LocalDate;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

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
	@DateTimeFormat
	private LocalDate targetDate;
	
	public interface EntrySearchInfoSelect {}
	
	
	public LocalDate getCurrentDate(int baseDate) {
		LocalDate localDate = LocalDate.now();
		if (localDate.getDayOfMonth() < baseDate) {
			return localDate.minusMonths(1);
		}
		return localDate;
	}
}
