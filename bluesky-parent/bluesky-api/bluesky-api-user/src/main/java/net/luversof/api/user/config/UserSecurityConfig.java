package net.luversof.api.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;


@Configuration
public class UserSecurityConfig {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostConstruct
	public void postConstruct() {
		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
	}

    @Bean
    JdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
    }

}