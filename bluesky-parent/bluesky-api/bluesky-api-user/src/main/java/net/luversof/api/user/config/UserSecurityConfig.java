package net.luversof.api.user.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;


@Configuration
public class UserSecurityConfig {
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostConstruct
	public void postConstruct() {
		objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
	}

    @Bean
    JdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
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
	UserDetailsManager userDetailsManager(@Qualifier("userDataSource") DataSource userDataSource) {
		return new JdbcUserDetailsManager(userDataSource);
	}

}