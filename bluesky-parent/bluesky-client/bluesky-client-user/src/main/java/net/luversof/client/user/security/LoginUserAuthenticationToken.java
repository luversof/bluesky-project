package net.luversof.client.user.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import net.luversof.client.user.domain.LoginUser;

/** 세션의 {@link LoginUser} 로 요청마다 만드는 인증 객체. 세션에는 저장하지 않는다. */
public class LoginUserAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final LoginUser loginUser;

  public LoginUserAuthenticationToken(LoginUser loginUser) {
    super(authorities(loginUser.authorities()));
    this.loginUser = loginUser;
    setAuthenticated(true);
  }

  /** 로그인 사용자는 모두 ROLE_USER 를 갖는다(게이트의 {@code @BlueskyPreAuthorize}). user API 의 역할을 더한다. */
  static Set<GrantedAuthority> authorities(Collection<String> userAuthorities) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    if (userAuthorities != null) {
      userAuthorities.stream()
          .filter(a -> a != null && !a.isBlank())
          .forEach(a -> authorities.add(new SimpleGrantedAuthority(a.strip())));
    }
    return authorities;
  }

  @Override
  public LoginUser getPrincipal() {
    return loginUser;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public String getName() {
    return loginUser.username();
  }
}
