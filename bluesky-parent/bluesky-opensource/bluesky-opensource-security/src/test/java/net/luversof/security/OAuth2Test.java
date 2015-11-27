package net.luversof.security;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.web.WebAppConfiguration;

@Slf4j
@WebAppConfiguration
public class OAuth2Test extends GeneralTest {

	@Autowired
	private OAuth2RestTemplate facebookRestTemplate;
	
	@Test
	public void test() {
		log.debug("test");
		
		Map<String, Object> me = facebookRestTemplate.getForObject("https://graph.facebook.com/me", Map.class);
		log.debug("result : ", me);
	}
}
