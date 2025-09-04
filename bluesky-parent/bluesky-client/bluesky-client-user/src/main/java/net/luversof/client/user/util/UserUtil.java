package net.luversof.client.user.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import lombok.experimental.UtilityClass;
import net.luversof.client.user.domain.LoginInfo;

@UtilityClass
public class UserUtil {
	
	private static final LoginInfo NOT_LOGIN_USER = new LoginInfo();
	
	public static LoginInfo getLoginInfo() {
		var securityContext = SecurityContextHolder.getContext();
		
		var loginInfo = NOT_LOGIN_USER;
		
		if (securityContext == null) {
			return loginInfo;
		}
		
		var authentication = securityContext.getAuthentication();
		
		if (securityContext.getAuthentication() instanceof AnonymousAuthenticationToken) {
			return loginInfo;
		}
		
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
