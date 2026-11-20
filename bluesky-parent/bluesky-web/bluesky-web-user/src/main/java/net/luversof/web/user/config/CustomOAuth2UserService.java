package net.luversof.web.user.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import net.luversof.client.user.domain.LoginUser;
import net.luversof.client.user.httpexchange.UserInfoApiClient;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  /** 로그인 요청 안에서 {@link OAuth2LoginSuccessHandler} 에 넘기는 속성 이름 */
  static final String LOGIN_USER_ATTRIBUTE = "loginUser";

  private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

  private final UserInfoApiClient userInfoApiClient;
  private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

  public CustomOAuth2UserService(UserInfoApiClient userInfoApiClient) {
    this.userInfoApiClient = userInfoApiClient;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = delegate.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    String provider = normalizeProvider(registrationId);

    // GitHub/Kakao OAuth2User attributes
    Object idAttr = oauth2User.getAttribute("id");
    String providerId = idAttr != null ? idAttr.toString() : null;
    String username = oauth2User.getAttribute("login"); // GitHub
    if (username == null) {
      username = oauth2User.getAttribute("preferred_username"); // Keycloak etc
    }
    if (username == null) {
      username = oauth2User.getName();
    }

    String email = oauth2User.getAttribute("email");
    String avatarUrl = oauth2User.getAttribute("avatar_url");

    UserInfoApiClient.UserInfoResponse userInfo;
    try {
      // Save or Update user info
      var request =
          new UserInfoApiClient.SaveOAuth2UserRequest(
              provider, providerId, username, email, avatarUrl);

      userInfo = userInfoApiClient.saveOAuth2User(request);
    } catch (Exception e) {
      log.warn("saveOAuth2User failed. provider: {}", provider, e);
      userInfo = null;
    }
    // 사용자 id 없이 로그인시키면 앞단 서버가 모두 401 을 낸다(예전엔 ROLE_USER 만 달고 로그인됐다).
    if (userInfo == null || userInfo.id() == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("user_save_failed"), "user API 에 사용자를 저장하지 못했다");
    }

    Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
    attributes.put(
        LOGIN_USER_ATTRIBUTE,
        new LoginUser(
            userInfo.id(),
            userInfo.username(),
            userInfo.provider(),
            userInfo.email(),
            userInfo.avatarUrl(),
            userInfo.authorities() == null ? List.of() : List.copyOf(userInfo.authorities())));

    String userNameAttributeName =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

    // 이 인증 객체는 로그인 요청 안에서만 쓰인다. 세션에는 OAuth2LoginSuccessHandler 가 LoginUser 만 남긴다.
    return new DefaultOAuth2User(oauth2User.getAuthorities(), attributes, userNameAttributeName);
  }

  private String normalizeProvider(String provider) {
    if (provider == null) {
      return null;
    }
    if (provider.startsWith("github")) {
      return "github";
    }
    if (provider.startsWith("kakao")) {
      return "kakao";
    }
    return provider;
  }
}
