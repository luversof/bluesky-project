package net.luversof.client.user.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Common OAuth2 Security Configuration for all bluesky-web modules
 * 
 * Provides:
 * - OAuth2 Login with GitHub/Kakao
 * - Common logout configuration
 * - CSRF disabled (for REST APIs)
 * - OAuth2 Client Manager
 */
@Configuration
@EnableWebSecurity
public class CommonOAuth2SecurityConfig {

	/**
	 * Configure SecurityFilterChain for OAuth2 Login
	 * All requests are permitted by default
	 * Authentication can be enforced at controller or page level
	 * 
	 * Note: exceptionHandling with empty authenticationEntryPoint prevents
	 * automatic redirect to /login
	 * OAuth2 login will only trigger when user explicitly clicks login button
	 * (/oauth2/authorization/{provider})
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			ObjectProvider<OAuth2LoginSuccessHandler> successHandlerProvider,
			ObjectProvider<CustomOAuth2UserService> customOAuth2UserServiceProvider) throws Exception {
		return http
				.csrf(CsrfConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2
						.successHandler(successHandlerProvider.getObject())
						.userInfoEndpoint(
								userInfo -> userInfo.userService(customOAuth2UserServiceProvider.getObject())))
				.oauth2Client(Customizer.withDefaults())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.clearAuthentication(true))
				.build();
	}

	/**
	 * OAuth2 Authorized Client Manager
	 * Supports authorization_code and refresh_token grant types
	 */
	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.authorizationCode()
				.refreshToken()
				.build();

		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientRepository);

		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}
}
