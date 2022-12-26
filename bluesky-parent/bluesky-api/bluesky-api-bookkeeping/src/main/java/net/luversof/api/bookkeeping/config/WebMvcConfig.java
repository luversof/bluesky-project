package net.luversof.api.bookkeeping.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/webjars/swagger-ui/swagger-initializer.js")
				.setViewName("/swagger-ui/swagger-initializer.js");
		registry.addRedirectViewController("/", "/webjars/swagger-ui/index.html");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOriginPatterns("http://localhost:[30120,40120]")
		.allowedHeaders("*")
		.allowedMethods("*")
		.allowCredentials(true);
	}

}