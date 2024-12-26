package net.luversof.api.bookkeeping.controller;

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
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/bookkeeping/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	private final BookkeepingService bookkeepingService;
	
	
	@PutMapping
	public Bookkeeping update(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@GetMapping("/{id}")
	public Optional<Bookkeeping> findById(@PathVariable UUID id) {
		return bookkeepingService.findById(id);
	}
	
	@GetMapping("/search/findByUserId/{userId}")
	public List<Bookkeeping> findByUserId(@PathVariable UUID userId) {
		return bookkeepingService.findByUserId(userId);
	}
}
