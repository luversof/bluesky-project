//package net.luversof.web.bookkeeping.controller;
//
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.ui.ModelMap;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
//import net.luversof.bookkeeping.domain.Bookkeeping;
//import net.luversof.bookkeeping.service.BookkeepingService;
//import net.luversof.user.domain.User;
//
////@Controller
//@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
//@Deprecated
//public class BookkeepingViewController {
//	
//	@Autowired
//	private BookkeepingService bookkeepingService;
//	
////	@RequestMapping(value = "/index", method = RequestMethod.GET)
////	public void index() {
////	}
//	
//	@GetMapping(value = "/test")
//	public void test() {
//	}
//	
//	@GetMapping(value = "/setting")
//	public void setting(ModelMap modelMap, User user) {
//		// 기본 설정한 bookkeeping Id를 반환하자.
//		Optional<Bookkeeping> userBookkeeping = bookkeepingService.getUserBookkeeping(user.getId());
//		if (userBookkeeping.isPresent()) {
//			modelMap.addAttribute("bookkeepingId", userBookkeeping.get().getId());
//		}
//	}
//	
//
////	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
////	@RequestMapping("/$!")
////	public String home(Bookkeeping bookkeeping) {
////		return redirectEntryList(bookkeeping.getId());
////	}
////	
////	private String redirectEntryList(@PathVariable long bookkeepingId) {
////		return MessageFormat.format("redirect:/bookkeeping/{0}/view/entry", bookkeepingId);
////	}
//	
//	@GetMapping(value = "/create")
//	public void createForm() {}
//	
//	@GetMapping(value = "/{bookkeepingId}/entry/index")
//	public String entryList(@PathVariable long bookkeepingId, @RequestParam(defaultValue = "1") int page) {
//		// 기본은 달 기준으로 요청을 처리해야할 듯한데..
//		// 요청 월이 없으면 현재달 기준으로 검색 해야함.
//		// 설정한 일자 기준으로 날짜 검색해야함
//		// TODO 우선 그냥 전체 모두 호출하는 식으로 처리해보자
//		// 이건 그냥 json response로 통합하는게 나을거 같은데..
//		return "bookkeeping/entry/index";
//	}
//	
//	
//	@BlueskyPreAuthorize
//	@GetMapping(value = "/{bookkeepingId}/entryGroup/setting")
//	public String entryGroupList(@PathVariable long bookkeepingId) {
//		return "bookkeeping/entryGroup/setting";
//	}
//	
//	@BlueskyPreAuthorize
//	@GetMapping(value = "/{bookkeepingId}/asset/setting")
//	public String assetList(@PathVariable long bookkeepingId) {
//		return "bookkeeping/asset/setting";
//	}
//	
//	@BlueskyPreAuthorize
//	@GetMapping(value = "/{bookkeepingId}/statistics/index")
//	public String statisticsList(@PathVariable long bookkeepingId) {
//		return "bookkeeping/statistics/index";
//	}
//}
