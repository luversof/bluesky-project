package net.luversof.web.gate.json.bookkeeping.controller;

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

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import net.luversof.web.gate.feign.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.feign.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.user.util.UserUtil;

@RestController
@RequestMapping(value = "/json/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingJsonController {

	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@BlueskyPreAuthorize
	@PostMapping
	public Bookkeeping create(@RequestBody Bookkeeping bookkeeping) {
		bookkeeping.setUserId(UserUtil.getUserId());
		return bookkeepingClient.create(bookkeeping);
	}
	
	@GetMapping
	public List<Bookkeeping> findByUserId(@RequestParam String userId) {
		return bookkeepingClient.findByUserId(userId);
	}
	
	@BlueskyPreAuthorize
	@PutMapping
	public Bookkeeping update(@RequestBody Bookkeeping bookkeeping) {
		bookkeeping.setUserId(UserUtil.getUserId());
		return bookkeepingClient.update(bookkeeping);
	}
	
	@BlueskyPreAuthorize
	@DeleteMapping
	public void delete(@RequestBody Bookkeeping bookkeeping) {
		bookkeeping.setUserId(UserUtil.getUserId());
		bookkeepingClient.delete(bookkeeping);
	}
}
