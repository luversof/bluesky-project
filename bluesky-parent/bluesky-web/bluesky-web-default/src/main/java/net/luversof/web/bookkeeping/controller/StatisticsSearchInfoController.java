package net.luversof.web.bookkeeping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo.StatisticsSearchInfoSelect;
import net.luversof.bookkeeping.service.StatisticsSearchInfoService;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping("/bookkeeping/{bookkeepingId}/statisticsSearchInfo")
public class StatisticsSearchInfoController {
	
	@Autowired
	private StatisticsSearchInfoService statisticsSearchInfoService;

	//기간별 통계 보기
	//월간, 년간 통계보기를 기본으로 삼자.
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public StatisticsSearchInfo getStatisticsSearchInfo(@Validated(StatisticsSearchInfoSelect.class) StatisticsSearchInfo statisticsSearchInfo) {
		return statisticsSearchInfoService.getStatisticsSearchInfo(statisticsSearchInfo);
	}
}
