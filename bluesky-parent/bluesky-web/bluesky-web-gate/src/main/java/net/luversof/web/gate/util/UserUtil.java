package net.luversof.web.gate.util;

import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.extern.slf4j.Slf4j;
import net.luversof.web.gate.config.ApplicationContextProvider;
import net.luversof.web.gate.user.openfeign.UserApiClient;

/**
 * 사용자 정보를 가져오는 유틸리티 클래스
 * api-user의 UserInfo를 조회하여 사용자 ID 반환
 */
@Slf4j
public class UserUtil {

	private UserUtil() {
		// 유틸리티 클래스는 인스턴스 생성 불가
	}

	/**
	 * 현재 인증된 사용자의 UUID를 반환
	 * 현재 인증 정보에서 provider + providerId를 추출하여 UserInfo 조회
	 * 
	 * @return 사용자 UUID, 인증되지 않은 경우 null
	 */
	public static UUID getUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}

		// JWT Token인 경우 subject에서 바로 추출
		if (authentication instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = jwtAuth.getToken();
			String sub = jwt.getSubject();
			try {
				return UUID.fromString(sub);
			} catch (IllegalArgumentException e) {
				log.warn("JWT subject is not a valid UUID: {}", sub);
				return null;
			}
		}

		// OAuth2 로그인인 경우 UserInfo 테이블 조회
		if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
			return getUserIdFromUserInfo(oauth2Auth);
		}

		return null;
	}

	private static UUID getUserIdFromUserInfo(OAuth2AuthenticationToken oauth2Auth) {
		OAuth2User principal = oauth2Auth.getPrincipal();
		String registrationId = oauth2Auth.getAuthorizedClientRegistrationId();
		String provider = normalizeProvider(registrationId);
		
		// GitHub의 id는 Integer 타입
		Object idAttr = principal.getAttribute("id");
		if (idAttr == null) {
			log.warn("id attribute is null for provider: {}", provider);
			return null;
		}
		
		String providerId = idAttr.toString();

		try {
			UserApiClient userApiClient = ApplicationContextProvider.getBean(UserApiClient.class);
			UserApiClient.UserInfoResponse userInfo = userApiClient.findUserByProvider(provider, providerId);

			if (userInfo == null || userInfo.id() == null) {
				log.warn("UserInfo not found: provider={}, providerId={}", provider, providerId);
				return null;
			}

			return UUID.fromString(userInfo.id());

		} catch (Exception e) {
			log.error("Failed to get UserInfo: provider={}, providerId={}", provider, providerId, e);
			return null;
		}
	}

	/**
	 * 현재 인증된 사용자의 username을 반환
	 * 
	 * @return username, 인증되지 않은 경우 null
	 */
	public static String getUsername() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}

		return authentication.getName();
	}

	/**
	 * 사용자가 로그인했는지 확인
	 * 
	 * @return 로그인 여부
	 */
	public static boolean isAuthenticated() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	/**
	 * Provider 이름을 정규화 (github-local → github)
	 */
	private static String normalizeProvider(String provider) {
		if (provider == null) {
			return null;
		}
		// github-local, github-dev 등을 모두 github로 통일
		if (provider.startsWith("github")) {
			return "github";
		}
		// kakao-local, kakao-dev 등을 모두 kakao로 통일
		if (provider.startsWith("kakao")) {
			return "kakao";
		}
		return provider;
	}
}
