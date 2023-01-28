package net.luversof.api.gate.user.domain;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import lombok.Data;

/**
 * 로컬 유저는 username이 구분 키
 * oauth2 유저는 authorizedClientRegistrationId + principal_name 이 구분 키
 * @author bluesky
 *
 */
@Data
public class LoginInfo {
	
	public LoginInfo() {
		isLogin = false;
	}
	
	public LoginInfo(UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
		isLogin = true;
		username = usernamePasswordAuthenticationToken.getName();
		authorities = usernamePasswordAuthenticationToken.getAuthorities();
	}
	
	public LoginInfo(OAuth2AuthenticationToken oAuth2AuthenticationToken) {
		isLogin = true;
		authorizedClientRegistrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
		authorities = oAuth2AuthenticationToken.getAuthorities();
		principalName = oAuth2AuthenticationToken.getName();
		// username은 authorizedClientRegistrationId + ":" + principalName으로 하려고 함

		
		username = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId().replace("-local", "") + ":" + oAuth2AuthenticationToken.getName();
	}
	
	private final boolean isLogin;
	
	private String username;
	
	private Collection<GrantedAuthority> authorities;
	
	/**
	 * oAuth2 인증을 한 경우 인증 구분 키
	 * 개발을 위해 github, github_local이 있음
	 */
	private String authorizedClientRegistrationId;
	
	/**
	 * oAuth2 인증을 한 경우 username
	 */
	private String principalName;
	

}
