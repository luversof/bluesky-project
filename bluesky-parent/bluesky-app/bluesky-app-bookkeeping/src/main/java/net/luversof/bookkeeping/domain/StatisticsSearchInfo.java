package net.luversof.bookkeeping.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

	private int baseDate;

	/**
	 * 년/월 단위 통계 보기를 위해 필요한 파라메터
	 */
	@NotNull(groups = { StatisticsSearchInfoSelect.class, StatisticsSearchInfoSelectEntryList.class })
	private ChronoUnit chronoUnit;

	@NotNull(groups = StatisticsSearchInfoSelectEntryList.class)
	private LocalDate targetLocalDate;

	private LocalDateTime startLocalDateTime;

	private LocalDateTime endLocalDateTime;

	public LocalDateTime getStartLocalDateTime() {
		if (chronoUnit == null) {
			return startLocalDateTime;
		} else if (chronoUnit == ChronoUnit.YEARS) {
			return BookkeepingUtils.getYearStartLocalDate(targetLocalDate, baseDate).atStartOfDay();
		} else if (chronoUnit == ChronoUnit.MONTHS) {
			return BookkeepingUtils.getMonthStartLocalDate(targetLocalDate, baseDate).atStartOfDay();
		}
		return null;
	}

	public LocalDateTime getEndLocalDateTime() {
		if (chronoUnit == null) {
			return endLocalDateTime;
		} else if (chronoUnit == ChronoUnit.YEARS) {
			return BookkeepingUtils.getYearEndLocalDate(targetLocalDate, baseDate).atStartOfDay();
		} else if (chronoUnit == ChronoUnit.MONTHS) {
			return BookkeepingUtils.getMonthEndLocalDate(targetLocalDate, baseDate).atStartOfDay();
		}
		return null;
	}

	public interface StatisticsSearchInfoSelect {
	}

	public interface StatisticsSearchInfoSelectEntryList {
	}

}
