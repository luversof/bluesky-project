package net.luversof.bookkeeping.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.mapper.StatisticsMapper;

@Service
public class StatisticsService {
	
	@Autowired
	private StatisticsMapper statisticsMapper;

	/**
	 * 
	 * @param statisticsSearchInfo
	 * @return
	 */
	public List<Statistics> selectStatistics(StatisticsSearchInfo statisticsSearchInfo) {
		return statisticsMapper.selectStatistics(statisticsSearchInfo);
	}
	
}
