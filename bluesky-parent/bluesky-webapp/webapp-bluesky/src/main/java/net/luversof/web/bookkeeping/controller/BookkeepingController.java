package net.luversof.web.bookkeeping.controller;

import static net.luversof.core.Constants.JSON_MODEL_KEY;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Bookkeeping.Add;
import net.luversof.bookkeeping.domain.Bookkeeping.Modify;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.LuversofUser;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/bookkeeping")
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping
	public String getBookkeeping(Authentication authentication, ModelMap modelMap) {
		modelMap.addAttribute(JSON_MODEL_KEY, bookkeepingService.findByUserId(((LuversofUser) authentication.getPrincipal()).getId()));
		return "/bookkeeping/bookkeeping";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Bookkeeping addBookkeeping(Authentication authentication, @RequestBody @Validated(Add.class) Bookkeeping bookkeeping) {
		bookkeeping.setUserId(((LuversofUser) authentication.getPrincipal()).getId());
		return bookkeepingService.save(bookkeeping);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Bookkeeping modifyBookkeeping(Authentication authentication, @RequestBody @Validated(Modify.class) Bookkeeping bookkeeping) {
		//TODO 본인 bookkeeping 확인 절차가 있어야 함
		bookkeeping.setUserId(((LuversofUser) authentication.getPrincipal()).getId());
		return bookkeepingService.save(bookkeeping);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void removeBookkeeping(Authentication authentication, @RequestBody @Validated(Modify.class) Bookkeeping bookkeeping) {
		//TODO 본인 bookkeeping 확인 절차가 있어야 함
		bookkeeping.setUserId(((LuversofUser) authentication.getPrincipal()).getId());
		bookkeepingService.delete(bookkeeping);
	}
}
