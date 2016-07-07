package net.luversof.web.index.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.bookkeeping.domain.Asset;
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
	
	@RequestMapping(value = "/test1")
	public void test1(ModelMap modelMap) {
		throw new BlueskyException("test1");
	}
	
	@RequestMapping(value = "/test2", produces = MediaType.APPLICATION_JSON_VALUE)
	public void test2(ModelMap modelMap) {
		throw new BlueskyException("test2");
	}
	
	@RequestMapping(value = "/test3", produces = MediaType.APPLICATION_XML_VALUE)
	public void test3(ModelMap modelMap) {
		throw new BlueskyException("test3");
	}
	
	@RequestMapping(value = "/test4")
	public void test4(@Validated(Asset.Create.class) Asset asset) {
		
	}
	
	@Profile({"opdev", "rc", "stage"})
	@RequestMapping("/study/**/*")
	public void study() {}
	
	/**
	 * 에러 처리를 위한 컨트롤러 (지우면 안됨)
	 */
	@RequestMapping("/error/**/*")
	public void error() {}
	
}
