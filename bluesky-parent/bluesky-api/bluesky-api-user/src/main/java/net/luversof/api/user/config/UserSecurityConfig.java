package net.luversof.api.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;
import net.luversof.api.user.repository.UserInfoRepository;

@Configuration
public class UserSecurityConfig {

	@Setter(onMethod_ = @Autowired)
	private UserInfoRepository userInfoRepository;

	@Bean
	@Order(3)
	SecurityFilterChain defaultSecurityFilterChain(
			HttpSecurity http,
			OAuth2AuthorizedClientRepository authorizedClientRepository) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/assets/**", "/error", "/actuator/**", "/api/**").permitAll()
				.anyRequest().authenticated())
			.formLogin(formLogin -> formLogin
				.loginPage("/login")
				.permitAll())
			.oauth2Login(oauth2Login -> oauth2Login
				.loginPage("/login")
				.authorizedClientRepository(authorizedClientRepository))
			.logout(logout -> logout.permitAll());

		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			var userInfo = userInfoRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

			return User.builder()
				.username(userInfo.getUsername())
				.password(userInfo.getPassword() != null ? userInfo.getPassword() : "{noop}password")
				.authorities("ROLE_USER")
				.build();
		};
	}

	@Bean
	OAuth2AuthorizedClientService authorizedClientService(
			JdbcTemplate jdbcTemplate,
			ClientRegistrationRepository clientRegistrationRepository) {
		return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
	}

	@Bean
	OAuth2AuthorizedClientRepository authorizedClientRepository(
			OAuth2AuthorizedClientService authorizedClientService) {
		return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
	}

}