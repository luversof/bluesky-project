package net.luversof.client.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ClientUserProperties.class)
@ConditionalOnProperty(prefix = "bluesky.client.user.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClientUserSecurityConfig {

	@Bean
	SecurityFilterChain clientUserSecurityFilterChain(HttpSecurity http, ClientUserProperties clientUserProperties)
			throws Exception {
		return http
				.csrf(CsrfConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.anyRequest().permitAll())
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(
								new LoginUrlAuthenticationEntryPoint(clientUserProperties.getLoginUrl())))
				.oauth2Client(Customizer.withDefaults())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.clearAuthentication(true))
				.build();
	}

}
