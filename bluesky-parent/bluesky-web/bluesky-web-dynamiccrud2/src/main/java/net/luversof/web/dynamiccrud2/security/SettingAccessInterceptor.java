package net.luversof.web.dynamiccrud2.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 설정 화면(/setting/**) 전체를 ROLE_ADMIN(MASTER 포함) 전용으로 막는다. */
@Component
public class SettingAccessInterceptor implements HandlerInterceptor {

  public static final String PATH_PATTERN = "/setting/**";

  private final MenuAuthorizer menuAuthorizer;

  public SettingAccessInterceptor(MenuAuthorizer menuAuthorizer) {
    this.menuAuthorizer = menuAuthorizer;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    menuAuthorizer.checkManageSettings();
    return true;
  }
}
