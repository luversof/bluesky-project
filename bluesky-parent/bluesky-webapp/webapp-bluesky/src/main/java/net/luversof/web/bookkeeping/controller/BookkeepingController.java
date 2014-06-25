package net.luversof.web.bookkeeping.controller;

import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.LuversofUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bookkeeping")
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping({ "", "/index" })
	public String index(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute("bookkeeping", bookkeepingService.findByUserId(((LuversofUser) authentication.getPrincipal()).getId()));
		return "/bookkeeping/index";
	}
}
