package net.luversof.client.user.util;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import io.github.luversof.boot.context.ApplicationContextUtil;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.UserInfoResponse;

public final class UserUtil {

  private static final Logger log = LoggerFactory.getLogger(UserUtil.class);

  private UserUtil() {}

  public static UUID getUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    // OAuth2 로그인 경우 UserInfo 조회
    if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
      OAuth2User principal = oauth2Auth.getPrincipal();
      if (principal.getAttribute("userInfo") instanceof UserInfoResponse userInfo) {
        return userInfo.id() != null ? UUID.fromString(userInfo.id()) : null;
      }
      return getUserIdFromUserInfo(oauth2Auth);
    }

    return null;
  }

  /**
   * 로그인한 사용자 id - 없으면 401.
   *
   * <p>게이트의 @BlueskyPreAuthorize 는 메서드 보안이 꺼져 있어 아무것도 막지 않는다. 그래서 쓰기 경로는 이걸로 직접 확인한다 - 예전엔 비로그인 요청이
   * getUserId().toString() 에서 NPE 500 이 나거나, null 사용자로 API 까지 갔다(2026-10-28 감사).
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

  private static UUID getUserIdFromUserInfo(OAuth2AuthenticationToken oauth2Auth) {
    OAuth2User principal = oauth2Auth.getPrincipal();
    String registrationId = oauth2Auth.getAuthorizedClientRegistrationId();
    String provider = normalizeProvider(registrationId);

    // GitHub의 id 타입 Integer 대응
    Object idAttr = principal.getAttribute("id");
    if (idAttr == null) {
      log.warn("id attribute is null for provider: {}", provider);
      return null;
    }

    String providerId = idAttr.toString();

    try {

      var userApiClient =
          ApplicationContextUtil.getApplicationContext().getBean(UserInfoApiClient.class);
      UserInfoApiClient.UserInfoResponse userInfo =
          userApiClient.findByProviderAndProviderId(provider, providerId);

      if (userInfo == null || userInfo.id() == null) {
        log.warn("UserInfo not found: provider={}, providerId={}", provider, providerId);
        return null;
      }

      return UUID.fromString(userInfo.id());

    } catch (Exception e) {
      log.error("Failed to get UserInfo: provider={}, providerId={}", provider, providerId, e);
      return null;
    }
  }

  /** Provider 이름 정규화 (github-local -> github) */
  private static String normalizeProvider(String provider) {
    if (provider == null) {
      return null;
    }
    // github-local, github-dev 통일
    if (provider.startsWith("github")) {
      return "github";
    }
    // kakao-local, kakao-dev 통일
    if (provider.startsWith("kakao")) {
      return "kakao";
    }
    return provider;
  }

  public static boolean isAuthenticated() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication
            instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
  }

  public static String getUsername() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    // OAuth2 로그인
    if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
      OAuth2User principal = oauth2Auth.getPrincipal();
      if (principal.getAttribute("userInfo") instanceof UserInfoResponse userInfo) {
        return userInfo.username();
      }

      String username = principal.getAttribute("preferred_username");
      if (username == null) {
        username = principal.getAttribute("name");
      }
      return username;
    }

    // 기존 방식
    return authentication.getName();
  }
}
