package net.luversof.web.index.controller;

import net.luversof.core.BlueskyException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {
	
	@Value("${datasource.blog.username}")
	private String a;

	@RequestMapping({ "/", "/index" })
	public String index() {
		System.out.println("테스트!!!!!");
		System.out.println("a : " + a);
		return "/index";
	}

	@RequestMapping("/index2")
	public void index2() {
		throw new BlueskyException("blog.menu.modify");
	}
}
