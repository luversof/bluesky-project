package net.luversof.client.user.config;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Common OAuth2 Login Success Handler for all bluesky-web modules Saves user information to
 * bluesky-api-user after successful OAuth2 login
 */
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private OAuth2AuthorizedClientRepository authorizedClientRepository;

  /** 세션 쿠키 도메인(예: bluesky.local) - 로그인 뒤 돌아갈 주소는 이 도메인 아래만 허용한다. */
  private String cookieDomain;

  @Autowired
  public void setEnvironment(Environment environment) {
    this.cookieDomain = environment.getProperty("server.servlet.session.cookie.domain");
  }

  @Autowired
  public void setAuthorizedClientRepository(
      OAuth2AuthorizedClientRepository authorizedClientRepository) {
    this.authorizedClientRepository = authorizedClientRepository;
  }

  public OAuth2LoginSuccessHandler() {
    setDefaultTargetUrl("/");
    setAlwaysUseDefaultTargetUrl(false);
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
      String registrationId = oauthToken.getAuthorizedClientRegistrationId();

      authorizedClientRepository.loadAuthorizedClient(registrationId, authentication, request);
    }

    if (request.getSession() != null) {
      String redirectUrl = (String) request.getSession().getAttribute("redirectUrl");
      if (redirectUrl != null) {
        request.getSession().removeAttribute("redirectUrl");
        // 우리 사이트 주소만 따라간다 - 예전엔 /login?redirectUrl=https://evil.example 로 진짜 로그인 뒤 남의 사이트로
        // 보냈다(2026-10-28 감사)
        if (isAllowedRedirect(redirectUrl, request.getServerName(), cookieDomain)) {
          getRedirectStrategy().sendRedirect(request, response, redirectUrl);
          return;
        }
      }
    }

    super.onAuthenticationSuccess(request, response, authentication);
  }

  /**
   * 로그인 뒤 돌아갈 주소로 허용하는가 - 상대 경로('/...'), 같은 호스트, 세션 쿠키 도메인 아래 호스트(http · https)만.
   *
   * @param url 돌아갈 주소
   * @param requestHost 지금 요청의 호스트
   * @param cookieDomain 세션 쿠키 도메인(없으면 같은 호스트만)
   * @return 허용하면 true
   */
  static boolean isAllowedRedirect(String url, String requestHost, String cookieDomain) {
    URI uri;
    try {
      uri = URI.create(url.trim());
    } catch (IllegalArgumentException e) {
      return false;
    }
    if (uri.getScheme() == null && uri.getRawAuthority() == null) {
      // '//evil.example' 은 authority 가 있어 여기 오지 않는다
      return url.trim().startsWith("/");
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (host == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
      return false;
    }
    host = host.toLowerCase(Locale.ROOT);
    if (host.equalsIgnoreCase(requestHost)) {
      return true;
    }
    if (cookieDomain == null || cookieDomain.isBlank()) {
      return false;
    }
    String domain = cookieDomain.trim().toLowerCase(Locale.ROOT);
    if (domain.startsWith(".")) {
      domain = domain.substring(1);
    }
    return host.equals(domain) || host.endsWith("." + domain);
  }
}
