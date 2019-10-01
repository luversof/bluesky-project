package net.luversof.bookkeeping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.StatisticsSearchInfoService;
import net.luversof.bookkeeping.service.StatisticsService;

@Slf4j
public class StatisticsTest extends GeneralTest {
	
	@Autowired
	private StatisticsSearchInfoService statisticsSearchInfoService;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private StatisticsService statisticsService;

	/**
	 * 통계를 위해 entryList 추출 -> statisticsList로 전환
	 */
	@Test
	public void test () {
		//1. 대상 bookkeeping 획득
		List<Bookkeeping> bookkeepingList = bookkeepingService.findByUserId(UUID.fromString("1"));
		Bookkeeping bookkeeping = bookkeepingList.get(0);
		
		//2 statisticsSearchInfo 획득
		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
		statisticsSearchInfo.setBookkeeping(bookkeeping);
		
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
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(UUID.randomUUID());
		
		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
		statisticsSearchInfo.setBookkeeping(bookkeeping);
		statisticsSearchInfo.setChronoUnit(ChronoUnit.YEARS);
		statisticsSearchInfo.setTargetLocalDate(LocalDate.now());
		
		List<Statistics> entryList = statisticsService.selectStatistics(statisticsSearchInfo);
		log.debug("entryList : {}", entryList);
	}
}
