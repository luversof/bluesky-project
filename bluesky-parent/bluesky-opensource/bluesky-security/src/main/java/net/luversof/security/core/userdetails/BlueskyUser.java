package net.luversof.security.core.userdetails;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@AllArgsConstructor
public class BlueskyUser implements UserDetails, CredentialsContainer {
	
	private static final long serialVersionUID = -7218355940538132953L;
	
	private final long id;
	private final String username;
	private String password;
	private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final UserType userType;
    
	@Override
	public void eraseCredentials() {
		this.password = null;
	}
}
