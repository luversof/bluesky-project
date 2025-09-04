package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.bookkeeping.openfeign.BookkeepingClient;

@DevCheckController
@RequestMapping(value = "/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingDevCheckController {
	
	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@GetMapping("/findByUserId")
	public List<Bookkeeping> findByUserId(@RequestParam String userId) {
		return bookkeepingClient.findByUserId(userId);
	}
	
}
