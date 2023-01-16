package net.luversof.api.gate.security.provisioning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.gate.constant.GateUserErrorCode;
import net.luversof.api.gate.security.domain.BlueskyUser;
import net.luversof.api.gate.user.client.UserClient;

public class GateUserDetailsManager implements UserDetailsManager {
	

	@Autowired
	private UserClient userClient;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var user = userClient.findByUserId(username).orElseThrow(() -> new BlueskyException(GateUserErrorCode.NOT_EXIST_USER));
		return new BlueskyUser(user);
	}

	@Override
	public void createUser(UserDetails user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateUser(UserDetails user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteUser(String username) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changePassword(String oldPassword, String newPassword) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean userExists(String username) {
		// TODO Auto-generated method stub
		return false;
	}

}
