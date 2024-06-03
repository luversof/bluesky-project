package net.luversof.api.bookkeeping.composite.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Ledger;
import net.luversof.api.bookkeeping.composite.service.BookkeepingCompositeService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/bookkeeping/composite", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingCompositeController {
	
	private final BookkeepingCompositeService bookkeepingCompositeService;
	

	@PostMapping
	public Ledger create(@RequestBody @Validated(Ledger.Create.class) Ledger bookkeeping) {
		bookkeepingCompositeService.create(bookkeeping);
		return bookkeeping;
	}
	

}
