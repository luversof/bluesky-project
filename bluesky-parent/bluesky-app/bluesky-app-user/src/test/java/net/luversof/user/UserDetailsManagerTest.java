package net.luversof.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;

import net.luversof.GeneralTest;

public class UserDetailsManagerTest extends GeneralTest {

	@Autowired
	private UserDetailsManager userDetailsManager;
	
	@Test
	void createUser() {
		UserDetails user = User.builder()
				.username("user")
				.password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
				.roles("USER")
				.build();
		userDetailsManager.createUser(user);
	}
}
