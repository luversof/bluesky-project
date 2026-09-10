package net.luversof.web.gate.config;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
public class GateRestClientConfig {

  @Bean
  // ⚠️ 모든 인증서를 신뢰(TLS 검증 무력화)하므로 운영 환경에는 절대 적용 금지.
  // 개발용 프로필(localdev, k8sdev)에서만 활성화된다. k8sdev는 로컬이 아닌 개발 클러스터이므로,
  // 인터넷에 노출되지 않고 자체 서명 인증서를 의도적으로 사용하는 환경인지 반드시 확인할 것.
  //
  // 실측 2026-09-10(qa/slow-proxy.cjs + slow-backend-ux.cjs): 예전에는 여기서 HttpClients.custom() 으로 클라이언트를
  // 직접
  // 만들어 넣었기 때문에 application.properties 의 spring.http.clients.read-timeout=10s 와
  // GateHttpClientPoolConfig 의
  // 풀 설정이 이 두 프로필에서는 전혀 적용되지 않았다 - api-stock 을 12s 지연시키니 자산 성장 조각이 15.5s 뒤 200 으로
  // 그냥 돌아왔다(타임아웃 없음). 지금은 자동 구성된 빌더(풀 설정 포함)에 신뢰-전부 TLS 전략만 얹고 같은 HttpClientSettings
  // 로 만들어, 타임아웃·풀이 운영 프로필과 같은 값으로 걸린다.
  @Profile({"localdev", "k8sdev"})
  RestClientCustomizer 이중인증우회Customizer(
      ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder, HttpClientSettings settings) {
    ClientHttpRequestFactory factory = trustAllRequestFactory(requestFactoryBuilder, settings);
    return restClientBuilder ->
        restClientBuilder.requestFactory(factory).defaultHeader("Content-Type", "application/json");
  }

  /**
   * 자동 구성된 빌더에 '모든 인증서 신뢰 + 호스트명 검증 생략' TLS 전략을 얹어 요청 팩토리를 만든다. 타임아웃·풀·리다이렉트 같은 나머지 설정은 {@code
   * settings}(= spring.http.clients.*)와 빌더 빈에서 그대로 온다.
   */
  static ClientHttpRequestFactory trustAllRequestFactory(
      ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder, HttpClientSettings settings) {
    try {
      SSLContext sslContext =
          SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
      ClientHttpRequestFactoryBuilder<?> trusting =
          requestFactoryBuilder instanceof HttpComponentsClientHttpRequestFactoryBuilder hc
              ? hc.withTlsSocketStrategyFactory(
                  bundle -> new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
              : requestFactoryBuilder;
      return trusting.build(settings);
    } catch (Exception e) {
      throw new IllegalStateException("RestClient SSL 우회 설정 중 오류 발생", e);
    }
  }
}
