package net.luversof.core.config;


import static org.junit.Assert.assertEquals;
import lombok.extern.slf4j.Slf4j;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.Test;

@Slf4j
public class EncryptionTest {

	@Test
	public void 암호화테스트() {
		PropertyConfig config = new PropertyConfig();
		StandardPBEStringEncryptor encryptor = config.standardPBEStringEncryptor();
		String url = "";
		String encUrl = encryptor.encrypt(url);
		log.debug("test.url={}",encUrl);
		log.debug("test.url={}",encryptor.decrypt(encUrl));
		assertEquals(url, encryptor.decrypt(encUrl));

		String username = "";
		String encUsername =encryptor.encrypt(username);
		log.debug("test.username={}", encUsername);
		log.debug("test.username={}", encryptor.decrypt(encUsername));
		assertEquals(username, encryptor.decrypt(encUsername));

		String passwordresult = "";
		String encPasswordresult = encryptor.encrypt(passwordresult );
		log.debug("test.password={}",encPasswordresult);
		log.debug("test.password={}",encryptor.decrypt(encPasswordresult));
		assertEquals(passwordresult, encryptor.decrypt(encPasswordresult));
	}

}