package org.springframework.security.core.userdetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.luversof.security.domain.User;
import net.luversof.security.domain.UserAuthority;
import net.luversof.security.service.UserService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class LuversofUserDetailsService implements UserDetailsService {

	private UserService userService;
	
	public LuversofUserDetailsService(UserService userService) {
		this.userService = userService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userService.findByUsername(username);
		
		List<UserAuthority> userAuthorityList = user.getUserAuthorityList();
		
		Set<GrantedAuthority> authoritySet = new HashSet<GrantedAuthority>();
		for (UserAuthority userAuthority : userAuthorityList) {
			authoritySet.add(new SimpleGrantedAuthority(userAuthority.getAuthority()));
		}
		return new LuversofUser(user.getId(), user.getUsername(), user.getPassword(), authoritySet, true, true, true, user.isEnable());
	}

}