package net.luversof.web.gate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import feign.Feign;
import feign.RequestInterceptor;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@EnableConfigurationProperties(GateProperties.class)
public class GateWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		.allowedOriginPatterns("http://*.bluesky.local:[*]")
		.allowedHeaders("*")
		.allowedMethods("*")
		.allowCredentials(true);
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LocaleChangeInterceptor());
	}

	@Bean
	LocaleResolver localeResolver() {
		return new CookieLocaleResolver();
	}
	
	/*
	 * feign client 전체 적용
	 */
	@Bean
	RequestInterceptor acceptLanguageHeaderRequestInterceptor () {
		 return restTemplate -> restTemplate.header("Accept-Language", new String[]{ LocaleContextHolder.getLocale().toLanguageTag() });
	}
	
	@Bean
	Feign.Builder feignBuilder(MeterRegistry meterRegistry) {
		return Feign.builder().addCapability(new MicrometerCapability(meterRegistry));
	}
}