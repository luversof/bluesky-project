package net.luversof.api.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class UserSecurityConfig {

  @Bean
  @Order(3)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/assets/**", "/error", "/actuator/**", "/api/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
