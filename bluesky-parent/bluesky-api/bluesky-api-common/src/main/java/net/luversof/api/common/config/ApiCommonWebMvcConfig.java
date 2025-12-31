package net.luversof.api.common.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class ApiCommonWebMvcConfig implements WebMvcConfigurer {

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
