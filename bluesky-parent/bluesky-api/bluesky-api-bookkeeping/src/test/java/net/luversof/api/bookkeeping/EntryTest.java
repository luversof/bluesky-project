//package net.luversof.api.bookkeeping;
//
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.util.List;
//import java.util.UUID;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.format.datetime.standard.DateTimeContextHolder;
//
//import lombok.extern.slf4j.Slf4j;
//import net.luversof.GeneralTest;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.base.domain.Entry;
//import net.luversof.api.bookkeeping.constant.TestConstant;
//import net.luversof.api.bookkeeping.domain.EntryGroup;
//import net.luversof.api.bookkeeping.repository.EntryRepository;
//import net.luversof.api.bookkeeping.service.BasicBookkeepingService;
//import net.luversof.api.bookkeeping.service.CompositeEntryGroupService;
//import net.luversof.api.bookkeeping.service.CompositeEntryService;
//
//@Slf4j
//class EntryTest implements GeneralTest {
//	
//	@Autowired
//	private BasicBookkeepingService bookkeepingService;
//
//	@Autowired
//	private CompositeEntryGroupService entryGroupService;
//	
//	@Autowired
//	private CompositeEntryService entryService;
//
//	@Autowired
//	private EntryRepository entryRepository;
//	
//	private Bookkeeping bookkeeping;
//
//	static final UUID TEST_USER_ID = UUID.fromString("35929103-da22-49e7-9d76-214bb081593f");
//	
//	@BeforeEach
//	public void before() {
//    	bookkeeping = bookkeepingService.findByUserId(TestConstant.USER_ID).stream().findFirst().get();
//	}
//
//	// 세이브 테스트
//	@Test
//	void create() {
//		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getId());
//		
//		Entry entry = new Entry();
//		entry.setBookkeepingId(bookkeeping.getId());
//		entry.setAmount(123);
//		entry.setEntryGroupId(entryGroupList.stream().findAny().get().getId());
//		entry.setMemo("test");
//		
//		entryService.create(entry);
//		
//		log.debug("Test : {}", entry);
//	}
//	
//	/**
//	 * 특정일 기준으로 한달 기간 조회
//	 */
//	@Test
//	void test3() {
//		log.debug("TEST : {}", ZonedDateTime.now());
//		
//		int baseDate = 21;
//		ZonedDateTime date = ZonedDateTime.now();
//		log.debug("date : {}, {}", date, date.getDayOfMonth());
//		
//		if (date.getDayOfMonth() >= baseDate) {
//			ZonedDateTime startDate = date.withDayOfMonth(baseDate);
//			log.debug("startDate : {}, {}", startDate, startDate.getDayOfMonth());
//			ZonedDateTime endDate;
//			if (baseDate == 1) {
//				endDate = date.withDayOfMonth(1).plusMonths(1).minusDays(1);
//			} else {
//				endDate = date.withDayOfMonth(baseDate - 1).plusMonths(1);
//			}
//			log.debug("endDate : {}, {}", endDate, endDate.getDayOfMonth());
//		}
//		log.debug("date : {}, {}", date, date.getDayOfMonth());
//		//log.debug("result : {}", entryService.findByBookkeepingIdAndEntryDateBetween(1, null, null));
//	}
//	
//	/**
//	 * 특정 일자 데이트 호출 확인
//	 */
//	@Test
//	void test5() {
//		ZonedDateTime startDate = ZonedDateTime.parse("2016-05-02"); 
//		ZonedDateTime endDate = ZonedDateTime.parse("2016-05-03");
//		List<Entry> entryList = entryService.findByBookkeepingIdAndEntryDateBetween(bookkeeping.getId(), startDate, endDate);
//		log.debug("entryList : {}", entryList);
//	}
//
//	@Test
//	void deleteByBookkeeping() {
//		entryRepository.deleteByBookkeepingId(bookkeeping.getId());
//	}
//	
//	@Test
//	void zoneIdTest() {
//		ZoneId timeZone = DateTimeContextHolder.getDateTimeContext().getTimeZone();
//		log.debug("zoneId : {}", timeZone);
//	}
//}
