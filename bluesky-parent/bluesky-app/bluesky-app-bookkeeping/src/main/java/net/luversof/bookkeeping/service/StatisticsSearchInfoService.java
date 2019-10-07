package net.luversof.bookkeeping.service;


import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.core.exception.BlueskyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;

@Service
public class StatisticsSearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public StatisticsSearchInfo getStatisticsSearchInfo(StatisticsSearchInfo statisticsSearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(statisticsSearchInfo.getBookkeeping()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		statisticsSearchInfo.setBookkeeping(targetBookkeeping);
		statisticsSearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return statisticsSearchInfo;
	}
}
