package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import net.luversof.client.user.openfeign.UserApiClient;

@AutoConfiguration(after = FeignAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository")
@EnableConfigurationProperties(ClientUserFeignProperties.class)
@Import(CommonOAuth2SecurityConfig.class)
public class ClientUserAutoConfiguration {

	@Bean
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
			OAuth2AuthorizedClientRepository authorizedClientRepository,
			UserApiClient userApiClient) {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler();
		handler.setAuthorizedClientRepository(authorizedClientRepository);
		handler.setUserApiClient(userApiClient);
		return handler;
	}

}
