package net.luversof.client.user.util;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import io.github.luversof.boot.context.ApplicationContextUtil;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.UserInfoResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserUtil {

	private static final Logger log = LoggerFactory.getLogger(UserUtil.class);

	private UserUtil() {
	}

	public static UUID getUserId() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		// OAuth2 로그인인 경우 UserInfo 테이블 조회
		if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
			OAuth2User principal = oauth2Auth.getPrincipal();
			if (principal.getAttribute("userInfo") instanceof UserInfoResponse userInfo) {
				return userInfo.id() != null ? UUID.fromString(userInfo.id()) : null;
			}
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

			var userApiClient = ApplicationContextUtil.getApplicationContext().getBean(UserInfoApiClient.class);
			UserInfoApiClient.UserInfoResponse userInfo = userApiClient.findByProviderAndProviderId(provider,
					providerId);

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

	public static String getUsername() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		// OAuth2 로그인
		if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
			OAuth2User principal = oauth2Auth.getPrincipal();
			if (principal.getAttribute("userInfo") instanceof UserInfoResponse userInfo) {
				return userInfo.username();
			}

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
