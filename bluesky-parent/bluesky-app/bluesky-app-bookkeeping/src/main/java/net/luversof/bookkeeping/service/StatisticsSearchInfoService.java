package net.luversof.bookkeeping.service;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;

@Service
@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
public class StatisticsSearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public StatisticsSearchInfo getStatisticsSearchInfo(long bookkeepingId, LocalDate targetLocalDate, ChronoUnit chronoUnit) {
		if (targetLocalDate == null) {
			targetLocalDate = LocalDate.now();
		}
		StatisticsSearchInfo statisticsSearchInfo = new StatisticsSearchInfo();
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(bookkeepingId);
		statisticsSearchInfo.setTargetLocalDate(targetLocalDate);
		statisticsSearchInfo.setBookkeepingId(targetBookkeeping.getId());
		statisticsSearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		statisticsSearchInfo.setChronoUnit(chronoUnit);
		return statisticsSearchInfo;
	}
}
