package net.luversof.bookkeeping.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookkeepingUtil {

	public static LocalDateTime getStartDate(int baseDate) {
		LocalDate localDate = LocalDate.now();
		LocalDate startLocalDate = null;
		if (localDate.getDayOfMonth() >= baseDate) {
			startLocalDate = localDate.withDayOfMonth(baseDate);
		} else {
			startLocalDate = localDate.minusMonths(1).withDayOfMonth(baseDate);
		}
		
		return startLocalDate.atStartOfDay();
	}
}
