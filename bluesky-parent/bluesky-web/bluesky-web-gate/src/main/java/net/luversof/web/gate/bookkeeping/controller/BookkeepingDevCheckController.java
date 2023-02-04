package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.devcheck.core.annotation.DevCheckController;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;

@DevCheckController
@RestController
@RequestMapping(value = "${bluesky-boot.dev-check.path-prefix}/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingDevCheckController {

	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@GetMapping
	public List<Bookkeeping> findByUserId(@RequestParam String userId) {
		return bookkeepingClient.findByUserId(userId);
	}
	
}
