package net.luversof.web.gate.config;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.gate.util.CspNonceHolder;

/**
 * 요청마다 CSP nonce 를 생성해 CspNonceHolder 에 담고, Report-Only CSP 헤더를 내려준다.
 *
 * <p>우선 Report-Only 로 적용해 위반 사항을 브라우저 콘솔에서 관찰한 뒤, 문제가 없으면 CONTENT_SECURITY_POLICY_REPORT_ONLY 를
 * enforcing 헤더(Content-Security-Policy)로 전환한다.
 *
 * <p>전제 조건: 정책에 'unsafe-eval' 이 없으므로 htmx 의 eval 기반 기능(hx-on:, hx-vals="js:")을 쓰면 안 된다 — 대신
 * common.ts 의 데이터 속성 위임([data-reload-after-request], [data-page-param-from-query])을 사용할 것. 인라인
 * 스크립트는 _components/ui/script.jte 래퍼로만 작성한다 (bare &lt;script&gt; 는 enforcing 시 차단됨). htmx 스왑
 * fragment 의 nonce 재부여 트레이드오프는 defaultLayout.jte 의 htmx-config 주석 참고.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class CspNonceFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "Content-Security-Policy-Report-Only";
  private static final String POLICY_TEMPLATE =
      "default-src 'self'; script-src 'self' 'nonce-%s'; style-src 'self' 'unsafe-inline'; "
          + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
          + "object-src 'none'; base-uri 'self'";

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // 정적 리소스에는 CSP 헤더가 의미 없으므로 제외한다.
    String path = request.getRequestURI();
    return path.startsWith("/js/")
        || path.startsWith("/images/")
        || path.startsWith("/webjars/")
        || path.equals("/main.css")
        || path.equals("/favicon.ico");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    byte[] bytes = new byte[16];
    secureRandom.nextBytes(bytes);
    String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    CspNonceHolder.set(nonce);
    response.setHeader(HEADER_NAME, POLICY_TEMPLATE.formatted(nonce));
    try {
      filterChain.doFilter(request, response);
    } finally {
      CspNonceHolder.clear();
    }
  }
}
