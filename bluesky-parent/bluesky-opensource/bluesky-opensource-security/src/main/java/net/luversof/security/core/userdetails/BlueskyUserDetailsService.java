package net.luversof.security.core.userdetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import net.luversof.user.domain.User;
import net.luversof.user.service.UserService;

@Service
public class BlueskyUserDetailsService implements UserDetailsService {

	@Autowired
	private UserService userService;

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userService.findByUsername(username);
		return new BlueskyUser(user);
	}

}
