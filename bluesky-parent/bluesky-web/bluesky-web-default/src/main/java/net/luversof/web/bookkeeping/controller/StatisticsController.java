package net.luversof.web.bookkeeping.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo.StatisticsSearchInfoSelect;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.bookkeeping.service.StatisticsSearchInfoService;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping("/bookkeeping/{bookkeepingId}/statistics")
public class StatisticsController {
	
	@Autowired
	private EntryService entryService;
	
	@Autowired
	private StatisticsSearchInfoService statisticsSearchInfoService;

	//기간별 통계 보기
	//월간, 년간 통계보기를 기본으로 삼자.
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public StatisticsSearchInfo getStatisticsSearchInfo(@PathVariable long bookkeepingId, @RequestParam(required = false) LocalDate targetLocalDate, @RequestParam ChronoUnit chronoUnit) {
		return statisticsSearchInfoService.getStatisticsSearchInfo(bookkeepingId, targetLocalDate, chronoUnit);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).bookkeeping.userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET)
	public List<Entry> getEntryList(@Validated(StatisticsSearchInfoSelect.class) StatisticsSearchInfo statisticsSearchInfo, Authentication authentication) {
		return entryService.findByStatisticsSearchInfo(statisticsSearchInfo);
	}
}
