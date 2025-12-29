package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository")
@Import({
		ClientUserCommonSessionConfig.class,
		CommonOAuth2SecurityConfig.class,
		ClientUserHttpExchangeConfig.class,
		CustomOAuth2UserService.class
})
@PropertySource("classpath:clientUser.properties")
public class ClientUserAutoConfiguration {

	@Bean
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
			OAuth2AuthorizedClientRepository authorizedClientRepository) {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler();
		handler.setAuthorizedClientRepository(authorizedClientRepository);
		return handler;
	}

}
