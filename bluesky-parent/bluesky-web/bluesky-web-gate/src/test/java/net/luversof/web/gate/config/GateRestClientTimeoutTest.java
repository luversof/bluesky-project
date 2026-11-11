package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 개발 프로필의 신뢰-전부 요청 팩토리도 설정된 읽기 타임아웃을 지킨다.
 *
 * <p>실측 2026-09-10(qa/slow-proxy.cjs 12s 지연 + slow-backend-ux.cjs): 예전 구성은 {@code
 * HttpClients.custom()} 으로 만든 클라이언트를 직접 넣어 {@code spring.http.clients.read-timeout=10s} 가
 * localdev/k8sdev 에서 적용되지 않았다 - 자산 성장 조각이 15.5s 뒤 200 으로 그냥 돌아왔다. 응답을 영영 안 주는 서버에 700ms 읽기 타임아웃으로
 * 붙어 그 안에 끊기는지 본다.
 */
class GateRestClientTimeoutTest {

  @Test
  void 응답이_없는_서버에는_설정된_읽기_타임아웃_안에_예외가_난다() throws Exception {
    try (ServerSocket hung = new ServerSocket(0)) {
      List<Socket> accepted = new ArrayList<>();
      Thread acceptor =
          new Thread(
              () -> {
                try {
                  while (!hung.isClosed()) accepted.add(hung.accept()); // 받기만 하고 답하지 않는다
                } catch (IOException ignored) {
                }
              });
      acceptor.setDaemon(true);
      acceptor.start();

      HttpClientSettings settings =
          HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(2), Duration.ofMillis(700));
      ClientHttpRequestFactory factory =
          GateRestClientConfig.trustAllRequestFactory(
              ClientHttpRequestFactoryBuilder.httpComponents(), settings);
      RestClient client = RestClient.builder().requestFactory(factory).build();

      long started = System.nanoTime();
      assertThatThrownBy(
              () ->
                  client
                      .get()
                      .uri("http://127.0.0.1:" + hung.getLocalPort() + "/hang")
                      .retrieve()
                      .body(String.class))
          .isInstanceOf(ResourceAccessException.class);
      long elapsedMs = (System.nanoTime() - started) / 1_000_000;
      assertThat(elapsedMs).as("읽기 타임아웃(700ms) 근처에서 끊겨야 한다").isBetween(500L, 5_000L);
      assertThat(accepted).as("서버가 연결은 받았어야 실험이 유효하다").isNotEmpty();
      for (Socket s : accepted) s.close();
    }
  }

  @Test
  void 개발_프로필_구성은_직접_만든_HttpClient_대신_자동_구성_빌더를_쓴다() throws IOException {
    String src =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/config/GateRestClientConfig.java"),
            StandardCharsets.UTF_8);
    // 주석은 예전 구성을 설명하느라 그 이름을 쓴다 - 코드 부분만 본다.
    String code = src.replaceAll("(?m)^\\s*//.*$", "").replaceAll("(?s)/\\*.*?\\*/", "");
    assertThat(code).doesNotContain("HttpClients.custom()");
    assertThat(src).contains("trusting.build(settings)");
    assertThat(src).contains("HttpClientSettings settings");
  }
}
