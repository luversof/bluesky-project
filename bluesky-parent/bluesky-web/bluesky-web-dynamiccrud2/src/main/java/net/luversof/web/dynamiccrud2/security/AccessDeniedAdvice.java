package net.luversof.web.dynamiccrud2.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.config.ClientUserProperties;

/**
 * 권한 거부 응답. 공통 CoreMvcExceptionHandler 가 MVC 안에서 Throwable 을 잡아 500 으로 만들어 Spring Security 의
 * ExceptionTranslationFilter 까지 가지 않으므로(실측 2026-11-17) 여기서 같은 규칙으로 처리한다: 익명이면 로그인으로(htmx · JSON 은
 * 401, ClientUserSecurityConfig 와 같다), 로그인 상태면 403.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedAdvice {

  private final MenuAuthorizer menuAuthorizer;
  private final ClientUserProperties clientUserProperties;

  public AccessDeniedAdvice(
      MenuAuthorizer menuAuthorizer, ClientUserProperties clientUserProperties) {
    this.menuAuthorizer = menuAuthorizer;
    this.clientUserProperties = clientUserProperties;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (menuAuthorizer.isLoggedIn()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    if (expectsStatus(request)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    response.sendRedirect(
        UriComponentsBuilder.fromUriString(clientUserProperties.getLoginUrl())
            .queryParam(
                "redirectUrl",
                ServletUriComponentsBuilder.fromRequest(request).build().toUriString())
            .build()
            .toUriString());
  }

  /** htmx 요청이거나 JSON 을 주고받는 요청 - 화면 이동이 아니라 상태 코드를 기대한다 */
  private static boolean expectsStatus(HttpServletRequest request) {
    if (request.getHeader("HX-Request") != null) {
      return true;
    }
    var contentType = request.getContentType();
    var accept = request.getHeader("Accept");
    return (contentType != null && contentType.toLowerCase().contains("json"))
        || (accept != null && accept.toLowerCase().contains("json"));
  }
}
