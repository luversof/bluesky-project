package net.luversof.web.gate.bookkeeping.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingViewController {
	
	@GetMapping({ "/", "/index" })
	public String index() {
		return "bookkeeping/index";
	}
}
