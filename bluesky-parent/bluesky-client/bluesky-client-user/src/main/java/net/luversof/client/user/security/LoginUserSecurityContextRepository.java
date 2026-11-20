package net.luversof.client.user.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.luversof.client.user.domain.LoginUser;

/**
 * 공유 세션의 {@link LoginUser} 하나로 로그인 상태를 읽고 쓴다. SecurityContext 는 세션에 넣지 않는다.
 *
 * <p>로그인은 web-user 만 하고 {@link #saveLoginUser} 로 남긴다. 다른 인증 객체(로그인 중의 OAuth2 토큰)는 저장하지 않는다.
 */
public class LoginUserSecurityContextRepository implements SecurityContextRepository {

  public static final String LOGIN_USER_ATTRIBUTE = "BLUESKY_LOGIN_USER";

  public static void saveLoginUser(HttpServletRequest request, LoginUser loginUser) {
    request.getSession().setAttribute(LOGIN_USER_ATTRIBUTE, loginUser);
  }

  @Override
  @SuppressWarnings("deprecation")
  public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    LoginUser loginUser = loginUser(requestResponseHolder.getRequest());
    if (loginUser != null) {
      context.setAuthentication(new LoginUserAuthenticationToken(loginUser));
    }
    return context;
  }

  @Override
  public void saveContext(
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    Authentication authentication = context.getAuthentication();
    // 로그아웃처럼 인증을 비울 때만 지운다. 로그인 사용자는 이미 세션의 LoginUser 에서 왔다.
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      HttpSession session = request.getSession(false);
      if (session != null && session.getAttribute(LOGIN_USER_ATTRIBUTE) != null) {
        session.removeAttribute(LOGIN_USER_ATTRIBUTE);
      }
    }
  }

  @Override
  public boolean containsContext(HttpServletRequest request) {
    return loginUser(request) != null;
  }

  private static LoginUser loginUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null && session.getAttribute(LOGIN_USER_ATTRIBUTE) instanceof LoginUser u
        ? u
        : null;
  }
}
