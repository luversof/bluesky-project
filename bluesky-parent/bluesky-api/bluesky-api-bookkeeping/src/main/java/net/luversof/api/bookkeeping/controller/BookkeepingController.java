package net.luversof.api.bookkeeping.controller;

import java.util.List;

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
	
	@GetMapping
	public List<Bookkeeping> findByUserId(String userId) {
		return bookkeepingService.findByUserId(userId);
	}
	
	@PutMapping
	public Bookkeeping update(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@DeleteMapping
	public void delete(String bookkeepingId) {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setBookkeepingId(bookkeepingId);
		bookkeepingService.delete(bookkeeping);
	}
}