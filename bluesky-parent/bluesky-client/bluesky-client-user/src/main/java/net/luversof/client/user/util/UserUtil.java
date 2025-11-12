package net.luversof.client.user.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import io.github.luversof.boot.context.ApplicationContextUtil;
import lombok.experimental.UtilityClass;
import net.luversof.client.user.domain.LoginInfo;
import net.luversof.client.user.openfeign.UserInfoClient;

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
				// UserInfo 조회 필요
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

		// 기존 방식 (폼 로그인 등)
		var loginInfo = getLoginInfo();
		if (loginInfo.isLogin()) {
			try {
				var userInfoClient = ApplicationContextUtil.getApplicationContext().getBean(UserInfoClient.class);
				var userInfoOptional = userInfoClient.findByUsername(loginInfo.getUsername());
				return userInfoOptional.isPresent() ? userInfoOptional.get().id() : null;
			} catch (Exception e) {
				// UserInfoClient가 없거나 조회 실패
				return null;
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

	/**
	 * 주어진 userId 목록에 대한 username 매핑을 반환합니다.
	 * 
	 * @param userIds 조회할 사용자 ID 목록
	 * @return userId -> username 매핑 Map
	 */
	public static Map<UUID, String> getUsernames(List<UUID> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}

		var userInfoClient = ApplicationContextUtil.getApplicationContext().getBean(UserInfoClient.class);
		Map<UUID, String> usernames = new HashMap<>();

		for (UUID userId : userIds) {
			try {
				var userInfoOptional = userInfoClient.findById(userId);
				if (userInfoOptional.isPresent()) {
					var userInfo = userInfoOptional.get();
					usernames.put(userId, userInfo.username() != null ? userInfo.username() : "알 수 없음");
				}
			} catch (Exception e) {
				// 개별 조회 실패 시 건너뜀
			}
		}

		return usernames;
	}
}
