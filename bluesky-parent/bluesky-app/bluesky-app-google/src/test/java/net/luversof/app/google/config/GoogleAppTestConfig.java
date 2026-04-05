package net.luversof.app.google.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@PropertySource("classpath:bluesky-app-google.properties")
public class GoogleAppTestConfig {

  @Bean
  JsonMapper jsonMapper(JsonMapper.Builder builder) {
    return builder.build();
  }
}
