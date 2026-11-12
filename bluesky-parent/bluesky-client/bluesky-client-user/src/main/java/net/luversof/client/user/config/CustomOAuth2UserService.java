package net.luversof.client.user.config;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

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

    UserInfoApiClient.UserInfoResponse userInfo = null;
    try {
      // Save or Update user info
      var request =
          new UserInfoApiClient.SaveOAuth2UserRequest(
              provider, providerId, username, email, avatarUrl);

      userInfo = userInfoApiClient.saveOAuth2User(request);
    } catch (Exception e) {
      // Log error but allow login to proceed
      log.warn("saveOAuth2User failed. provider: {}", provider, e);
    }

    Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
    if (userInfo != null) {
      attributes.put("userInfo", userInfo);
    }

    String userNameAttributeName =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

    return new DefaultOAuth2User(withUserRole(oauth2User), attributes, userNameAttributeName);
  }

  /**
   * GitHub 가 주는 권한(OAUTH2_USER, SCOPE_*)에 ROLE_USER 를 더한다. 게이트의 {@code @BlueskyPreAuthorize} 가
   * {@code hasRole('USER')} 라, 이게 없으면 로그인한 사용자도 막힌다.
   */
  static Set<GrantedAuthority> withUserRole(OAuth2User oauth2User) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>(oauth2User.getAuthorities());
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    return authorities;
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
