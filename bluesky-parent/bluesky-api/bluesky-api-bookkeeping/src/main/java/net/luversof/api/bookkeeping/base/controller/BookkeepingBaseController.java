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
import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
import net.luversof.api.bookkeeping.base.service.BookkeepingBaseService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/bookkeeping/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingBaseController {

	private final BookkeepingBaseService bookkeepingBaseService;
	
	
	@PutMapping
	public Bookkeeping update(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingBaseService.update(bookkeeping);
	}
	
	@GetMapping("/{id}")
	public Optional<Bookkeeping> findById(@PathVariable UUID id) {
		return bookkeepingBaseService.findById(id);
	}
	
	@GetMapping("/search/findByUserId/{userId}")
	public List<Bookkeeping> findByUserId(@PathVariable UUID userId) {
		return bookkeepingBaseService.findByUserId(userId);
	}
}
