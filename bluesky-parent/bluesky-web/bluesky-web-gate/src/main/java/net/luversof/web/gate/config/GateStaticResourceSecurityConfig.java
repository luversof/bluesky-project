package net.luversof.web.gate.config;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 정적 자산의 캐시 동작을 정한다.
 *
 * <p>기본 보안 설정이 모든 응답에 {@code no-cache, no-store} 를 붙여 브라우저가 css/js/폰트를 저장하지 못했다. 그래서 페이지를 옮길 때마다
 * 14개 파일 약 145KB 를 매번 다시 받았다(실측).
 *
 * <p>두 가지를 한다. 하나는 이 경로들을 보안 필터 체인에서 빼는 것(로그인 화면에서도 쓰는 공개 파일이라 보호할 내용이 없다). 다른 하나는 {@code no-cache}
 * 를 직접 붙이는 것 — 저장은 허용하되 매번 조건부 요청을 보내게 해서, 안 바뀐 파일은 304 로 본문 없이 끝나고 배포로 바뀐 파일은 즉시 반영된다. 헤더를 아예 두지
 * 않으면 브라우저가 임의로 캐시 기간을 정해(휴리스틱) 배포 후 낡은 파일이 남을 수 있다.
 *
 * <p>{@code spring.web.resources.cache.cachecontrol.*} 로도 시도했으나 이 앱에서는 헤더가 나가지 않아 필터로 붙인다.
 */
@Configuration
public class GateStaticResourceSecurityConfig {

  private static final String[] STATIC_PATTERNS = {
    "/main.css", "/favicon.ico", "/css/**", "/js/**", "/fonts/**"
  };

  private static final String[] STATIC_URL_PATTERNS = {
    "/main.css", "/favicon.ico", "/css/*", "/js/*", "/js/*/*", "/js/*/*/*", "/fonts/*"
  };

  @Bean
  WebSecurityCustomizer gateStaticResourceWebSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers(STATIC_PATTERNS);
  }

  @Bean
  FilterRegistrationBean<OncePerRequestFilter> gateStaticResourceCacheControlFilter(
      @Value("${server.servlet.session.cookie.name:SESSION}") String sessionCookieName) {
    OncePerRequestFilter filter =
        new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
              throws ServletException, IOException {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            // 이 요청들에서는 세션 쿠키를 아래로 넘기지 않는다.
            //
            // 이 앱의 세션 저장소는 요청마다 user API 를 원격 호출해 세션을 검증한다
            // (ApiSessionRepository#findById). Spring Session 은 응답을 커밋할 때 세션을 지연
            // 로드하므로, 세션을 쓰지도 않는 css/js/폰트 요청까지 그 원격 호출을 한 번씩 했다.
            // 실측(같은 파일 0.9KB, 연결 재사용): 쿠키 없음 1.4ms / 세션 쿠키 20.3ms.
            // 위 no-cache 설정 때문에 페이지를 옮길 때마다 이 비용을 14번 냈다.
            //
            // 쿠키를 가리면 세션 id 가 해석되지 않아 조회 자체가 일어나지 않는다. 이 경로들은 이미
            // 보안 필터 체인에서도 빠져 있어(위 WebSecurityCustomizer) 세션이 필요 없다.
            // HttpSessionIdResolver 를 갈아끼우는 방법도 시도했으나, 그러면 Spring Boot 이
            // CookieSerializer 를 만들지 않아 쿠키 이름이 기본값으로 돌아가 로그인이 풀렸다(실측).
            filterChain.doFilter(
                new SessionCookieHiddenRequest(request, sessionCookieName), response);
          }
        };
    FilterRegistrationBean<OncePerRequestFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns(STATIC_URL_PATTERNS);
    registration.setOrder(Integer.MIN_VALUE);
    return registration;
  }

  /** 세션 쿠키만 감춘 요청. 나머지 쿠키와 헤더는 그대로 둔다. */
  private static final class SessionCookieHiddenRequest extends HttpServletRequestWrapper {

    private final String sessionCookieName;

    private SessionCookieHiddenRequest(HttpServletRequest request, String sessionCookieName) {
      super(request);
      this.sessionCookieName = sessionCookieName;
    }

    @Override
    public Cookie[] getCookies() {
      Cookie[] cookies = super.getCookies();
      if (cookies == null || cookies.length == 0) {
        return cookies;
      }
      return Arrays.stream(cookies)
          .filter(cookie -> !sessionCookieName.equals(cookie.getName()))
          .toArray(Cookie[]::new);
    }
  }
}
