package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * 백엔드 호출 커넥션 풀 설정이 실제로 만들어지는지 본다.
 *
 * <p>이 설정은 Apache HttpClient 5 가 클래스패스에 있을 때만 성립한다. 빠지면 {@code
 * ClientHttpRequestFactoryBuilder.httpComponents()} 가 기동 중에 터지고, 그때는 이미 화면이 죽은 뒤다. 빌드에서 먼저 잡는다.
 *
 * <p>배경(실측, 지연 300ms 로컬 서버에 같은 RestClient 로 동시 요청): 기본 풀은 목적지당 동시 5개라 동시 8 요청이 605ms 걸렸고(서버가 본 동시
 * 5), 상한을 20 으로 올리자 305ms 로 줄었다(서버가 본 동시 8). 주식 화면 하나가 api-stock 를 최대 5개까지 동시에 부르므로 화면 하나가 이미 기본
 * 상한에 닿아 있었다.
 */
class GateHttpClientPoolConfigTest {

  @Test
  void 커넥션풀_설정이_아파치_팩토리를_만든다() {
    var factory = new GateHttpClientPoolConfig().clientHttpRequestFactoryBuilder().build();

    assertThat(factory)
        .as("Apache HttpClient 5 가 클래스패스에서 빠지면 여기서 먼저 깨져야 한다")
        .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }
}
