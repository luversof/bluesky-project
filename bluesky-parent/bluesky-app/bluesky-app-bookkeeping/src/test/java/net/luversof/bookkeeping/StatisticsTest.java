package net.luversof.bookkeeping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.service.BasicBookkeepingService;
import net.luversof.bookkeeping.service.StatisticsSearchInfoService;
import net.luversof.bookkeeping.service.StatisticsService;

@Slf4j
public class StatisticsTest extends GeneralTest {
	
	@Autowired
	private StatisticsSearchInfoService statisticsSearchInfoService;
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;
	
	@Autowired
	private StatisticsService statisticsService;
	
	private Bookkeeping bookkeeping;

	@BeforeEach
	public void before() {
		bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
	}

	/**
	 * 통계를 위해 entryList 추출 -> statisticsList로 전환
	 */
	@Test
	public void test () {
		//1. 대상 bookkeeping 획득
		
		//2 statisticsSearchInfo 획득
		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
		statisticsSearchInfo.setBookkeepingId(bookkeeping.getBookkeepingId());
		
		statisticsSearchInfoService.getStatisticsSearchInfo(statisticsSearchInfo);
		
		log.debug("statisticsSearchInfo : {}", statisticsSearchInfo);
		
		//3. entryList 획득
		// List<Entry> entryList = entryService.findByStatisticsSearchInfo(statisticsSearchInfo);
		// log.debug("entryList : {}", entryList);
		
		
		//3. entryList 획득
		List<Statistics> entryList = statisticsService.selectStatistics(statisticsSearchInfo);
		log.debug("entryList : {}", entryList);
		
	}
	
	@Test
	public void test2() {
		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
		statisticsSearchInfo.setBookkeepingId(bookkeeping.getBookkeepingId());
		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
		statisticsSearchInfo.setTargetLocalDate(LocalDate.now());
		
		List<Statistics> entryList = statisticsService.selectStatistics(statisticsSearchInfo);
		log.debug("entryList : {}", entryList);
	}
}
