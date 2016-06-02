package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/entryGroup")
public class EntryGroupController {

	@Autowired
	private EntryGroupService entryGroupService;
	


	@RequestMapping(method = RequestMethod.GET)
	public List<EntryGroup> getEntryGroupList(@PathVariable("bookkeeping.id") long bookkeepingId, Authentication authentication) {
		return entryGroupService.findByBookkeepingId(bookkeepingId);
	}

	@RequestMapping(method = RequestMethod.POST)
	public EntryGroup createEntryGroup(@RequestBody @Validated(EntryGroup.Create.class) EntryGroup entryGroup, @PathVariable("bookkeeping.id") long bookkeepingId, Authentication authentication) {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(bookkeepingId);
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		bookkeeping.setUserId(blueskyUser.getId());
		entryGroup.setBookkeeping(bookkeeping);
		return entryGroupService.create(entryGroup);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public EntryGroup updateEntryGroup(@RequestBody @Validated(EntryGroup.Update.class) EntryGroup entryGroup, Authentication authentication) {
		entryGroup.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return entryGroupService.update(entryGroup);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void deleteEntryGroup(@Validated(EntryGroup.Delete.class) EntryGroup entryGroup, Authentication authentication) {
		entryGroup.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		entryGroupService.delete(entryGroup);
	}

}
