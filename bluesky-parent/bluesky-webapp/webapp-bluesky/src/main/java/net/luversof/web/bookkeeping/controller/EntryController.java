package net.luversof.web.bookkeeping.controller;

import static net.luversof.core.Constants.JSON_MODEL_KEY;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.Entry.Add;
import net.luversof.bookkeeping.domain.Entry.Modify;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.LuversofUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/bookkeeping/entry")
public class EntryController {
	
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
	
	/*
	 * 검색 조건은 어떤 것이 있을까?
	 * 1. 날짜
	 * 2. Asset 별
	 * 3. 입출력 별 등등
	 * 위의 다양한 경우가 있지만 1차 순위로 월별 검색으로 처리해보자.
	 * 그 다음 월별이 아닌 마감일설정 별로 검색 처리를 해보자.
	 * 검색 조건별 검색처리는 통계 정보 제공으로 넘기자.
	 * 
	 * 페이지에서 사용할 데이터를 내려줘야할까?
	 * 우선 검색 결과를 내려준 이후 처리 고민
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public void get(Authentication authentication, ModelMap modelMap) {
		
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		modelMap.addAttribute(JSON_MODEL_KEY, entryService.findByBookkeepingId(bookkeeping.getId()));
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Entry add(Authentication authentication, @RequestBody @Validated(Add.class) Entry entry) {
		entry.setBookkeeping(getBookkeeping(authentication));
		return entryService.save(entry);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Entry modify(Authentication authentication, @RequestBody @Validated(Modify.class) Entry entry, ModelMap modelMap) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entry.setBookkeeping(getBookkeeping(authentication));
		return entryService.save(entry);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(Authentication authentication, @RequestBody @Validated(Modify.class) Entry entry, ModelMap modelMap) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entry.setBookkeeping(getBookkeeping(authentication));
		entryService.delete(entry);
	}
}
