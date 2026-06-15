package net.luversof.web.gate.config;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration(proxyBeanMethods = false)
public class GateRestClientConfig {

  @Bean
  // ⚠️ 모든 인증서를 신뢰(TLS 검증 무력화)하므로 운영 환경에는 절대 적용 금지.
  // 개발용 프로필(localdev, k8sdev)에서만 활성화된다. k8sdev는 로컬이 아닌 개발 클러스터이므로,
  // 인터넷에 노출되지 않고 자체 서명 인증서를 의도적으로 사용하는 환경인지 반드시 확인할 것.
  @Profile({"localdev", "k8sdev"})
  RestClientCustomizer 이중인증우회Customizer() {
    return restClientBuilder -> {
      try {
        // 1. 모든 인증서를 신뢰하는 SSLContext 생성
        SSLContext sslContext =
            SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();

        // 2. 호스트네임 검증을 무시하는 HttpClient 구성
        CloseableHttpClient httpClient =
            HttpClients.custom()
                .setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create()
                        .setTlsSocketStrategy(
                            new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
                        .build())
                .build();

        // 3. RequestFactory 주입 및 기본 설정
        restClientBuilder
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .defaultHeader("Content-Type", "application/json");

      } catch (Exception e) {
        throw new IllegalStateException("RestClient SSL 우회 설정 중 오류 발생", e);
      }
    };
  }
}
