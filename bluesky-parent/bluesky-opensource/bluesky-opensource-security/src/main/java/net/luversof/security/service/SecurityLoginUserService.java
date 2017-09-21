package net.luversof.security.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.CoreErrorCode;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

@Service
public class SecurityLoginUserService implements LoginUserService {

	@Override
	public UUID getUserId() {
		return getUser().orElseThrow(() -> new BlueskyException(CoreErrorCode.NOT_EXIST_USER_ID)).getId();
	}

	@Override
	public Optional<User> getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlueskyUser)) {
			return Optional.empty();
		}
		return Optional.of(((BlueskyUser) authentication.getPrincipal()).getUser());
	}

}
