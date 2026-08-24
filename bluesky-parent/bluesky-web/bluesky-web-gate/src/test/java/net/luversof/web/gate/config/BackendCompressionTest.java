package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * 백엔드 호출이 gzip 을 협상하는지 <b>실제로 요청을 보내</b> 확인한다.
 *
 * <p>왜 중요한가(실측 2026-08-24): api-stock 의 일별 시계열은 압축 없이 1,584,142 바이트인데 gzip 으로는 59,181 바이트다 (26.8
 * 배). 게이트가 {@code Accept-Encoding} 을 보내지 않으면 그 차이를 그대로 네트워크로 실어 나른다. 같은 파드가 아니면 바로 비용이다.
 *
 * <p>Apache HttpClient 5 는 기본으로 압축을 협상하지만 {@code disableContentCompression()} 한 줄이면 꺼진다. 설정이 아니라
 * 행동으로 못박는다 &mdash; 로컬 서버를 띄워 실제로 받은 요청 헤더를 본다.
 */
class BackendCompressionTest {

  private final AtomicReference<String> seenAcceptEncoding = new AtomicReference<>();

  private static final String BODY = "0123456789".repeat(500);

  private HttpServer startServer() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          seenAcceptEncoding.set(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
          byte[] raw = BODY.getBytes(StandardCharsets.UTF_8);
          java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
          try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(raw);
          }
          byte[] compressed = buffer.toByteArray();
          exchange.getResponseHeaders().add("Content-Encoding", "gzip");
          exchange.getResponseHeaders().add("Content-Type", "text/plain");
          exchange.sendResponseHeaders(200, compressed.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(compressed);
          }
        });
    server.start();
    return server;
  }

  /** 앱이 쓰는 팩토리 빌더 그대로 만든 클라이언트. */
  private RestClient appRestClient() {
    return RestClient.builder()
        .requestFactory(new GateHttpClientPoolConfig().clientHttpRequestFactoryBuilder().build())
        .build();
  }

  @Test
  void 백엔드_호출은_gzip_을_요청하고_받은_응답을_푼다() throws IOException {
    HttpServer server = startServer();
    try {
      String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

      String body = appRestClient().get().uri(baseUrl + "/ping").retrieve().body(String.class);

      assertThat(seenAcceptEncoding.get())
          .as("Accept-Encoding 을 보내지 않는다 - 일별 시계열 기준 26.8 배를 압축 없이 실어 나르게 된다")
          .isNotNull()
          .contains("gzip");
      assertThat(body).as("gzip 응답을 풀지 못했다 - 본문이 깨지면 압축 협상 자체가 위험해진다").isEqualTo(BODY);
    } finally {
      server.stop(0);
    }
  }
}
