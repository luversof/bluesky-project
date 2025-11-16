package net.luversof.client.user.util;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserUtil {

	public static UUID getUserId() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		// JWT Token에서 추출 (Resource Server에서 사용)
		if (authentication instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = jwtAuth.getToken();
			String sub = jwt.getSubject();
			try {
				return UUID.fromString(sub);
			} catch (IllegalArgumentException e) {
				// sub가 UUID가 아닌 경우 (username인 경우)
				return null;
			}
		}

		// OAuth2 로그인 (Client에서 사용)
		if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
			OAuth2User principal = oauth2Auth.getPrincipal();
			String sub = principal.getAttribute("sub");
			if (sub != null) {
				try {
					return UUID.fromString(sub);
				} catch (IllegalArgumentException e) {
					// sub가 UUID가 아닌 경우
				}
			}
		}

		return null;
	}

	public static String getUsername() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		// JWT Token에서 추출
		if (authentication instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = jwtAuth.getToken();
			String username = jwt.getClaim("preferred_username");
			if (username == null) {
				username = jwt.getClaim("username");
			}
			if (username == null) {
				username = jwt.getSubject();
			}
			return username;
		}

		// OAuth2 로그인
		if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
			OAuth2User principal = oauth2Auth.getPrincipal();
			String username = principal.getAttribute("preferred_username");
			if (username == null) {
				username = principal.getAttribute("name");
			}
			return username;
		}

		// 기존 방식
		return authentication.getName();
	}
}
