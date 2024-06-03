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
import net.luversof.api.bookkeeping.base.domain.TransactionType;
import net.luversof.api.bookkeeping.base.service.TransactionTypeService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/entryType/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryTransactionTypeBaseController {

	private final TransactionTypeService entryTypeBaseService;
	
	@PutMapping
	public TransactionType update(@RequestBody @Validated(TransactionType.Update.class) TransactionType entryType) {
		return entryTypeBaseService.update(entryType);
	}

	@GetMapping("/{id}")
	public Optional<TransactionType> findById(@PathVariable UUID id) {
		return entryTypeBaseService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<TransactionType> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
		return entryTypeBaseService.findByLedgerId(bookkeepingId);
	}
	
}
