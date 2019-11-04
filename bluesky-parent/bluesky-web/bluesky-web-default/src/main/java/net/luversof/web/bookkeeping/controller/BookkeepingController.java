package net.luversof.web.bookkeeping.controller;

import java.util.Optional;

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

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	@PostMapping
	public Bookkeeping createUserBookkeeping(@RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping, BlueskyUser blueskyUser) {
		bookkeeping.setUserId(blueskyUser.getId());
		return bookkeepingService.createUserBookkeeping(bookkeeping);
	}
	
	/**
	 * 로그인한 유저의 bookkeeping 리스트 반환
	 * @param authentication
	 * @return
	 */
	@GetMapping
	public Optional<Bookkeeping> getUserBookkeepingList(BlueskyUser blueskyUser) {
		return bookkeepingService.getUserBookkeeping(blueskyUser.getId());
	}
	
	@PutMapping
	public Bookkeeping updateUserBookkeeping(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping, BlueskyUser blueskyUser) {
		bookkeeping.setUserId(blueskyUser.getId());
		return bookkeepingService.updateUserBookkeeping(bookkeeping);
	}
	
	@DeleteMapping
	public void deleteUserBookkeeping(BlueskyUser blueskyUser) {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(blueskyUser.getId());
		bookkeepingService.deleteUserBookkeeping(bookkeeping);
	}
}