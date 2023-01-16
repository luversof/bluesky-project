package net.luversof.api.gate.security.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.gate.constant.GateUserErrorCode;
import net.luversof.api.gate.security.domain.BlueskyUser;
import net.luversof.api.gate.user.client.UserDetailsClient;
import net.luversof.api.gate.user.domain.User;

/**
 * oauth 인증한 유저의 정보 조회를 추가 처리하기 위해 제공
 * @author bluesky
 *
 */
@Service
public class SecurityUserService {
	
	@Autowired
	private UserDetailsClient userClient;

	public String getUserId() {
		return getUser().orElseThrow(() -> new BlueskyException(GateUserErrorCode.NOT_EXIST_USER)).userId();
	}

	public Optional<User> getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return Optional.empty();
		}
		
		if (authentication.getPrincipal() instanceof BlueskyUser blueskyUser) {
			return Optional.of(blueskyUser.getUser());
		}
		
//		if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
//			return userService.findByExternalIdAndUserType(oAuth2AuthenticationToken.getPrincipal().getName(), UserType.findByName(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()));
//		}
		return Optional.empty();
	}
}
