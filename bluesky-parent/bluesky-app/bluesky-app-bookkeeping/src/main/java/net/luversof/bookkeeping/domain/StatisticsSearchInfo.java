package net.luversof.bookkeeping.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import net.luversof.bookkeeping.util.BookkeepingUtils;
import net.luversof.core.exception.BlueskyException;

/**
 * 통계보기 검색 관련 객체
 * @author bluesky
 *
 */
@Data
public class StatisticsSearchInfo {
	
	@NotNull(groups = StatisticsSearchInfoSelect.class)
	@Min(value = 1, groups = StatisticsSearchInfoSelect.class)
	private long bookkeepingId;
	
	@NotNull(groups = StatisticsSearchInfoSelect.class)
	private LocalDateTime startDateTime;
	
	@NotNull(groups = StatisticsSearchInfoSelect.class)
	private LocalDateTime endDateTime;
	
	public interface StatisticsSearchInfoSelect {}
	
	
	
//	public static StatisticsSearchInfo getEntrySearchInfo(long bookkeepingId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
//		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
//		Bookkeeping targetBookkeeping = bookkeepingService.findOne(bookkeepingId);
//		if (targetBookkeeping == null) {
//			throw new BlueskyException("NOT_EXIST_BOOKKEEPING");
//		}
//		entrySearchInfo.setBookkeepingId(targetBookkeeping.getId());
//		if (startDateTime == null || endDateTime == null) {
//			entrySearchInfo.setStartDateTime(BookkeepingUtils.getStartDateTime(targetBookkeeping.getBaseDate()));
//			entrySearchInfo.setEndDateTime(BookkeepingUtils.getEndDateTime(targetBookkeeping.getBaseDate()));
//		} else {
//			entrySearchInfo.setStartDateTime(startDateTime);
//			entrySearchInfo.setEndDateTime(endDateTime);
//		}
//		return entrySearchInfo;
//	}
}
