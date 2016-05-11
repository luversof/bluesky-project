package net.luversof.bookkeeping.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.bookkeeping.util.BookkeepingUtils;

/**
 * Entry 입력시 기존 입력 정보 참고에 필요한 파라메터를 관리하기 위한 클래스
 * 월 단위로 무조건 고정하여 한달씩 보여준다.
 * @author bluesky
 *
 */
@Data
public class EntrySearchInfo {
	
	/**
	 * user의 bookkeeping table id 값
	 */
	@NotNull(groups = EntrySearchInfoSelect.class)
	@Min(value = 1, groups = EntrySearchInfoSelect.class)
	private long bookkeepingId;
	
	/**
	 * user의 bookkeeping table baseDate 값
	 */
	private int baseDate;
	
	/**
	 * 검색할 월이 있는 대상 날짜
	 * 검색 시 넘어오는 파라메터 값
	 */
	@NotNull(groups = EntrySearchInfoSelect.class)
	private LocalDate targetLocalDate;
	
	
	public interface EntrySearchInfoSelect {}
	
	public LocalDateTime getStartLocalDateTime() {
		return BookkeepingUtils.getMonthStartLocalDate(targetLocalDate, baseDate).atStartOfDay();
	}
	
	public LocalDateTime getEndLocalDateTime() {
		return BookkeepingUtils.getMonthEndLocalDate(targetLocalDate, baseDate).atStartOfDay();
	}
}
