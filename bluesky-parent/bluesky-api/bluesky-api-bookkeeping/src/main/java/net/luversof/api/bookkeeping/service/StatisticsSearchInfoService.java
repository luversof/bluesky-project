//package net.luversof.api.bookkeeping.service;
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//import net.luversof.api.bookkeeping.domain.StatisticsSearchInfo;
//
////@Service
//public class StatisticsSearchInfoService {
//	
//	@Autowired
//	private BasicBookkeepingService bookkeepingService;
//	
//	public StatisticsSearchInfo getStatisticsSearchInfo(StatisticsSearchInfo statisticsSearchInfo) {
//		Bookkeeping targetBookkeeping = bookkeepingService.findById(statisticsSearchInfo.getBookkeepingId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		statisticsSearchInfo.setBaseDate(targetBookkeeping.getBaseDate());
//		return statisticsSearchInfo;
//	}
//}
