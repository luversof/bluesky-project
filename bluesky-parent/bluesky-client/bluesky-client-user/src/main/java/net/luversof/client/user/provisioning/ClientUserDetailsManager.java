package net.luversof.client.user.provisioning;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.client.user.constant.ClientUserErrorCode;
import net.luversof.client.user.openfeign.client.UserDetailsClient;

public class ClientUserDetailsManager implements UserDetailsManager {

	private UserDetailsClient userDetailsClient;
	
	public ClientUserDetailsManager(UserDetailsClient userDetailsClient) {
		this.userDetailsClient = userDetailsClient;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userDetailsClient.loadUserByUsername(username).orElseThrow(() -> new BlueskyException(ClientUserErrorCode.NOT_EXIST_USER));
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
