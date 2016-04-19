package net.luversof.web.bookkeeping.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping("bookkeeping/{bookkeeping.id}/entrySearchInfo")
public class EntrySearchInfoController {
	
	@Autowired
	private EntryService entryService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public EntrySearchInfo getEntryInfo(@PathVariable("bookkeeping.id") long bookkeepingId, @RequestParam(required = false) LocalDateTime startDateTime, @RequestParam(required = false) LocalDateTime endDateTime) {
		return entryService.getEntrySearchInfo(bookkeepingId, startDateTime, endDateTime);
	}
}
