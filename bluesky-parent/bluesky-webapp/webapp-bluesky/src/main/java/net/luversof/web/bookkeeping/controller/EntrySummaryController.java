package net.luversof.web.bookkeeping.controller;

import static net.luversof.core.Constants.JSON_MODEL_KEY;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.security.core.userdetails.LuversofUser;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/bookkeeping/entrySummary")
public class EntrySummaryController {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private EntryService entryService;
	
	/**
	 * 가계부가 1개만 존재한다는 전제조건으로 설정
	 * @author bluesky
	 *
	 */
	private Bookkeeping getBookkeeping(Authentication authentication) {
		return bookkeepingService.findByUserId(((LuversofUser) authentication.getPrincipal()).getId()).get(0);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public void get(Authentication authentication, ModelMap modelMap) {
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		modelMap.addAttribute(JSON_MODEL_KEY, entryService.findByBookkeepingId(bookkeeping.getId()));
	}
}
