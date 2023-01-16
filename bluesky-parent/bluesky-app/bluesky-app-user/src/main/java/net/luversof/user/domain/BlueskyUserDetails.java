package net.luversof.user.domain;

import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
public class BlueskyUserDetails implements UserDetails {
	
	private static final long serialVersionUID = 1L;
	
	private String username;
	private String password;
	private Set<BlueskyGrantedAuthority> authorities;
	private boolean accountNonExpired;
	private boolean accountNonLocked;
	private boolean credentialsNonExpired;
	private boolean enabled;

}
