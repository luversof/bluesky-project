package net.luversof.client.user.util;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.client.user.domain.LoginUser;

public final class UserUtil {

  private UserUtil() {}

  /** 로그인 사용자. 로그인하지 않았으면 null. */
  public static LoginUser getLoginUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser
        ? loginUser
        : null;
  }

  public static UUID getUserId() {
    LoginUser loginUser = getLoginUser();
    return loginUser == null || loginUser.id() == null ? null : UUID.fromString(loginUser.id());
  }

  /**
   * 로그인한 사용자 id - 없으면 401.
   *
   * <p>게이트의 @BlueskyPreAuthorize 가 먼저 막지만(2026-11-13 부터, 그 전엔 메서드 보안이 꺼져 실효가 없었다) 어노테이션을 빠뜨린 엔드포인트도
   * 있으니 쓰기 경로는 이걸로 한 번 더 확인한다 - 예전엔 비로그인 요청이 getUserId().toString() 에서 NPE 500 이 나거나, null 사용자로 API
   * 까지 갔다(2026-10-28 감사).
   *
   * @return 사용자 id
   */
  public static UUID requireUserId() {
    UUID userId = getUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return userId;
  }

  public static boolean isAuthenticated() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  public static String getUsername() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    // LoginUserAuthenticationToken 은 username 을 이름으로 쓴다.
    return authentication.getName();
  }
}
