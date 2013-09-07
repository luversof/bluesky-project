package net.luversof.web.config.security;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class SecurityTest {

	@Test
	public void test() {
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		log.debug("passwordEncoder : {}", passwordEncoder.encode("----"));

	}
}