package net.luversof.api.bookkeeping;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import net.luversof.api.bookkeeping.util.BookkeepingUtils;

@Slf4j
public class BookkeepingUtilsTest {

	@Test
	public void test() {
		int baseDate = 4;
		LocalDate targetDate = LocalDate.parse("2016-01-03");
		LocalDate startDate = BookkeepingUtils.getMonthStartLocalDate(targetDate, baseDate);
		LocalDate endDate = BookkeepingUtils.getMonthEndLocalDate(targetDate, baseDate);
		
		LocalDate startDate2 = BookkeepingUtils.getYearStartLocalDate(targetDate, baseDate);
		LocalDate endDate2 = BookkeepingUtils.getYearEndLocalDate(targetDate, baseDate);
		log.debug("targetLocalDate : {}", targetDate);
		log.debug("startDate : {}", startDate);
		log.debug("endDate : {}", endDate);
		log.debug("startDate2 : {}", startDate2);
		log.debug("endDate2 : {}", endDate2);
	}
}
