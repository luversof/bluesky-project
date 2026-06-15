package net.luversof.api.stock.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class StockConfig {

  @Bean
  JsonMapper jsonMapper(JsonMapper.Builder builder) {
    return builder.build();
  }

  /**
   * 외부(KIS) API 호출용 RestTemplate. connect/read 타임아웃을 지정해 응답이 없는 엔드포인트에서 호출 스레드가 무한 대기하는 것을
   * 방지한다.
   */
  @Bean
  RestTemplate kisRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));
    return new RestTemplate(factory);
  }
}
