package net.luversof.web.gate.user.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import lombok.experimental.UtilityClass;
import net.luversof.web.gate.user.domain.LoginInfo;

@UtilityClass
public class UserUtil {
	
	private static LoginInfo NOT_LOGIN_USER = new LoginInfo();
	
	public static LoginInfo getLoginInfo() {
		var securityContext = SecurityContextHolder.getContext();
		
		if (securityContext == null) {
			return NOT_LOGIN_USER;
		}
		
		var authentication = securityContext.getAuthentication();
		
		if (securityContext.getAuthentication() instanceof AnonymousAuthenticationToken) {
			return NOT_LOGIN_USER;
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

	
	public static String getUserId() {
		return getLoginInfo().getUsername();
	}
}
