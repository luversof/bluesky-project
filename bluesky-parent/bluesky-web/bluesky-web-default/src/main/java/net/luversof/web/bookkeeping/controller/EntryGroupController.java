package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/entryGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryGroupController {

	@Autowired
	private EntryGroupService entryGroupService;


	@PostMapping
	public EntryGroup createUserEntryGroup(@RequestBody @Validated(EntryGroup.Create.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		entryGroup.setBookkeeping(new Bookkeeping());
		entryGroup.getBookkeeping().setUserId(blueskyUser.getId());
		return entryGroupService.createUserEntryGroup(entryGroup);
	}

	@GetMapping
	public List<EntryGroup> getUserEntryGroupList(BlueskyUser blueskyUser) {
		return entryGroupService.getUserEntryGroupList(blueskyUser.getId());
	}

	@PutMapping
	public EntryGroup updateUserEntryGroup(@RequestBody @Validated(EntryGroup.Update.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		entryGroup.setBookkeeping(new Bookkeeping());
		entryGroup.getBookkeeping().setUserId(blueskyUser.getId());
		return entryGroupService.updateUserEntryGroup(entryGroup);
	}

	@DeleteMapping
	public void deleteUserEntryGroup(@Validated(EntryGroup.Delete.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		entryGroup.setBookkeeping(new Bookkeeping());
		entryGroup.getBookkeeping().setUserId(blueskyUser.getId());
		entryGroupService.deleteUserEntryGroup(entryGroup);
	}

}
