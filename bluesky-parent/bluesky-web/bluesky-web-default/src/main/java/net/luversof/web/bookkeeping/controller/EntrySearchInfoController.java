package net.luversof.web.bookkeeping.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.domain.EntrySearchInfo.EntrySearchInfoSelect;
import net.luversof.bookkeeping.service.EntrySearchInfoService;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping("/bookkeeping/{bookkeeping.id}/entrySearchInfo")
public class EntrySearchInfoController {
	
	@Autowired
	private EntrySearchInfoService entrySearchInfoService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public EntrySearchInfo getEntrySearchInfo(@Validated(EntrySearchInfoSelect.class) EntrySearchInfo entrySearchInfo) {
		return entrySearchInfoService.getEntrySearchInfo(entrySearchInfo);
	}
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public LocalDateTime test(@RequestParam(required = false) LocalDateTime test) {
		System.out.println(test);
		return test;
	}
}
