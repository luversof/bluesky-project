package net.luversof.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
public class UserDetailsManagerTest extends GeneralTest {

	@Autowired
	private UserDetailsManager userDetailsManager;
	
	@Test
	void createUser() {
		UserDetails user = User.builder()
				.username("user")
				.password("{bcrypt}$2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e")
				.roles("USER")
				.build();
		userDetailsManager.createUser(user);
	}
	
	@Test
	void encryptTest() {
		var encoder = new BCryptPasswordEncoder();
		log.debug("encode string : {}", encoder.encode("test"));
		//encoder result : $2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e
	}
}
