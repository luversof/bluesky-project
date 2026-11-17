package net.luversof.web.dynamiccrud2.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;

/**
 * 메뉴 · 작업 권한 판정. 공통 보안 설정이 모든 요청을 permitAll 로 두므로 dynamiccrud2 가 직접 막는다.
 *
 * <ul>
 *   <li>메뉴 권한 null = 로그인한 사용자 누구나
 *   <li>작업 권한 null = 메뉴 권한을 따른다
 *   <li>역할 계층 ROLE_MASTER &gt; ROLE_ADMIN &gt; ROLE_USER
 * </ul>
 */
@Component
public class MenuAuthorizer {

  public static final String SETTING_AUTHORITY = "ROLE_ADMIN";

  // 빈으로 등록하지 않는다(다른 모듈의 Spring Security 판정에 끼어들지 않게)
  private final RoleHierarchy roleHierarchy =
      RoleHierarchyImpl.fromHierarchy("ROLE_MASTER > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");

  /** 목록 표시 · 조회 · 엑셀 */
  public boolean canRead(Menu menu) {
    return allows(menu.getAuthority());
  }

  /** 입력 · 수정 · 삭제 실행 */
  public boolean canRun(Menu menu, MenuQuery query) {
    return allows(query.getAuthority() != null ? query.getAuthority() : menu.getAuthority());
  }

  /** 설정 화면 전체 */
  public boolean canManageSettings() {
    return allows(SETTING_AUTHORITY);
  }

  public boolean isLoggedIn() {
    return currentAuthentication() != null;
  }

  public void checkLoggedIn() {
    check(isLoggedIn());
  }

  public void checkRead(Menu menu) {
    check(canRead(menu));
  }

  public void checkRun(Menu menu, MenuQuery query) {
    check(canRun(menu, query));
  }

  public void checkManageSettings() {
    check(canManageSettings());
  }

  private boolean allows(String authority) {
    var authentication = currentAuthentication();
    if (authentication == null) {
      return false;
    }
    if (authority == null || authority.isBlank()) {
      return true;
    }
    return roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities()).stream()
        .anyMatch(granted -> authority.equals(granted.getAuthority()));
  }

  /** 로그인한 사용자. 익명 토큰은 로그인으로 보지 않는다 */
  private static Authentication currentAuthentication() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    return authentication;
  }

  /** 거부 응답(로그인 이동 · 401 · 403)은 AccessDeniedAdvice 가 만든다 */
  private static void check(boolean allowed) {
    if (!allowed) {
      throw new AccessDeniedException("dynamiccrud2 access denied");
    }
  }
}
