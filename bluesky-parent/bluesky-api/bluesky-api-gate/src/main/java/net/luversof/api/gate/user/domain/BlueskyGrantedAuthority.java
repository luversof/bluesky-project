package net.luversof.api.gate.user.domain;

import org.springframework.security.core.GrantedAuthority;

public record BlueskyGrantedAuthority(String authority) implements GrantedAuthority {

	@Override
	public String getAuthority() {
		return authority();
	}

}
