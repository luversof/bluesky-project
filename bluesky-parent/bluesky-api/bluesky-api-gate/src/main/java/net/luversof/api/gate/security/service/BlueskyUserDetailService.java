package net.luversof.api.gate.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.gate.constant.GateUserErrorCode;
import net.luversof.api.gate.security.domain.BlueskyUser;
import net.luversof.api.gate.user.client.UserClient;

@Service
public class BlueskyUserDetailService implements UserDetailsService {

	@Autowired
	private UserClient userClient;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = userClient.findByUserId(username).orElseThrow(() -> new BlueskyException(GateUserErrorCode.NOT_EXIST_USER));
		return new BlueskyUser(user);
	}
	
}
