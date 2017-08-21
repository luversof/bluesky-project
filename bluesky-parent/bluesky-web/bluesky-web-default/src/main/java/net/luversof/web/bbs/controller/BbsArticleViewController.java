package net.luversof.web.bbs.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/bbs/{aliasName}", produces = MediaType.TEXT_HTML_VALUE)
public class BbsArticleViewController {

	@GetMapping("/list")
	public void list() {
	}
	
	@GetMapping("/view")
	public void view() {
	}
}
