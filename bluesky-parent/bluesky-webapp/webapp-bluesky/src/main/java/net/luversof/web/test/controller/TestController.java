package net.luversof.web.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TestController {

	@Autowired
	private OAuth2RestTemplate oAuth2RestTemplate;
	
	@RequestMapping("/test")
	public void test() {
		Object result = oAuth2RestTemplate.getForObject("https://api.github.com/authorizations", Object.class);
		System.out.println(result);
	}
}
