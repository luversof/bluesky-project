package org.springframework.security.core.userdetails;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

@Data
@AllArgsConstructor
public class LuversofUser implements UserDetails, CredentialsContainer {
	
	private static final long serialVersionUID = -7218355940538132953L;
	
	private final long id;
	private final String username;
	private String password;
	private final Set<GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    
	@Override
	public void eraseCredentials() {
		this.password = null;
	}
}
