package net.luversof.web.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

//@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/entryGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryGroupController {

	@Autowired
	private EntryGroupService entryGroupService;

	@GetMapping
	public List<EntryGroup> getEntryGroupList(@PathVariable("bookkeeping.id") UUID bookkeepingId, Authentication authentication) {
		return entryGroupService.findByBookkeepingId(bookkeepingId);
	}

	@PostMapping
	public EntryGroup createEntryGroup(@RequestBody @Validated(EntryGroup.Create.class) EntryGroup entryGroup, Authentication authentication) {
		entryGroup.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return entryGroupService.create(entryGroup);
	}

	@PutMapping(value = "/{id}")
	public EntryGroup updateEntryGroup(@RequestBody @Validated(EntryGroup.Update.class) EntryGroup entryGroup, Authentication authentication) {
		entryGroup.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return entryGroupService.update(entryGroup);
	}

	@DeleteMapping(value = "/{id}")
	public void deleteEntryGroup(@Validated(EntryGroup.Delete.class) EntryGroup entryGroup, Authentication authentication) {
		entryGroup.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		entryGroupService.delete(entryGroup);
	}

}
