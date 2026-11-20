package net.luversof.client.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import net.luversof.client.user.domain.LoginUser;

/** 공유 세션에는 LoginUser 만 담고, SecurityContext 는 요청마다 그것으로 만든다(2026-11-20). */
class LoginUserSecurityContextRepositoryTest {

  private static final LoginUser LOGIN_USER =
      new LoginUser(
          "00000000-0000-0000-0000-000000000001", "octo", "github", null, null, List.of());

  private final LoginUserSecurityContextRepository repository =
      new LoginUserSecurityContextRepository();

  @SuppressWarnings("deprecation")
  private Object principal(MockHttpServletRequest request) {
    var authentication =
        repository
            .loadContext(new HttpRequestResponseHolder(request, new MockHttpServletResponse()))
            .getAuthentication();
    return authentication == null ? null : authentication.getPrincipal();
  }

  @Test
  void 세션의_로그인_사용자로_인증한다() {
    var request = new MockHttpServletRequest();
    LoginUserSecurityContextRepository.saveLoginUser(request, LOGIN_USER);

    assertThat(principal(request)).isEqualTo(LOGIN_USER);
    assertThat(repository.containsContext(request)).isTrue();
  }

  @Test
  void 세션이_없으면_비로그인이고_세션을_만들지_않는다() {
    var request = new MockHttpServletRequest();

    assertThat(principal(request)).isNull();
    assertThat(request.getSession(false)).isNull();
  }

  @Test
  void 로그아웃하면_세션에서_지운다() {
    var request = new MockHttpServletRequest();
    LoginUserSecurityContextRepository.saveLoginUser(request, LOGIN_USER);

    repository.saveContext(
        SecurityContextHolder.createEmptyContext(), request, new MockHttpServletResponse());

    assertThat(principal(request)).isNull();
  }

  @Test
  void 다른_인증_객체는_세션에_넣지_않는다() {
    var request = new MockHttpServletRequest();

    repository.saveContext(
        new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated("octo", null, List.of())),
        request,
        new MockHttpServletResponse());

    assertThat(request.getSession(false)).isNull();
  }
}
