package net.luversof.bookkeeping.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;

@Service
public class StatisticsSearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	public StatisticsSearchInfo getStatisticsSearchInfo(StatisticsSearchInfo statisticsSearchInfo) {
		Bookkeeping targetBookkeeping = bookkeepingService.getUserBookkeeping(statisticsSearchInfo.getBookkeeping().getUserId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
		statisticsSearchInfo.setBookkeeping(targetBookkeeping);
		statisticsSearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
		return statisticsSearchInfo;
	}
}
