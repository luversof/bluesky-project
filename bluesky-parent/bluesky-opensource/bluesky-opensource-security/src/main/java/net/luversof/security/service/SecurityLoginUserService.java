package net.luversof.security.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import net.luversof.boot.exception.BlueskyException;
import net.luversof.core.exception.CoreErrorCode;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.user.domain.User;
import net.luversof.user.domain.UserType;
import net.luversof.user.service.LoginUserService;
import net.luversof.user.service.UserService;

@Service
public class SecurityLoginUserService implements LoginUserService {
	
	@Autowired
	private UserService userService;

	@Override
	public UUID getUserId() {
		return getUser().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID)).getId();
	}

	@Override
	public Optional<User> getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return Optional.empty();
		}
		if (authentication.getPrincipal() instanceof BlueskyUser) {
			return Optional.of(((BlueskyUser) authentication.getPrincipal()).getUser());
		}
		
		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
			return userService.findByExternalIdAndUserType(oAuth2AuthenticationToken.getPrincipal().getName(), UserType.findByName(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()));
		}
		return Optional.empty();
	}
}
