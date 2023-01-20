package net.luversof.api.user.security.oauth2.client.config;

import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;


@Configuration
public class UserSecurityOauth2ClientConfig {
	
	@Bean
	OAuth2ClientJackson2Module oAuth2ClientJackson2Module(ObjectMapperProvider objectMapperProvider) {
		return new OAuth2ClientJackson2Module();
	}

    @Bean
    JdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
    }

}