package net.luversof.api.gate.security.provisioning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.gate.constant.GateUserErrorCode;
import net.luversof.api.gate.user.client.UserDetailsClient;

@Component
public class GateUserDetailsManager implements UserDetailsManager {
	

	@Autowired
	private UserDetailsClient userDetailsClient;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userDetailsClient.loadUserByUsername(username).orElseThrow(() -> new BlueskyException(GateUserErrorCode.NOT_EXIST_USER));
	}

	@Override
	public void createUser(UserDetails user) {
		userDetailsClient.createUser((User) user);
	}

	@Override
	public void updateUser(UserDetails user) {
		userDetailsClient.updateUser((User) user);
	}

	@Override
	public void deleteUser(String username) {
		userDetailsClient.deleteUser(username);
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
