package net.luversof.client.user.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

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
		var loginInfo = getLoginInfo();
		var userInfoOptional = ApplicationContextUtil.getApplicationContext().getBean(UserInfoClient.class)
				.findByUsername(loginInfo.getUsername());
		return userInfoOptional.isPresent() ? userInfoOptional.get().id() : null;
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
