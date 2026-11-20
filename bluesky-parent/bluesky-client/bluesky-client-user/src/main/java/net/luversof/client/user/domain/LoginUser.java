package net.luversof.client.user.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 로그인한 사용자. 앞단 서버가 공유하는 세션에는 이것만 담는다.
 *
 * <p>예전엔 Spring Security 의 SecurityContext(OAuth2AuthenticationToken) 를 통째로 Java 직렬화해 담아서, 앱마다
 * Spring Security 버전이 어긋나면 다른 앱이 세션을 못 읽었다. 이 레코드는 java.* 타입만 쓰므로 그런 결합이 없다. 필드를 지우거나 타입을 바꾸면 이미
 * 로그인한 세션을 못 읽으니 더하기만 한다.
 *
 * @param authorities user API 가 돌려준 역할. ROLE_USER 는 인증 객체를 만들 때 더한다.
 */
public record LoginUser(
    String id,
    String username,
    String provider,
    String email,
    String avatarUrl,
    List<String> authorities)
    implements Serializable {

  private static final long serialVersionUID = 1L;
}
