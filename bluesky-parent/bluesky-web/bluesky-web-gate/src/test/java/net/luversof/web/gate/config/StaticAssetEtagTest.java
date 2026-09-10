package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * 정적 자산은 Last-Modified 가 아니라 내용 기반 ETag 로 재검증한다.
 *
 * <p>실측 2026-09-09(k8s 게이트 gate.web.bluesky.local): jib 이미지는 모든 파일의 mtime 을 1970-01-01 로 고정하므로
 * {@code Last-Modified: Thu, 01 Jan 1970 00:00:01 GMT} 가 배포마다 같고, {@code If-Modified-Since:
 * 2026-01-01} 을 보내도 304 였다. 정적 자산 정책이 {@code no-cache}(저장하되 매번 물어보라)인데 물음에 늘 "안 바뀜" 이라 답하니 배포 뒤에도
 * 브라우저는 옛 JS/CSS 를 쓴다 - 로컬(실제 mtime)에서는 재현되지 않아 오래 숨어 있었다. 날짜 검증을 끄고 ETag 필터를 정적 경로에 건다.
 */
class StaticAssetEtagTest {

  @Test
  void 날짜_검증은_끈다() throws IOException {
    String properties =
        Files.readString(
            Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);
    assertThat(properties).contains("spring.web.resources.cache.use-last-modified=false");
  }

  @Test
  void 정적_경로에_ETag_필터가_걸린다() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
        new GateStaticResourceSecurityConfig().gateStaticResourceEtagFilter();
    assertThat(registration.getFilter()).isInstanceOf(ShallowEtagHeaderFilter.class);
    assertThat(registration.getUrlPatterns())
        .contains("/main.css", "/js/*", "/js/*/*", "/js/*/*/*", "/css/*", "/fonts/*");
    assertThat(registration.getOrder())
        .as("캐시 헤더 필터(MIN_VALUE) 바로 다음에서 응답을 감싼다")
        .isEqualTo(Integer.MIN_VALUE + 1);
  }
}
