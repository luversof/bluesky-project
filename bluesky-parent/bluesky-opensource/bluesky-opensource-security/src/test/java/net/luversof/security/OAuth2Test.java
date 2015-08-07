package net.luversof.security;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@Slf4j
public class OAuth2Test extends GeneralTest {

	@Autowired
	private OAuth2RestTemplate oAuth2RestTemplate;
	@Test
	public void test() {
		log.debug("test");
		
		OAuth2AccessToken result = oAuth2RestTemplate.getAccessToken();
		log.debug("result : ", result);
	}
}
