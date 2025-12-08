package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.service.registry.ImportHttpServices;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository")
@Import(CommonOAuth2SecurityConfig.class)
@ImportHttpServices(group = "client-user", types = UserInfoApiClient.class)
public class ClientUserAutoConfiguration {
	
	@Bean
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
			OAuth2AuthorizedClientRepository authorizedClientRepository, 
			UserInfoApiClient userInfoApiClient) {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler();
		handler.setAuthorizedClientRepository(authorizedClientRepository);
		handler.setUserInfoApiClient(userInfoApiClient);
		return handler;
	}

}
