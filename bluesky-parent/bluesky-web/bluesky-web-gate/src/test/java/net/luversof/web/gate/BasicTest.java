package net.luversof.web.gate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.luversof.GeneralTest;

class BasicTest implements GeneralTest {

	private static final Logger log = LoggerFactory.getLogger(BasicTest.class);

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void encryptTest() {
		var encoder = new BCryptPasswordEncoder();
		log.debug("encode string : {}", encoder.encode("test"));
		// encoder result : $2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e
		assertThat(encoder.matches("test", "$2a$10$GEfQb7E10fOeFQo2XowAkubxab4XQGKOvO0Vf.zo6HGUPevVA2t2e")).isTrue();

		log.debug("encode string : {}", passwordEncoder.encode("test"));
		// encode string :
		// {bcrypt}$2a$10$Oc60Qx5tpBNfA6du4HMWDeBotdxoln.wTdiowKkbrHFmIo6jIxrIy
		assertThat(
				passwordEncoder.matches("test", "{bcrypt}$2a$10$Oc60Qx5tpBNfA6du4HMWDeBotdxoln.wTdiowKkbrHFmIo6jIxrIy"))
				.isTrue();
	}
}
