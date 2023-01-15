package net.luversof.api.gate.security.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;
import net.luversof.api.gate.user.domain.User;
import net.luversof.api.gate.user.domain.UserAuthority;
import net.luversof.api.gate.user.domain.UserType;

/**
 * Spring Security UserDetails 구현 객체
 * oauth 인증 유저 정보도 같이 저장하기 위해 별도 구현
 * @author bluesky
 *
 */
@Data
//@AllArgsConstructor
public class BlueskyUser implements UserDetails, CredentialsContainer {
	
	private static final long serialVersionUID = 1L;
	
	private final String id;
	private final String username;
	private String password;
	private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final UserType userType;
    private final String externalId;	// oauth 인증으로 넘어온 경우의 externalId 정보
    
    private final User user;
    
    public BlueskyUser(User user) {
    	this.user = user;
    	this.id = user.userId();
    	this.username = user.userName();
    	this.password = user.password();
    	
    	List<String> authorityList = new ArrayList<>();
    	if (user.userAuthorityList() != null) {
    		for (UserAuthority userAuthority : user.userAuthorityList()) {
    			authorityList.add(userAuthority.authority());
    		}
    	}
    	this.authorities =  AuthorityUtils.createAuthorityList(authorityList.stream().toArray(String[]::new));
    	this.accountNonExpired = user.accountNonExpired();
    	this.accountNonLocked = user.accountNonLocked();
    	this.credentialsNonExpired = user.credentialsNonExpired();
    	this.enabled = user.enabled();
    	this.userType = user.userType();
    	this.externalId = user.externalId();
    }
    
	@Override
	public void eraseCredentials() {
		this.password = null;
	}
}
