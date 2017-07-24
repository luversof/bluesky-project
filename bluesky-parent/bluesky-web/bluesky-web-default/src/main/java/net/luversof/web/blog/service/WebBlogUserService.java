package net.luversof.web.blog.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import net.luversof.blog.service.BlogUserService;
import net.luversof.security.core.userdetails.BlueskyUser;

@Service
public class WebBlogUserService implements BlogUserService {

	@Override
	public Optional<UUID> getUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlueskyUser)) {
			return Optional.empty();
		}
		return Optional.of(((BlueskyUser) authentication.getPrincipal()).getId());
	}

}
