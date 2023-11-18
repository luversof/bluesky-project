package net.luversof.web.gate.feign.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import feign.Feign;
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
		 return restTemplate -> restTemplate.header("Accept-Language", new String[]{ LocaleContextHolder.getLocale().toLanguageTag() });
	}
	
	@Bean
	Feign.Builder feignBuilder(MeterRegistry meterRegistry) {
		return Feign.builder().addCapability(new MicrometerCapability(meterRegistry));
	}
}
