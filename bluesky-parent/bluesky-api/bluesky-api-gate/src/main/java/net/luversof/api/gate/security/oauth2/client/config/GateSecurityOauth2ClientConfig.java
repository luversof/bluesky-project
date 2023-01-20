package net.luversof.api.gate.security.oauth2.client.config;

import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;

@Configuration
public class GateSecurityOauth2ClientConfig {
	
	@Bean
	OAuth2ClientJackson2Module oAuth2ClientJackson2Module(ObjectMapperProvider objectMapperProvider) {
		return new OAuth2ClientJackson2Module();
	}

}
