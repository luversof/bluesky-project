package net.luversof.web.gate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import feign.Feign;
import feign.Logger.Level;
import feign.RequestInterceptor;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@EnableConfigurationProperties(GateFeignProperties.class)
public class GateFeignConfig {

	/*
	 * feign client 전체 적용
	 */
	@Bean
	RequestInterceptor feignClientRequestInterceptor () {
		 return restTemplate -> 
		 	restTemplate
//		 		.header(HttpHeaders.ACCEPT_LANGUAGE, new String[]{ LocaleContextHolder.getLocale().toLanguageTag() })
		 		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		 		;
	}
	
	@Bean
	Feign.Builder feignBuilder(MeterRegistry meterRegistry) {
		return Feign
				.builder()
				.logLevel(Level.FULL)
				.addCapability(new MicrometerCapability(meterRegistry));
	}
}
