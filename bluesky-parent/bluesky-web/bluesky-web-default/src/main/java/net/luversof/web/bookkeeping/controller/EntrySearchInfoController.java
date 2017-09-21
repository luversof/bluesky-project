package net.luversof.web.bookkeeping.controller;

import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.domain.EntrySearchInfo.Select;
import net.luversof.bookkeeping.service.EntrySearchInfoService;
import net.luversof.web.constant.AuthorizeRole;

//@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/entrySearchInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntrySearchInfoController {
	
	@Autowired
	private EntrySearchInfoService entrySearchInfoService;
	
	@GetMapping
	public EntrySearchInfo getEntrySearchInfo(@Validated(Select.class) EntrySearchInfo entrySearchInfo) {
		return entrySearchInfoService.getEntrySearchInfo(entrySearchInfo);
	}
	
	@GetMapping(value = "/test")
	public ZonedDateTime test(@RequestParam(required = false) ZonedDateTime test) {
		System.out.println(test);
		return test;
	}
}
