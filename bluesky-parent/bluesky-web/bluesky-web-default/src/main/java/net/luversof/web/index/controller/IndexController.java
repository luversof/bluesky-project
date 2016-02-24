package net.luversof.web.index.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.core.exception.BlueskyException;

@Controller
public class IndexController {
	
	@Value("${datasource.blog.username}")
	private String a;

	@RequestMapping({ "/", "/index" })
	public String index(@RequestHeader(value = "User-Agent") String userAgent) {
		System.out.println("테스트!!!!!");
		System.out.println("a : " + a);
		System.out.println(userAgent);
		return "index";
	}
	
	@RequestMapping("login")
	public void login() {}

	@RequestMapping("/index2")
	public void index2() {
		throw new BlueskyException("blog.menu.modify");
	}
	
	@RequestMapping("/test")
	public void test(ModelMap modelMap) {
		modelMap.addAttribute("test!!");
	}
	
	@Profile({"opdev", "rc", "stage"})
	@RequestMapping("/study/**/*")
	public void study() {}
	
}
