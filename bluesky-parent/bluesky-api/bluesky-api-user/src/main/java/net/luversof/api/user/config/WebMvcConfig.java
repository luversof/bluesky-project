package net.luversof.api.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOriginPatterns("http://*.bluesky.local")
		.allowedHeaders("*")
		.allowedMethods("*")
		.allowCredentials(true);
	}
	
	@Bean
	SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
		http
		.authorizeHttpRequests().anyRequest().permitAll().and()
		.csrf().disable();
		return http.build();
	}

}