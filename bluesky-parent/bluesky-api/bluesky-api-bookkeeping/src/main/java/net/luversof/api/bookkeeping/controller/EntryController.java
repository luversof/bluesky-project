package net.luversof.api.bookkeeping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.EntryService;

@RestController
@RequestMapping(value = "/api/entries", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {

	@Autowired
	private EntryService entryService;

	@PostMapping
	public Entry createEntry(@RequestBody Entry entry) {
		return entryService.createEntry(entry);
	}

	@PutMapping
	public Entry updateEntry(@RequestBody Entry entry) {
		return entryService.updateEntry(entry);
	}

	/**
	 * 이거 요청이 많다면 putMapping의 requestBody를 고려해야 할 수도 있음
	 * 
	 * @param entry
	 */
	@DeleteMapping
	public void deleteEntry(Entry entry) {
		entryService.deleteEntry(entry);
	}

}
