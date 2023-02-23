package net.luversof.web.gate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import feign.RequestInterceptor;

@Configuration
@EnableConfigurationProperties(GateProperties.class)
public class GateWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOriginPatterns("http://*.bluesky.local:*")
		.allowedHeaders("*")
		.allowedMethods("*")
		.allowCredentials(true);
	}

	/*
	 * feign client 전체 적용
	 */
	@Bean
	RequestInterceptor acceptLanguageHeaderRequestInterceptor () {
		 return restTemplate -> restTemplate.header("Accept-Language", new String[]{ LocaleContextHolder.getLocale().toLanguageTag() });
	}
}