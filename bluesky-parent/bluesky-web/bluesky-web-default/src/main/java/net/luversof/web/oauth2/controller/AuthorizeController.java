package net.luversof.web.oauth2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthorizeController {

	@Autowired
	private OAuth2RestTemplate facebookRestTemplate;
	
//	@Autowired
//	private AuthorizationCodeServices authorizationCodeServices;
	
	@RequestMapping("/test")
	public void test(Authentication authentiation) {
		System.out.println("테스트 :!!!!!!");
		
		OAuth2AccessToken a = facebookRestTemplate.getAccessToken();
		
		System.out.println(a);
	}
}
