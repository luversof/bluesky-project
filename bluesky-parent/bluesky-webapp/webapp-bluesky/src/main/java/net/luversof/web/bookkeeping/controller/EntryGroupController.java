package net.luversof.web.bookkeeping.controller;

import static net.luversof.core.Constants.JSON_MODEL_KEY;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.domain.EntryGroup.Add;
import net.luversof.bookkeeping.domain.EntryGroup.Modify;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;
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
@RequestMapping("/bookkeeping/entryGroup")
public class EntryGroupController {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private EntryGroupService entryGroupService;
	

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
	public void getEntryGroup(Authentication authentication, ModelMap modelMap) {
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		modelMap.addAttribute(JSON_MODEL_KEY , entryGroupService.findByBookkeepingId(bookkeeping.getId()));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public EntryGroup addEntryGroup(Authentication authentication, @RequestBody @Validated(Add.class) EntryGroup entryGroup) {
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		return entryGroupService.save(entryGroup);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public EntryGroup modifyEntryGroup(Authentication authentication, @RequestBody @Validated(Modify.class) EntryGroup entryGroup) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		return entryGroupService.save(entryGroup);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(Authentication authentication, @RequestBody @Validated(Modify.class) EntryGroup entryGroup) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		entryGroupService.delete(entryGroup);
	}

}
