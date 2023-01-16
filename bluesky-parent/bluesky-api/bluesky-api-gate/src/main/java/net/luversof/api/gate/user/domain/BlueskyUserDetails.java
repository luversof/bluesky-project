package net.luversof.api.gate.user.domain;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record BlueskyUserDetails(String username, String password, Set<BlueskyGrantedAuthority> authorities, boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities();
	}

	@Override
	public String getPassword() {
		return password();
	}

	@Override
	public String getUsername() {
		return username();
	}

	@Override
	public boolean isAccountNonExpired() {
		return accountNonExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return enabled();
	}

}
