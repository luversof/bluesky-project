package net.luversof.bookkeeping.service;

import static net.luversof.bookkeeping.BookkeepingConstants.BOOKKEEPING_TRANSACTIONMANAGER;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.mapper.StatisticsMapper;

@Service
@Transactional(BOOKKEEPING_TRANSACTIONMANAGER)
public class StatisticsService {
	
	@Autowired
	private StatisticsMapper statisticsMapper;

	/**
	 * 
	 * @param statisticsSearchInfo
	 * @return
	 */
	@Transactional(value = BOOKKEEPING_TRANSACTIONMANAGER, readOnly = true)
	public List<Statistics> selectStatistics(StatisticsSearchInfo statisticsSearchInfo) {
		return statisticsMapper.selectStatistics(statisticsSearchInfo);
	}
	
}
