package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public Bookkeeping create(@RequestBody Bookkeeping bookkeeping) {
		return bookkeepingClient.create(bookkeeping.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@GetMapping
	public List<Bookkeeping> findByUserId(@RequestParam String userId) {
		return bookkeepingClient.findByUserId(userId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public Bookkeeping update(@RequestBody Bookkeeping bookkeeping) {
		return bookkeepingClient.update(bookkeeping.toBuilder().userId(UserUtil.getUserId()).build());
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody Bookkeeping bookkeeping) {
		bookkeepingClient.delete(bookkeeping.toBuilder().userId(UserUtil.getUserId()).build());
	}
}
