package net.luversof.api.bookkeeping.composite.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.CompositeService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/bookkeeping/composite", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingCompositeController {
	
	private final CompositeService bookkeepingCompositeService;
	

	@PostMapping
	public Bookkeeping create(@RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping) {
		bookkeepingCompositeService.initDataSetup(bookkeeping);
		return bookkeeping;
	}
	

}
