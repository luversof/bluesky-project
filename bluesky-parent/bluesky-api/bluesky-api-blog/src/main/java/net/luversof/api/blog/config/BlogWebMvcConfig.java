package net.luversof.api.blog.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BlogWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(@NonNull CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOriginPatterns("http://*.bluesky.local:[*]", "https://*.bluesky.local:[*]")
		.allowedHeaders("*")
		.allowedMethods("*")
		.allowCredentials(true);
	}
	
	@Override
	public void addViewControllers(@NonNull ViewControllerRegistry registry) {
		registry.addRedirectViewController("/", "/swagger-ui.html");
	}

}