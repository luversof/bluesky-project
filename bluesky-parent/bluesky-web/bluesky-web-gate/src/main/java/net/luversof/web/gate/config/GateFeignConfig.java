package net.luversof.web.gate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;

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
	RequestInterceptor acceptLanguageHeaderRequestInterceptor () {
		 return restTemplate -> restTemplate.header(HttpHeaders.ACCEPT_LANGUAGE, new String[]{ LocaleContextHolder.getLocale().toLanguageTag() });
	}
	
	@Bean
	Feign.Builder feignBuilder(MeterRegistry meterRegistry) {
		return Feign
				.builder()
				.logLevel(Level.FULL)
				.addCapability(new MicrometerCapability(meterRegistry));
	}
}
