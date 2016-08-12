package net.luversof.bookkeeping.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.context.i18n.LocaleContextHolder;

import lombok.Data;
import net.luversof.bookkeeping.util.BookkeepingUtils;

/**
 * 통계보기 검색 관련 객체 - 년/월 단위 검색 가능 - 특정 기간 동안 검색 가능
 * 
 * 검색시 필요한 파라메터
 * 	단위기간 검색 : bookeeping.id, chronoUnit
 * 	기간지정 검색 : 2차 스펙으로 고민해볼까...
 * @author bluesky
 *
 */
@Data
public class StatisticsSearchInfo {

	@Valid
	private Bookkeeping bookkeeping;

	private int baseDate = 1;
	
	/**
	 * 년/월 단위 통계 보기를 위해 필요한 파라메터
	 */
	@NotNull(groups = { Select.class, SelectEntryList.class })
	private ChronoUnit chronoUnit;

	@NotNull(groups = SelectEntryList.class)
	private LocalDate targetLocalDate = LocalDate.now();

	public ZonedDateTime getStartZonedDateTime() {
		return BookkeepingUtils.getStartLocalDate(targetLocalDate, baseDate, chronoUnit).atStartOfDay(ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}

	public ZonedDateTime getEndZonedDateTime() {
		return BookkeepingUtils.getEndLocalDate(targetLocalDate, baseDate, chronoUnit).atStartOfDay(ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
	}

	public interface Select {
	}

	public interface SelectEntryList {
	}

}
