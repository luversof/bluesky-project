package net.luversof.web.gate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * 백엔드 호출용 HTTP 커넥션 풀 상한을 올린다.
 *
 * <p>이 앱의 {@code RestClient} 는 Apache HttpClient 5 로 만들어지는데(실측: Spring Boot 의 {@code
 * ClientHttpRequestFactoryBuilder.detect()} 가 {@code HttpComponentsClientHttpRequestFactory} 를
 * 고른다), 그 기본 커넥션 풀은 <b>목적지 하나당 동시 5개</b>다. 이 상한은 프로퍼티로 노출돼 있지 않아 여기서 조정한다({@code
 * spring.http.clients.*} 에는 타임아웃·리다이렉트·SSL 만 있다).
 *
 * <p>왜 필요한가(실측, 지연 300ms 짜리 로컬 서버에 같은 {@code RestClient} 로 동시 요청):
 *
 * <pre>
 *   기본(5)   동시 4 -&gt; 서버가 본 동시 4,  308ms
 *             동시 8 -&gt; 서버가 본 동시 5,  605ms   (2 회로 나뉘어 나감)
 *             동시16 -&gt; 서버가 본 동시 5, 1207ms   (4 회)
 *   상한 20   동시 4 -&gt; 서버가 본 동시 4,  303ms
 *             동시 8 -&gt; 서버가 본 동시 8,  305ms   (한 번에 나감)
 * </pre>
 *
 * <p>주식 화면 하나가 api-stock 를 최대 5개까지 동시에 호출한다. 즉 화면 하나가 이미 상한에 닿아 있고, 이 풀은 게이트 프로세스 전체가 공유하므로 두 번째
 * 사용자의 호출은 첫 번째가 끝날 때까지 큐에서 기다린다. 목적지별 상한을 올려 그 직렬화를 없앤다.
 *
 * <p>상한을 20 으로 둔 이유: api-stock 의 DB 커넥션 풀이 20 이라 그보다 더 보내도 거기서 대기하게 된다. 전체 40 은 게이트가 부르는 다른 백엔드
 * (user/blog/board/bookkeeping)까지 합친 여유값이다.
 */
@Configuration
public class GateHttpClientPoolConfig {

  private static final Logger log = LoggerFactory.getLogger(GateHttpClientPoolConfig.class);

  private static final int MAX_CONN_PER_ROUTE = 20;

  private static final int MAX_CONN_TOTAL = 40;

  /**
   * Boot 의 자동 설정 빈({@code @ConditionalOnMissingBean})을 대신한다. 타임아웃·SSL·리다이렉트는 별도의 {@code
   * ClientHttpRequestFactorySettings} 로 적용되므로 여기서 건드리지 않는다.
   */
  @Bean
  ClientHttpRequestFactoryBuilder<HttpComponentsClientHttpRequestFactory>
      clientHttpRequestFactoryBuilder() {
    return ClientHttpRequestFactoryBuilder.httpComponents()
        .withConnectionManagerCustomizer(
            manager -> {
              manager.setMaxConnPerRoute(MAX_CONN_PER_ROUTE);
              manager.setMaxConnTotal(MAX_CONN_TOTAL);
              log.info(
                  "http client pool tuned: maxConnPerRoute={}, maxConnTotal={}",
                  MAX_CONN_PER_ROUTE,
                  MAX_CONN_TOTAL);
            });
  }
}
