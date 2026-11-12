package net.luversof.client.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

/**
 * 게이트의 {@code @BlueskyPreAuthorize}(hasRole('USER'))를 켠 뒤(2026-11-13): 로그인 사용자는 ROLE_USER 를 갖고,
 * htmx · JSON 요청은 로그인 화면으로 302 대신 401 을 받는다.
 */
class ClientUserAuthorizationTest {

  @Test
  void GitHub_로그인_사용자는_ROLE_USER_를_갖는다() {
    var github =
        new DefaultOAuth2User(
            List.of(new OAuth2UserAuthority(Map.of("login", "octo"))),
            Map.of("login", "octo"),
            "login");

    assertThat(CustomOAuth2UserService.withUserRole(github))
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_USER", "OAUTH2_USER");
  }

  @Test
  void htmx_와_JSON_요청은_상태코드를_기대한다() {
    var htmx = new MockHttpServletRequest();
    htmx.addHeader("HX-Request", "true");
    var json = new MockHttpServletRequest();
    json.setContentType("application/json");
    var fetchJson = new MockHttpServletRequest();
    fetchJson.addHeader("Accept", "application/json");

    assertThat(ClientUserSecurityConfig.expectsStatus(htmx)).isTrue();
    assertThat(ClientUserSecurityConfig.expectsStatus(json)).isTrue();
    assertThat(ClientUserSecurityConfig.expectsStatus(fetchJson)).isTrue();
  }

  @Test
  void 화면_이동은_로그인으로_리다이렉트한다() {
    var page = new MockHttpServletRequest();
    page.addHeader("Accept", "text/html,application/xhtml+xml,application/json;q=0.9");

    assertThat(ClientUserSecurityConfig.expectsStatus(page)).isFalse();
    assertThat(ClientUserSecurityConfig.expectsStatus(new MockHttpServletRequest())).isFalse();
  }
}
