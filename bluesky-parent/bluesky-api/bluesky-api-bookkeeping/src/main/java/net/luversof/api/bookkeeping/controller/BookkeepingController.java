package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

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

import io.swagger.v3.oas.annotations.Operation;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.CompositeBookkeepingService;

@RestController
@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	@Autowired
	private CompositeBookkeepingService bookkeepingService;

	@PostMapping
	public Bookkeeping create(@RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping) {
		return bookkeepingService.create(bookkeeping);
	}
	
	@Operation(description = "해당 userId의 bookkeeping 목록 조회")
	@GetMapping
	public List<Bookkeeping> findByUserId(UUID userId) {
		return bookkeepingService.findByUserId(userId);
	}
	
	@PutMapping
	public Bookkeeping update(@Validated(Bookkeeping.Update.class) @RequestBody Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@DeleteMapping
	public void delete(@Validated(Bookkeeping.Delete.class) @RequestBody Bookkeeping bookkeeping) {
		bookkeepingService.delete(bookkeeping);
	}
}