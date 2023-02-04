package net.luversof.api.bookkeeping.mapper;

import java.util.List;

import net.luversof.api.bookkeeping.domain.Statistics;
import net.luversof.api.bookkeeping.domain.StatisticsSearchInfo;


public interface StatisticsMapper {
	List<Statistics> selectStatistics(StatisticsSearchInfo statisticsSearchInfo);
}
