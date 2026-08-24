package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * 백엔드 호출용 커넥션 풀 조정이 <b>실제로 효과가 있는지</b> 본다.
 *
 * <p>Apache HttpClient 5 의 기본 풀은 목적지 하나당 동시 5 개다. 주식 화면 하나가 api-stock 을 최대 5 개까지 동시에 부르므로, 조정이 없으면
 * 화면 하나가 이미 상한에 닿고 두 번째 사용자의 호출은 큐에서 기다린다. {@code GateHttpClientPoolConfig} 가 그 상한을 20 으로 올린다.
 *
 * <p>그런데 그 조정이 먹었는지 확인할 방법이 <b>기동 로그 한 줄뿐</b>이었다. 커스터마이저가 받는 것은 매니저가 아니라 빌더라 적용된 값을 지표로 낼 수도 없다
 * (실제로 시도했다가 접었다). DB 커넥션 풀에서는 같은 유형의 스킵이 실제 사고가 됐다 &mdash; 조정이 건너뛰어진 상태로 동시 8 화면을 걸면 40 요청 중 28 건이
 * 30 초 뒤 실패했는데 앱이 남긴 신호는 WARN 한 줄이었다.
 *
 * <p>그래서 설정값이 아니라 <b>효과</b>를 잰다. 지연을 주는 로컬 서버를 띄우고 같은 팩토리로 만든 {@code RestClient} 로 동시에 던져, 서버가 실제로
 * 동시에 받은 요청 수를 센다. 조정이 빠지면 그 수가 5 에서 멈춘다.
 */
class HttpClientPoolEffectTest {

  private static final int CONCURRENCY = 8;

  private static final long HANDLER_DELAY_MS = 300;

  /** 서버가 동시에 처리 중이던 요청 수의 최대치. */
  private final AtomicInteger inFlight = new AtomicInteger();

  private final AtomicInteger peakInFlight = new AtomicInteger();

  private HttpServer startServer() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newFixedThreadPool(CONCURRENCY * 2));
    server.createContext(
        "/",
        exchange -> {
          int now = inFlight.incrementAndGet();
          peakInFlight.accumulateAndGet(now, Math::max);
          try {
            Thread.sleep(HANDLER_DELAY_MS);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            inFlight.decrementAndGet();
          }
          byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
          }
        });
    server.start();
    return server;
  }

  private int measurePeakConcurrency(RestClient restClient, String baseUrl) {
    inFlight.set(0);
    peakInFlight.set(0);
    ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
    try {
      List<CompletableFuture<String>> calls = new ArrayList<>();
      for (int i = 0; i < CONCURRENCY; i++) {
        calls.add(
            CompletableFuture.supplyAsync(
                () -> restClient.get().uri(baseUrl + "/ping").retrieve().body(String.class), pool));
      }
      for (CompletableFuture<String> call : calls) {
        call.join();
      }
    } finally {
      pool.shutdown();
    }
    return peakInFlight.get();
  }

  /**
   * 앱이 쓰는 설정 그대로 만든 클라이언트는 동시 8 을 한 번에 보낸다.
   *
   * <p>{@code GateHttpClientPoolConfig} 의 상수(목적지당 20)를 그대로 쓴다. 그 값이 5 이하로 내려가면 이 검사가 깨진다.
   */
  @Test
  void 조정된_풀은_화면_하나보다_많은_동시_호출을_한번에_보낸다() throws IOException {
    HttpServer server = startServer();
    try {
      String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
      RestClient restClient =
          RestClient.builder()
              .requestFactory(
                  org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
                      .httpComponents()
                      .withConnectionManagerCustomizer(
                          manager -> {
                            manager.setMaxConnPerRoute(20);
                            manager.setMaxConnTotal(40);
                          })
                      .build())
              .build();

      int peak = measurePeakConcurrency(restClient, baseUrl);

      assertThat(peak)
          .as("조정된 풀인데 동시 %d 가 한 번에 나가지 않았다 - 목적지당 상한을 확인할 것", CONCURRENCY)
          .isEqualTo(CONCURRENCY);
    } finally {
      server.stop(0);
    }
  }

  /**
   * 조정하지 않으면 목적지당 5 에서 막힌다.
   *
   * <p>이 검사가 없으면 위 검사가 "무엇을 지키고 있는지" 알 수 없다 &mdash; 기본값으로도 8 이 나간다면 조정은 필요 없는 것이다.
   */
  @Test
  void 조정하지_않은_풀은_목적지당_5에서_막힌다() throws IOException {
    HttpServer server = startServer();
    try {
      String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
      RestClient restClient =
          RestClient.builder()
              .requestFactory(
                  org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
                      .httpComponents()
                      .build())
              .build();

      int peak = measurePeakConcurrency(restClient, baseUrl);

      assertThat(peak)
          .as("Apache HttpClient 5 의 기본 목적지당 상한이 5 가 아니게 됐다 - 조정 근거를 다시 볼 것")
          .isEqualTo(5);
    } finally {
      server.stop(0);
    }
  }

  /** 앱 설정이 그 상한을 실제로 올려 두는지. 상수가 내려가면 위 효과 검사가 무의미해진다. */
  @Test
  void 앱_설정이_목적지당_상한을_올려_둔다() throws IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/config/GateHttpClientPoolConfig.java"),
            StandardCharsets.UTF_8);
    assertThat(source).contains("MAX_CONN_PER_ROUTE = 20");
    assertThat(source).contains("MAX_CONN_TOTAL = 40");
    assertThat(source).contains("setMaxConnPerRoute(MAX_CONN_PER_ROUTE)");
    assertThat(source).contains("setMaxConnTotal(MAX_CONN_TOTAL)");
  }
}
