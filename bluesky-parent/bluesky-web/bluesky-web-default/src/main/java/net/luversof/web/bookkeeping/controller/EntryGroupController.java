package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.domain.EntryGroup.Add;
import net.luversof.bookkeeping.domain.EntryGroup.Modify;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "bookkeeping/{bookkeeping.id}/entryGroup")
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
		return bookkeepingService.findByUserId(((BlueskyUser) authentication.getPrincipal()).getId()).get(0);
	}

	@RequestMapping(method = RequestMethod.GET)
	public List<EntryGroup> getEntryGroupList(Authentication authentication) {
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		return entryGroupService.findByBookkeepingId(bookkeeping.getId());
	}

	@RequestMapping(method = RequestMethod.POST)
	public EntryGroup addEntryGroup(Authentication authentication, @RequestBody @Validated(Add.class) EntryGroup entryGroup) {
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		return entryGroupService.save(entryGroup);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public EntryGroup modifyEntryGroup(Authentication authentication, @RequestBody /*@Validated(Modify.class)*/ EntryGroup entryGroup) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		return entryGroupService.save(entryGroup);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void delete(Authentication authentication, @RequestBody @Validated(Modify.class) EntryGroup entryGroup) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		entryGroup.setBookkeeping(getBookkeeping(authentication));
		entryGroupService.delete(entryGroup);
	}

}
