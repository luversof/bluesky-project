package net.luversof.web.bbs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/board/{aliasName}")
public class BbsArticleViewController {

	@RequestMapping("/list")
	public void list() {
	}
}
