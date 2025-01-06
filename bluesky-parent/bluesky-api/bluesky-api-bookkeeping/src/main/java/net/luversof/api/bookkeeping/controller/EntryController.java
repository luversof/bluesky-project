package net.luversof.api.bookkeeping.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.EntryService;

@RestController
@RequestMapping(value = "/api/entries", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {

	@Setter(onMethod_ = @Autowired)
	private EntryService entryService;
	
	@PostMapping
	public Entry createEntry(Entry entry) {
		return entryService.createEntry(entry);
	}
	
	@PutMapping
	public Entry updateEntry(Entry entry) {
		return entryService.updateEntry(entry);
	}
	
	@DeleteMapping
	public void deleteEntry(UUID id) {
		entryService.deleteEntry(id);
	}

}
