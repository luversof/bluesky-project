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
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setConfig(config.environmentStringPBEConfig());
		String url = "jdbc:oracle:thin:@127.0.0.1:1521:XE";
		String encUrl = encryptor.encrypt(url);
		log.debug("test.url={}",encUrl);
		log.debug("test.url={}",encryptor.decrypt(encUrl));
		assertEquals(url, encryptor.decrypt(encUrl));

		String username = "root";
		String encUsername =encryptor.encrypt(username);
		log.debug("test.username={}", encUsername);
		log.debug("test.username={}", encryptor.decrypt(encUsername));
		assertEquals(username, encryptor.decrypt(encUsername));

		String passwordresult = "1234";
		String encPasswordresult = encryptor.encrypt(passwordresult );
		log.debug("test.password={}",encPasswordresult);
		log.debug("test.password={}",encryptor.decrypt(encPasswordresult));
		assertEquals(passwordresult, encryptor.decrypt(encPasswordresult));
	}

}