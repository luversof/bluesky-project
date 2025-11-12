package net.luversof.api.user.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;
import net.luversof.api.user.repository.UserInfoRepository;

@Configuration
public class UserSecurityConfig {

	@Setter(onMethod_ = @Autowired)
	private UserInfoRepository userInfoRepository;

	@Bean
	@Order(3)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/assets/**", "/error", "/actuator/**").permitAll()
						.anyRequest().authenticated())
				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.permitAll())
				.oauth2Login(oauth2Login -> oauth2Login
						.loginPage("/login")
						.userInfoEndpoint(userInfo -> userInfo
								.userAuthoritiesMapper(grantedAuthoritiesMapper())))
				.logout(logout -> logout
						.logoutSuccessUrl("/")
						.permitAll())
				.csrf(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			// UserInfo 조회
			var userInfo = userInfoRepository.findByUsername(username)
					.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

			// UserDetails 생성
			return User.builder()
					.username(userInfo.getUsername())
					.password(userInfo.getPassword() != null ? userInfo.getPassword() : "{noop}password")
					.authorities("ROLE_USER")
					.build();
		};
	}

	@Bean
	GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
		return authorities -> {
			Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
			mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

			// OAuth2 provider별 추가 권한 매핑
			authorities.forEach(authority -> {
				if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
					// 필요시 추가 권한 매핑
				}
			});

			return mappedAuthorities;
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	OAuth2AuthorizedClientService authorizedClientService(
			JdbcTemplate jdbcTemplate,
			ClientRegistrationRepository clientRegistrationRepository) {
		return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
	}

}