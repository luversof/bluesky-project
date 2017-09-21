package net.luversof.web.bookkeeping.controller;

import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.service.StatisticsSearchInfoService;
import net.luversof.web.constant.AuthorizeRole;

//@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/statisticsSearchInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsSearchInfoController {
	
	@Autowired
	private StatisticsSearchInfoService statisticsSearchInfoService;

	//기간별 통계 보기
	//월간, 년간 통계보기를 기본으로 삼자.
	
	@GetMapping
	public StatisticsSearchInfo getStatisticsSearchInfo(@Validated(StatisticsSearchInfo.Select.class) StatisticsSearchInfo statisticsSearchInfo) {
		return statisticsSearchInfoService.getStatisticsSearchInfo(statisticsSearchInfo);
	}
	
	@GetMapping(value = "/test")
	public ChronoUnit test(ChronoUnit chronoUnit) {
		System.out.println(chronoUnit);
		return chronoUnit;
	}
	
	@GetMapping(value = "/test2")
	public StatisticsSearchInfo test(StatisticsSearchInfo statisticsSearchInfo) {
		statisticsSearchInfoService.getStatisticsSearchInfo(statisticsSearchInfo);
		System.out.println(statisticsSearchInfo);
		return statisticsSearchInfo;
	}
}
