package net.luversof.web.index.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
	
	@GetMapping("/index2")
	public void index2(ModelMap modelMap) {
		List<String> testList = new ArrayList<>();
		testList.add("StrA");
		testList.add("StrB");
		testList.add("StrC");
		
		
		List<User> users = new ArrayList<>();
		users.add(new User("user1", "type1"));
		users.add(new User("user2", "type2"));
		users.add(new User("user3", "type3"));
		users.add(new User("user4", "type4"));
	
		modelMap.addAttribute("testList", testList);
		modelMap.addAttribute("users", users);
	}
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class User {
		private String name;
		private String type;
	}
	
	@GetMapping("login")
	public void login() {}

	@GetMapping("/index3")
	public void index3() {
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
