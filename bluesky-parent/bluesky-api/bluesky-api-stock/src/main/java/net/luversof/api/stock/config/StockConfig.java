package net.luversof.api.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class StockConfig {

  @Bean
  JsonMapper jsonMapper(JsonMapper.Builder builder) {
    return builder.build();
  }
}
