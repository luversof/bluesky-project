package net.luversof.web.gate.json.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.bookkeeping.openfeign.BookkeepingClient;

@DevCheckController
public class BookkeepingDevCheckController {
	
	private final String pathPrefix = "/bookkeeping";

	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@GetMapping(pathPrefix + "/findByUserId")
	public List<Bookkeeping> findByUserId(@RequestParam String userId) {
		return bookkeepingClient.findByUserId(userId);
	}
	
}
