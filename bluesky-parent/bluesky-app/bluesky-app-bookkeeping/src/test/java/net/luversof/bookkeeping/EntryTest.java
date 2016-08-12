package net.luversof.bookkeeping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.datetime.standard.DateTimeContextHolder;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.bookkeeping.service.EntryService;

@Slf4j
public class EntryTest extends GeneralTest {
	
	@Autowired
	private BookkeepingService bookkeepingService;

	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private EntryService entryService;
	
	// 세이브 테스트
	@Test
	public void test2() {
		Bookkeeping bookkeeping = bookkeepingService.findOne(1);
		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getId());
		
		Entry entry = new Entry();
		entry.setBookkeeping(bookkeeping);
		entry.setAmount(123);
		entry.setEntryGroup(entryGroupList.get(3));
		entry.setMemo("test");
		
		entryService.create(entry);
		
		log.debug("Test : {}", entry);
	}
	
	/**
	 * 특정일 기준으로 한달 기간 조회
	 */
	@Test
	public void test3() {
		log.debug("TEST : {}", ZonedDateTime.now());
		
		int baseDate = 21;
		LocalDate localDate = LocalDate.now();
		log.debug("localDate : {}, {}", localDate, localDate.getDayOfMonth());
		
		if (localDate.getDayOfMonth() >= baseDate) {
			LocalDate startLocalDate = localDate.withDayOfMonth(baseDate);
			log.debug("startLocalDate : {}, {}", startLocalDate, startLocalDate.getDayOfMonth());
			LocalDate endLocalDate;
			if (baseDate == 1) {
				endLocalDate = localDate.withDayOfMonth(1).plusMonths(1).minusDays(1);
			} else {
				endLocalDate = localDate.withDayOfMonth(baseDate - 1).plusMonths(1);
			}
			log.debug("endLocalDate : {}, {}", endLocalDate, endLocalDate.getDayOfMonth());
		}
		
		ZonedDateTime testZonedlDateTime = localDate.atStartOfDay(ZoneId.of(LocaleContextHolder.getTimeZone().getID()));
		log.debug("testZonedlDateTime : {}, {}", testZonedlDateTime, testZonedlDateTime.getDayOfMonth());
		//log.debug("result : {}", entryService.findByBookkeepingIdAndEntryDateBetween(1, null, null));
	}
	
	/**
	 * 특정 일자 데이트 호출 확인
	 */
	@Test
	public void test5() {
		LocalDate startDate = LocalDate.parse("2016-05-02"); 
		LocalDate endDate = LocalDate.parse("2016-05-03");
		ZoneId timeZone = ZoneId.of(LocaleContextHolder.getTimeZone().getID());
		List<Entry> entryList = entryService.findByBookkeepingIdAndEntryDateBetween(1, startDate.atStartOfDay(timeZone), endDate.atStartOfDay(timeZone));
		log.debug("entryList : {}", entryList);
	}
	
	
	@Test
	public void test6() {
		entryService.test(12);
	}
	
	@Test
	public void LocaleTest() {
		
	}
	
	@Test
	public void zoneIdTest() {
		ZoneId timeZone = DateTimeContextHolder.getDateTimeContext().getTimeZone();
		log.debug("zoneId : {}", timeZone);
	}
}
