package net.luversof.data.jpa.config;

import lombok.extern.slf4j.Slf4j;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@Slf4j
public class CoreTest extends GeneralTest {
	@Autowired
	private Environment env;


	@Autowired
	private ApplicationContext applicationContext;


	@Value("#{systemProperties['spring.profiles.active']}")
	String properties;

	@Autowired
	private StandardPBEStringEncryptor standardPBEStringEncryptor;

	@Test
	public void 테스트() {
		log.debug("properties : {}", properties);
	}
	
	@Value("${dataSource.security.username}") String username;
	
	@Test
	public void 테스트2() {
		log.debug("username : {}", username);
	}
}
