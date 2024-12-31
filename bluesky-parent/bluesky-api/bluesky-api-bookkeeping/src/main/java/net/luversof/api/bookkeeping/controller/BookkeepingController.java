package net.luversof.api.bookkeeping.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;

@RestController
@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	@Setter(onMethod_ = @Autowired)
	private BookkeepingService bookkeepingService;
	
	@PostMapping
	public Bookkeeping createBookkeeping(@RequestBody Bookkeeping bookkeeping) {
		return bookkeepingService.createBookkeeping(bookkeeping);
	}
	
	@DeleteMapping
	public void deleteBookkeepingByUserId(UUID userId) {
		bookkeepingService.deleteBookkeepingByUserId(userId);
	}
	
	@DeleteMapping
	public void deleteBookkeepingByBookkeepingId(UUID bookkeepingId) {
		bookkeepingService.deleteBookkeepingByBookkeepingId(bookkeepingId);
	}
	
	
}
