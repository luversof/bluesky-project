package net.luversof.api.bookkeeping.base.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.EntryType;
import net.luversof.api.bookkeeping.base.service.EntryTypeBaseService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/entryType/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryTypeBaseController {

	private final EntryTypeBaseService entryTypeBaseService;
	
	@PutMapping
	public EntryType update(@RequestBody @Validated(EntryType.Update.class) EntryType entryType) {
		return entryTypeBaseService.update(entryType);
	}

	@GetMapping("/{id}")
	public Optional<EntryType> findById(@PathVariable UUID id) {
		return entryTypeBaseService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<EntryType> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
		return entryTypeBaseService.findByBookkeepingId(bookkeepingId);
	}
	
}
