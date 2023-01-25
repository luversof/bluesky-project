package net.luversof.bookkeeping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.datetime.standard.DateTimeContextHolder;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.service.BasicBookkeepingService;
import net.luversof.bookkeeping.service.CompositeEntryGroupService;
import net.luversof.bookkeeping.service.CompositeEntryService;

@Slf4j
class EntryTest implements GeneralTest {
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;

	@Autowired
	private CompositeEntryGroupService entryGroupService;
	
	@Autowired
	private CompositeEntryService entryService;

	@Autowired
	private EntryRepository entryRepository;
	
	private Bookkeeping bookkeeping;

	static final UUID TEST_USER_ID = UUID.fromString("35929103-da22-49e7-9d76-214bb081593f");
	
	@BeforeEach
	public void before() {
    	bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
	}

	// 세이브 테스트
	@Test
	void create() {
		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getBookkeepingId());
		
		Entry entry = new Entry();
		entry.setBookkeepingId(bookkeeping.getBookkeepingId());
		entry.setAmount(123);
		entry.setEntryGroupId(entryGroupList.stream().findAny().get().getEntryGroupId());
		entry.setMemo("test");
		
		entryService.create(entry);
		
		log.debug("Test : {}", entry);
	}
	
	/**
	 * 특정일 기준으로 한달 기간 조회
	 */
	@Test
	void test3() {
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
	void test5() {
		LocalDate startDate = LocalDate.parse("2016-05-02"); 
		LocalDate endDate = LocalDate.parse("2016-05-03");
		List<Entry> entryList = entryService.findByBookkeepingIdAndEntryDateBetween(bookkeeping.getBookkeepingId(), startDate, endDate);
		log.debug("entryList : {}", entryList);
	}

	@Test
	void deleteByBookkeeping() {
		entryRepository.deleteByBookkeepingId(bookkeeping.getBookkeepingId());
	}
	
	@Test
	void zoneIdTest() {
		ZoneId timeZone = DateTimeContextHolder.getDateTimeContext().getTimeZone();
		log.debug("zoneId : {}", timeZone);
	}
}
