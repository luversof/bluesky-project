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
 * <p>주식 화면(/stock)은 위반 0건을 확인해 enforcing 으로 전환했고, 나머지 경로는 아직 Report-Only 다.
 *
 * <p>전제 조건: 정책에 'unsafe-eval' 이 없으므로 htmx 의 eval 기반 기능(hx-on:, hx-vals="js:")을 쓰면 안 된다 — 대신
 * common.ts 의 데이터 속성 위임([data-reload-after-request], [data-page-param-from-query])을 사용할 것. 인라인
 * 스크립트는 _components/ui/script.jte 래퍼로만 작성한다 (bare &lt;script&gt; 는 enforcing 시 차단됨). htmx 스왑
 * fragment 의 nonce 재부여 트레이드오프는 defaultLayout.jte 의 htmx-config 주석 참고.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class CspNonceFilter extends OncePerRequestFilter {

  private static final String REPORT_ONLY_HEADER = "Content-Security-Policy-Report-Only";
  private static final String ENFORCE_HEADER = "Content-Security-Policy";

  /**
   * 이 접두어의 요청은 정책을 강제한다. 나머지는 아직 Report-Only.
   *
   * <p>실측 근거: 주식 화면 7개(/stock, analytics, dividend, trade, account, item, simulator)를 브라우저로 열고
   * details 펼치기·기간 버튼(htmx 스왑)까지 조작해 securitypolicyviolation 을 수집한 결과 위반 0건이었다. 스왑으로 들어온 조각의 인라인
   * 스크립트는 htmx-config 의 inlineScriptNonce 가 페이지 nonce 를 재부여하므로 막히지 않는다(실측: 스왑 후 조각 스크립트의 행 선택이 정상
   * 동작).
   *
   * <p>전체 전환은 아직 못 한다 — nonce 없는 인라인 스크립트가 PoE 템플릿 7개에 남아 있다(check-jte-script-nonce 로 확인). 그쪽이 정리되면
   * 이 분기를 지우고 항상 enforcing 으로 바꾸면 된다.
   */
  private static final String ENFORCED_PATH_PREFIX = "/stock";

  private static final String POLICY_TEMPLATE =
      "default-src 'self'; script-src 'self' 'nonce-%s'; style-src 'self' 'unsafe-inline'; "
          + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
          // frame-ancestors 는 X-Frame-Options: DENY 의 현대식 대응이고(둘 다 두면 최신 브라우저는 이쪽을 본다),
          // form-action 은 폼 전송 대상을 자기 출처로 묶는다. 실측: 템플릿의 <form> 25개 모두 상대 경로라 영향 없음.
          + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'";

  /** 외부로 나가는 요청에 전체 URL(쿼리 포함)이 새지 않게 한다. 크로스 오리진에는 출처만 보낸다. */
  private static final String REFERRER_POLICY = "strict-origin-when-cross-origin";

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
    String path = request.getRequestURI();
    boolean enforce = path != null && path.startsWith(ENFORCED_PATH_PREFIX);
    response.setHeader(
        enforce ? ENFORCE_HEADER : REPORT_ONLY_HEADER, POLICY_TEMPLATE.formatted(nonce));
    response.setHeader("Referrer-Policy", REFERRER_POLICY);
    try {
      filterChain.doFilter(request, response);
    } finally {
      CspNonceHolder.clear();
    }
  }
}
