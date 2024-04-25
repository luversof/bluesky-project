//package net.luversof.api.bookkeeping;
//
//import java.time.LocalDate;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import lombok.extern.slf4j.Slf4j;
//import net.luversof.GeneralTest;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.constant.TestConstant;
//import net.luversof.api.bookkeeping.domain.Statistics;
//import net.luversof.api.bookkeeping.domain.StatisticsSearchInfo;
//import net.luversof.api.bookkeeping.service.BasicBookkeepingService;
//import net.luversof.api.bookkeeping.service.StatisticsSearchInfoService;
//import net.luversof.api.bookkeeping.service.StatisticsService;
//
//@Slf4j
//class StatisticsTest implements GeneralTest {
//	
//	@Autowired
//	private StatisticsSearchInfoService statisticsSearchInfoService;
//	
//	@Autowired
//	private BasicBookkeepingService bookkeepingService;
//	
//	@Autowired
//	private StatisticsService statisticsService;
//	
//	private Bookkeeping bookkeeping;
//
//	@BeforeEach
//	public void before() {
//		bookkeeping = bookkeepingService.findByUserId(TestConstant.USER_ID).stream().findFirst().get();
//	}
//
//	/**
//	 * 통계를 위해 entryList 추출 -> statisticsList로 전환
//	 */
//	@Test
//	void test () {
//		//1. 대상 bookkeeping 획득
//		
//		//2 statisticsSearchInfo 획득
//		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
//		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
//		statisticsSearchInfo.setBookkeepingId(bookkeeping.getId());
//		
//		statisticsSearchInfoService.getStatisticsSearchInfo(statisticsSearchInfo);
//		
//		log.debug("statisticsSearchInfo : {}", statisticsSearchInfo);
//		
//		//3. entryList 획득
//		// List<Entry> entryList = entryService.findByStatisticsSearchInfo(statisticsSearchInfo);
//		// log.debug("entryList : {}", entryList);
//		
//		
//		//3. entryList 획득
//		List<Statistics> entryList = statisticsService.selectStatistics(statisticsSearchInfo);
//		log.debug("entryList : {}", entryList);
//		
//	}
//	
//	@Test
//	void test2() {
//		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
//		statisticsSearchInfo.setBookkeepingId(bookkeeping.getId());
//		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
//		statisticsSearchInfo.setTargetLocalDate(LocalDate.now());
//		
//		List<Statistics> entryList = statisticsService.selectStatistics(statisticsSearchInfo);
//		log.debug("entryList : {}", entryList);
//	}
//}
