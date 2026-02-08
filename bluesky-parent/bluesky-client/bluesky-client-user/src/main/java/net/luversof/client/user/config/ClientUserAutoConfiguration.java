package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.filter.ForwardedHeaderFilter;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository")
@Import({
		ClientUserHttpExchangeConfig.class,
		CustomOAuth2UserService.class,
		ClientUserSecurityConfig.class,
		ApiSessionConfig.class
})
@PropertySource("classpath:clientUser.properties")
public class ClientUserAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ForwardedHeaderFilter forwardedHeaderFilter() {
		return new ForwardedHeaderFilter();
	}

	@Bean
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
			OAuth2AuthorizedClientRepository authorizedClientRepository) {
		OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler();
		handler.setAuthorizedClientRepository(authorizedClientRepository);
		return handler;
	}

}
