package net.luversof.api.user.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;


@Configuration
public class UserSecurityConfig {
	
//	@Setter(onMethod_ = @Autowired)
//	private ObjectMapper objectMapper;
//	
//	@PostConstruct
//	public void postConstruct() {
//		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
//	}
//	
//	@Bean
//	RestTemplateCustomizer disableDefaultTypingForRestTemplate() {
//		return restTemplate -> {
//			restTemplate.getMessageConverters().stream()
//				.filter(MappingJackson2HttpMessageConverter.class::isInstance)
//				.map(c -> (MappingJackson2HttpMessageConverter) c)
//				.forEach(converter -> {
//					ObjectMapper copy = converter.getObjectMapper().copy();
//					// remove any global default typing so plain JSON (no '@class') can be read
//					copy.setDefaultTyping(null);
//					converter.setObjectMapper(copy);
//				});
//		};
//	}

	@Bean
	JdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService(
			NamedParameterJdbcOperations namedParameterJdbcOperations, 
			ClientRegistrationRepository clientRegistrationRepository, 
			UserInfoService userInfoService) {
		return new JdbcOAuth2AuthorizedClientService(namedParameterJdbcOperations.getJdbcOperations(), clientRegistrationRepository) {

			@Override
			public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
				super.saveAuthorizedClient(authorizedClient, principal);
				var userName = makeUsername(authorizedClient);
				if (userInfoService.findByUsername(userName).isEmpty()) {
					var userInfo = new UserInfo();
					userInfo.setUsername(userName);
					userInfoService.save(userInfo);
				};
			}
			
		};
	}
	
	private String makeUsername(OAuth2AuthorizedClient authorizedClient) {
		return authorizedClient.getClientRegistration().getClientId()
				.replace("-local", "")
				.replace("-local2", "")
				+ ":" + authorizedClient.getPrincipalName();
	}

	@Bean
	SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
		http
		.authorizeHttpRequests(request -> request.anyRequest().permitAll())
		.csrf(csrf -> csrf.disable());
		return http.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsManager userDetailsManager(@Qualifier("routingDataSource") DataSource routingDataSource) {
		return new JdbcUserDetailsManager(routingDataSource);
	}

}