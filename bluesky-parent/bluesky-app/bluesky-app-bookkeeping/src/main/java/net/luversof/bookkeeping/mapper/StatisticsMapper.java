package net.luversof.bookkeeping.mapper;

import java.util.List;

import net.luversof.bookkeeping.domain.Statistics;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;

public interface StatisticsMapper {
	List<Statistics> selectStatistics(StatisticsSearchInfo statisticsSearchInfo);
}
