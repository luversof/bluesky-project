package net.luversof.web.index.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.core.exception.BlueskyException;

@Controller
public class IndexController {
	
	@Value("${datasource.blog.username}")
	private String a;

	@GetMapping({ "/", "/index" })
	public String index(@RequestHeader(value = "User-Agent") String userAgent) {
		System.out.println("테스트!!");
		System.out.println("a : " + a);
		System.out.println(userAgent);
		return "index";
	}
	
	@GetMapping("login")
	public void login() {}

	@GetMapping("/index2")
	public void index2() {
		throw new BlueskyException("blog.menu.modify");
	}
	
	@GetMapping(value = "/test1")
	public void test1(ModelMap modelMap) {
		throw new BlueskyException("test1");
	}
	
	@GetMapping(value = "/test2")
	public Asset test2(@RequestBody @Validated(Asset.Create.class) Asset asset) {
		return asset;
	}
	
	@GetMapping(value = "/test3")
	public Asset test3(@Validated(Asset.Create.class) Asset asset) {
		return asset;
	}
	
	@GetMapping(value = "/test4")
	public Asset test4(@ModelAttribute @Validated(Asset.Create.class) Asset asset) {
		return asset;
	}
	
	@GetMapping("/study/**/*")
	public void study() {}
	
	/**
	 * 에러 처리를 위한 컨트롤러 (지우면 안됨)
	 */
	@GetMapping("/error/**/*")
	public void error() {}
	
}
