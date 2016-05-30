package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.StatisticsSearchInfo;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.web.constant.AuthorizeRole;

/**
 * 통계의 경우
 * StatisticsSearchInfo에 따른 데이터 호출 부분을 표현함.
 * 전체 자산 현황은 자산 정보 설정 부분에서 확인 가능
 * @author bluesky
 *
 */
@RestController
@RequestMapping("/bookkeeping/{bookkeeping.id}/statistics")
public class StatisticsController {
	
	@Autowired
	private EntryService entryService;
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).bookkeeping.userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Entry> getEntryList(@Validated(StatisticsSearchInfo.SelectEntryList.class) StatisticsSearchInfo statisticsSearchInfo, Authentication authentication) {
		return entryService.findByStatisticsSearchInfo(statisticsSearchInfo);
	}
}
