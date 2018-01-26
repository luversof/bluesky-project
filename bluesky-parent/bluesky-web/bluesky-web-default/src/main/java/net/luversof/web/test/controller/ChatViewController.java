package net.luversof.web.test.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/chat", produces = MediaType.TEXT_HTML_VALUE)
public class ChatViewController {

	@GetMapping({"", "/index"})
	public String index() {
		return "/chat/index";
	}
}
