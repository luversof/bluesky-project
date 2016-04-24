package net.luversof.bookkeeping;

import java.time.LocalDate;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BookkeepingUtilsTest {

	@Test
	public void test() {
		
		
		LocalDate localDate = LocalDate.now();
		LocalDate localDate2 = LocalDate.now();
		
		LocalDate localDate3 = localDate.minusMonths(1);
		
		log.debug("localDate : {}", localDate);
		log.debug("localDate2 : {}", localDate2);
		log.debug("localDate3 : {}", localDate3);
		
	}
}
