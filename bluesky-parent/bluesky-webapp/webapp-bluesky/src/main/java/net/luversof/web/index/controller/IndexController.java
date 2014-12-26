package net.luversof.web.index.controller;

import net.luversof.core.BlueskyException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

	@RequestMapping("/index")
	public void index() {}
	
	
	@RequestMapping("/index2")
	public void index2() {
		throw new BlueskyException("blog.menu.modify");
	}
}
