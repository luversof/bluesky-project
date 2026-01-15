package net.luversof.app.google.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@PropertySource("classpath:bluesky-app-google.properties")
public class GoogleAppConfig {

	@Bean
	@ConditionalOnMissingBean
	JsonMapper jsonMapper(JsonMapper.Builder builder) {
		return builder.build();
	}
}
