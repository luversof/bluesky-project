package net.luversof.bookkeeping.service;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

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
	
	public StatisticsSearchInfo getStatisticsSearchInfo(StatisticsSearchInfo statisticsSearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(statisticsSearchInfo.getBookkeeping().getId());
		statisticsSearchInfo.setBookkeeping(targetBookkeeping);
		statisticsSearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return statisticsSearchInfo;
	}
}
