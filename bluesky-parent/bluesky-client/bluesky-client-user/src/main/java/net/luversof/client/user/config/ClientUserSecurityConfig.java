package net.luversof.client.user.config;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class ClientUserSecurityConfig {

  @Bean
  ClientUserProperties clientUserProperties() {
    return new ClientUserProperties();
  }

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  SecurityFilterChain clientUserSecurityFilterChain(
      HttpSecurity http, ClientUserProperties clientUserProperties) throws Exception {
    return http.csrf(CsrfConfigurer::disable)
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .exceptionHandling(exception ->
            exception.authenticationEntryPoint((request, response, authException) -> {
              // htmx · fetch 는 302 를 따라가 로그인 화면 HTML 을 조각 자리에 넣는다. 401 이면 레이아웃 오류
              // 처리기와 JSON errorHandler 가 /login?redirectUrl= 로 보낸다(HtmxUnauthorizedRedirectTest).
              if (expectsStatus(request)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
              }
              String url =
                  UriComponentsBuilder.fromUriString(clientUserProperties.getLoginUrl())
                      .queryParam(
                          "redirectUrl",
                          ServletUriComponentsBuilder.fromRequest(request).build().toUriString())
                      .build()
                      .toUriString();
              response.sendRedirect(url);
            }))
        .oauth2Client(Customizer.withDefaults())
        .logout(logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true))
        .build();
  }

  /** htmx 요청이거나 JSON 을 주고받는 요청 - 화면 이동(302)이 아니라 상태 코드를 기대한다. */
  static boolean expectsStatus(HttpServletRequest request) {
    if (request.getHeader("HX-Request") != null) {
      return true;
    }
    String contentType = request.getContentType();
    if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
      return true;
    }
    String accept = request.getHeader("Accept");
    return accept != null
        && accept.toLowerCase(Locale.ROOT).contains("json")
        && !accept.toLowerCase(Locale.ROOT).contains("text/html");
  }
}
