package net.luversof.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
class UserDetailsManagerTest implements GeneralTest {

	@Autowired
	private UserDetailsManager userDetailsManager;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Test
	void createUser() {
		UserDetails user = User.builder()
				.username("user")
				.passwordEncoder(passwordEncoder::encode)
				.password("pass")
				.roles("USER")
				.build();
		userDetailsManager.createUser(user);
		assertThat(user).isNotNull();
	}
	
	@Test
	void deleteUser() {
		userDetailsManager.deleteUser("user");
	}
	
	@Test
	void encryptTest() {
		var encoder = new BCryptPasswordEncoder();
		log.debug("encode string : {}", encoder.encode("test"));
		//encoder result : $2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e
		assertThat(encoder.matches("test", "$2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e")).isTrue();
		
		log.debug("encode string : {}", passwordEncoder.encode("test"));
		//encode string : {bcrypt}$2a$10$Oc60Qx5tpBNfA6du4HMWDeBotdxoln.wTdiowKkbrHFmIo6jIxrIy
		assertThat(passwordEncoder.matches("test", "{bcrypt}$2a$10$Oc60Qx5tpBNfA6du4HMWDeBotdxoln.wTdiowKkbrHFmIo6jIxrIy")).isTrue();
	}
}
