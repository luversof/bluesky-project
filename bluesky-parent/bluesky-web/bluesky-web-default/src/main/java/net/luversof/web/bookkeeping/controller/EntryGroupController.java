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
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.CompositeBookkeepingService;
import net.luversof.bookkeeping.service.CompositeEntryGroupService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/entryGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryGroupController {

	@Autowired
	private CompositeEntryGroupService entryGroupService;
	
	@Autowired
	private CompositeBookkeepingService bookkeepingService;

	@PostMapping
	public EntryGroup create(@RequestBody @Validated(EntryGroup.Create.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entryGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		return entryGroupService.create(entryGroup);
	}

	@GetMapping
	public List<EntryGroup> findByBookkeepingId(BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		return entryGroupService.findByBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
	}

	@PutMapping
	public EntryGroup update(@RequestBody @Validated(EntryGroup.Update.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entryGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		return entryGroupService.update(entryGroup);
	}

	@DeleteMapping
	public void delete(@Validated(EntryGroup.Delete.class) EntryGroup entryGroup, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entryGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		entryGroupService.delete(entryGroup);
	}

}
