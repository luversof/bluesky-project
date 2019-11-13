package net.luversof.bookkeeping.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.context.i18n.LocaleContextHolder;

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
	private UUID userId;
	
	/**
	 * user의 bookkeeping table baseDate 값
	 */
	private int baseDate = 1;
	
	/**
	 * 검색할 월이 있는 대상 날짜
	 * 검색 시 넘어오는 파라메터 값
	 */
	@NotNull(groups = SelectEntryList.class)
	private LocalDate targetLocalDate = LocalDate.now();
	
	
	public interface Select {}
	public interface SelectEntryList {}
	
	public ZonedDateTime getStartZonedDateTime() {
		return BookkeepingUtils.getMonthStartLocalDate(targetLocalDate, baseDate).atStartOfDay(ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}
	
	public ZonedDateTime getEndZonedDateTime() {
		return BookkeepingUtils.getMonthEndLocalDate(targetLocalDate, baseDate).atStartOfDay(ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}
}
