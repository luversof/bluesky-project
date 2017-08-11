package net.luversof.security.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.user.domain.User;
import net.luversof.user.service.LoginUserService;

@Service
public class SecurityLoginUserService implements LoginUserService {

	@Override
	public Optional<UUID> getUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlueskyUser)) {
			return Optional.empty();
		}
		return Optional.of(((BlueskyUser) authentication.getPrincipal()).getId());
	}

	@Override
	public Optional<User> getUser() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

}
