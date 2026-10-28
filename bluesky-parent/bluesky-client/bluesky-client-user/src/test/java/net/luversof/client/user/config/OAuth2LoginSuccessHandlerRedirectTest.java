package net.luversof.client.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 로그인 뒤 돌아갈 주소는 우리 사이트만(2026-10-28 감사 - 예전엔 아무 주소로나 보냈다). */
class OAuth2LoginSuccessHandlerRedirectTest {

  private static final String HOST = "user.web.bluesky.local";
  private static final String DOMAIN = "bluesky.local";

  @Test
  void 우리_사이트_주소는_허용한다() {
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("/stock", HOST, DOMAIN)).isTrue();
    assertThat(
            OAuth2LoginSuccessHandler.isAllowedRedirect(
                "https://gate.web.bluesky.local/stock?x=1", HOST, DOMAIN))
        .isTrue();
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("https://" + HOST + "/", HOST, null))
        .as("쿠키 도메인이 없어도 같은 호스트는 된다")
        .isTrue();
  }

  @Test
  void 남의_사이트와_꼼수_주소는_막는다() {
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("https://evil.example/", HOST, DOMAIN))
        .isFalse();
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("//evil.example/", HOST, DOMAIN))
        .isFalse();
    assertThat(
            OAuth2LoginSuccessHandler.isAllowedRedirect("https://evilbluesky.local/", HOST, DOMAIN))
        .as("도메인 이름 끝만 같은 남의 호스트")
        .isFalse();
    assertThat(
            OAuth2LoginSuccessHandler.isAllowedRedirect(
                "https://bluesky.local.evil.example/", HOST, DOMAIN))
        .isFalse();
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("javascript:alert(1)", HOST, DOMAIN))
        .isFalse();
    assertThat(OAuth2LoginSuccessHandler.isAllowedRedirect("/\\evil.example", HOST, DOMAIN))
        .as("브라우저가 '//' 로 읽는 역슬래시")
        .isFalse();
    assertThat(
            OAuth2LoginSuccessHandler.isAllowedRedirect(
                "https://gate.web.bluesky.local/", HOST, null))
        .as("쿠키 도메인이 없으면 다른 호스트는 막는다")
        .isFalse();
  }
}
