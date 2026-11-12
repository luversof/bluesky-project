package net.luversof.web.gate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * {@code @BlueskyPreAuthorize}({@code @PreAuthorize("hasRole('USER')")}) 를 실제로 동작시킨다. 예전엔 메서드 보안이
 * 꺼져 있어 아무것도 막지 않았다(2026-11-13 감사). ROLE_USER 는 로그인 때 CustomOAuth2UserService 가 붙인다.
 */
@Configuration
@EnableMethodSecurity
public class GateMethodSecurityConfig {

  /**
   * 인가 실패는 Spring Security 의 ExceptionTranslationFilter 가 처리해야 비로그인은 로그인(htmx · JSON 은 401)으로, 권한
   * 없음은 403 으로 간다. bluesky-boot 의 CoreMvcExceptionHandler 가 모든 Exception 을 잡아 오류 화면으로 바꾸므로, 그보다 먼저
   * 받아 그대로 다시 던진다.
   */
  @ControllerAdvice
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class AccessDeniedPassThroughAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public void rethrow(AccessDeniedException e) {
      throw e;
    }
  }
}
