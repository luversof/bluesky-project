package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingViewController {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
//	@RequestMapping(value = "/index", method = RequestMethod.GET)
//	public void index() {
//	}
	
	@RequestMapping(value = "/setting", method = RequestMethod.GET)
	public void setting(ModelMap modelMap, Authentication authentication) {
		// 기본 설정한 bookkeeping Id를 반환하자.
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		List<Bookkeeping> bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		if (!bookkeepingList.isEmpty()) {
			modelMap.addAttribute("bookkeepingId", bookkeepingList.get(0).getId());
		}
	}
	

//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
//	@RequestMapping("/$!")
//	public String home(Bookkeeping bookkeeping) {
//		return redirectEntryList(bookkeeping.getId());
//	}
//	
//	private String redirectEntryList(@PathVariable long bookkeepingId) {
//		return MessageFormat.format("redirect:/bookkeeping/{0}/view/entry", bookkeepingId);
//	}
	
	@RequestMapping(value = "/create", method=RequestMethod.GET)
	public void createForm() {}
	
	@RequestMapping(value = "/{bookkeepingId}/entry/index", method = RequestMethod.GET)
	public String list(@PathVariable long bookkeepingId, @RequestParam(defaultValue = "1") int page) {
		// 기본은 달 기준으로 요청을 처리해야할 듯한데..
		// 요청 월이 없으면 현재달 기준으로 검색 해야함.
		// 설정한 일자 기준으로 날짜 검색해야함
		// TODO 우선 그냥 전체 모두 호출하는 식으로 처리해보자
		// 이건 그냥 json response로 통합하는게 나을거 같은데..
		return "bookkeeping/entry/index";
	}
	
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{bookkeepingId}/entryGroup/setting", method = RequestMethod.GET)
	public String entryGroupList(@PathVariable long bookkeepingId) {
		return "bookkeeping/entryGroup/setting";
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{bookkeepingId}/asset/setting", method = RequestMethod.GET)
	public String assetList(@PathVariable long bookkeepingId) {
		return "bookkeeping/asset/setting";
	}
	
}
