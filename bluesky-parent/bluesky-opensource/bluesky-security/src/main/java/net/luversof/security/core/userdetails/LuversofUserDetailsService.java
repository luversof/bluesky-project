package net.luversof.security.core.userdetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.luversof.user.domain.User;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class LuversofUserDetailsService implements UserDetailsService {

	@Autowired
	private UserService userService;

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userService.findByUsername(username);
		
		List<UserAuthority> userAuthorityList = user.getUserAuthorityList();
		
		Set<GrantedAuthority> authoritySet = new HashSet<GrantedAuthority>();
		for (UserAuthority userAuthority : userAuthorityList) {
			authoritySet.add(new SimpleGrantedAuthority(userAuthority.getAuthority()));
		}
		return new BlueskyUser(user.getUserId(), user.getUsername(), user.getPassword(), authoritySet, true, true, true, user.isEnable(), UserType.LOCAL);
	}

}
