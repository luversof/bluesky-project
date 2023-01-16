package net.luversof.user.domain;

import org.springframework.security.core.GrantedAuthority;

import lombok.Data;

@Data
public class BlueskyGrantedAuthority implements GrantedAuthority {

	private static final long serialVersionUID = 1L;

	private String authority;

}
