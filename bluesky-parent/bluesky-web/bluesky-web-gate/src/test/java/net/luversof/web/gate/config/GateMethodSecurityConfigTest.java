package net.luversof.web.gate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;

/**
 * {@code @BlueskyPreAuthorize} 가 실제로 막는다(2026-11-13 전엔 메서드 보안이 꺼져 비로그인도 통과했다). 앱 전체 대신 보안 설정만 올린 작은
 * 컨텍스트로 본다 - 이 모듈의 전체 컨텍스트는 config server 없이 뜨지 않는다(ContextDependentTestInventoryTest).
 */
class GateMethodSecurityConfigTest {

  static class Guarded {
    @BlueskyPreAuthorize
    String write() {
      return "ok";
    }
  }

  @Configuration
  @Import(GateMethodSecurityConfig.class)
  static class TestConfig {
    @Bean
    Guarded guarded() {
      return new Guarded();
    }
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static String call() {
    try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
      return context.getBean(Guarded.class).write();
    }
  }

  @Test
  void 비로그인은_막힌다() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThatThrownBy(GateMethodSecurityConfigTest::call)
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void ROLE_USER_가_없는_옛_로그인_세션은_막힌다() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "octo", null, List.of(new SimpleGrantedAuthority("OAUTH2_USER"))));

    assertThatThrownBy(GateMethodSecurityConfigTest::call)
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void ROLE_USER_는_통과한다() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "octo", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    assertThat(call()).isEqualTo("ok");
  }
}
