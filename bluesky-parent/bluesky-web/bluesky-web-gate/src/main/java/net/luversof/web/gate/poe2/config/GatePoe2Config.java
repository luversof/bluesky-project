package net.luversof.web.gate.poe2.config;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.poe2.httpexchange.Poe2DataClient;
import net.luversof.web.gate.poe2.httpexchange.Poe2EngineClient;
import net.luversof.web.gate.poe2.httpexchange.Poe2RegexClient;

/** PoE2 클라이언트 — PoE1 과 같은 백엔드(bluesky-api-poe)라 PoE1 의 프록시 팩토리를 그대로 쓴다(경로만 /api/poe2). */
@Configuration
public class GatePoe2Config {

  @Bean
  Poe2DataClient poe2DataClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(Poe2DataClient.class);
  }

  /**
   * 엔진 호출 전용 — 공통 빌더(오류 처리기·관측 포함)를 그대로 쓰되 읽기 제한만 90초. 전역 10초(application.properties)는 api-stock
   * 기준이라 PoB 가이드(실빌드 10~25초)에는 짧다. lb:// 면 부하분산 빌더(ClientCommonAutoConfiguration 과 같은 규칙).
   */
  @Bean
  Poe2EngineClient poe2EngineClient(
      @LoadBalanced ObjectProvider<RestClient.Builder> loadBalancedBuilder,
      @Qualifier("restClientBuilder") ObjectProvider<RestClient.Builder> builder,
      @Value("${spring.http.serviceclient.client-poe.base-url:}") String baseUrl) {
    RestClient.Builder b =
        baseUrl.startsWith("lb://") ? loadBalancedBuilder.getObject() : builder.getObject();
    RestClient restClient =
        b.baseUrl(baseUrl)
            .requestFactory(
                ClientHttpRequestFactoryBuilder.detect()
                    .build(
                        HttpClientSettings.defaults()
                            .withTimeouts(Duration.ofSeconds(3), Duration.ofSeconds(90))))
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
        .build()
        .createClient(Poe2EngineClient.class);
  }

  @Bean
  Poe2RegexClient poe2RegexClient(
      @Qualifier("poeHttpServiceProxyFactory") HttpServiceProxyFactory poeHttpServiceProxyFactory) {
    return poeHttpServiceProxyFactory.createClient(Poe2RegexClient.class);
  }
}
