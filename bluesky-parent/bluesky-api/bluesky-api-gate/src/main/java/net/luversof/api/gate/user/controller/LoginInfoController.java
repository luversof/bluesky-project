package net.luversof.api.gate.user.controller;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.gate.user.domain.LoginInfo;

@RestController
@RequestMapping(value= "/api/user/loginInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginInfoController {
	
	
	private LoginInfo nonLoginUser = new LoginInfo();

	@GetMapping
	public LoginInfo loginInfo() {
		var securityContext = SecurityContextHolder.getContext();
		
		if (securityContext == null) {
			return nonLoginUser;
		}
		
		var authentication = securityContext.getAuthentication();
		
		if (securityContext.getAuthentication() instanceof AnonymousAuthenticationToken) {
			return nonLoginUser;
		}
		
		LoginInfo loginInfo = null;
		
		if (authentication instanceof UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
			loginInfo = new LoginInfo(usernamePasswordAuthenticationToken);
		}
		
		if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
			loginInfo = new LoginInfo(oAuth2AuthenticationToken);
		}
		
		return loginInfo;
	}
	
	
}
