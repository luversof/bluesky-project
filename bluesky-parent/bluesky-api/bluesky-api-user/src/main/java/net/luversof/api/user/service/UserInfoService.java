package net.luversof.api.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.repository.UserInfoRepository;

@Service
public class UserInfoService {

  private UserInfoRepository userInfoRepository;

  private JdbcClient userJdbcClient;

  @Autowired
  public void setUserInfoRepository(UserInfoRepository userInfoRepository) {
    this.userInfoRepository = userInfoRepository;
  }

  @Autowired
  public void setUserJdbcClient(@Qualifier("userJdbcClient") JdbcClient userJdbcClient) {
    this.userJdbcClient = userJdbcClient;
  }

  /**
   * OAuth2 로그인 사용자에게 더할 역할(예: ROLE_ADMIN). "UserAuthority" 테이블에 직접 넣어 관리한다. 로그인할 때 세션에 들어가므로 바꾸면 다시
   * 로그인해야 반영된다.
   */
  public List<String> findAuthorities(UUID userInfoId) {
    return userJdbcClient
        .sql("SELECT \"authority\" FROM \"UserAuthority\" WHERE \"userInfo_id\" = :id")
        .param("id", userInfoId)
        .query(String.class)
        .list();
  }

  public UserInfo save(UserInfo userInfo) {
    return userInfoRepository.save(userInfo);
  }

  public Optional<UserInfo> findById(UUID id) {
    return userInfoRepository.findById(id);
  }

  public List<UserInfo> findByIdIn(List<UUID> ids) {
    return userInfoRepository.findByIdIn(ids);
  }

  public Optional<UserInfo> findByUsername(String username) {
    return userInfoRepository.findByUsername(username);
  }

  public Optional<UserInfo> findByProviderAndProviderId(String provider, String providerId) {
    return userInfoRepository.findByProviderAndProviderId(provider, providerId);
  }

  /** Provider 이름을 정규화 (github-local → github) */
  private String normalizeProvider(String provider) {
    if (provider == null) {
      return null;
    }
    // github-local, github-dev 등을 모두 github로 통일
    if (provider.startsWith("github")) {
      return "github";
    }
    // kakao-local, kakao-dev 등을 모두 kakao로 통일
    if (provider.startsWith("kakao")) {
      return "kakao";
    }
    return provider;
  }

  /** OAuth2 로그인 사용자 정보 저장 (신규 생성 또는 업데이트) */
  public UserInfo saveOAuth2User(
      String provider, String providerId, String username, String email, String avatarUrl) {
    // Provider 정규화 (github-local → github)
    provider = normalizeProvider(provider);

    // 기존 사용자 확인
    Optional<UserInfo> existingUser = findByProviderAndProviderId(provider, providerId);

    if (existingUser.isPresent()) {
      // 기존 사용자 업데이트 - email과 avatarUrl만 갱신
      UserInfo userInfo = existingUser.get();
      userInfo.setEmail(email);
      userInfo.setAvatarUrl(avatarUrl);
      return save(userInfo);
    }

    String uniqueUsername = uniqueUsername(username, provider, providerId, this::findByUsername);

    UserInfo userInfo = new UserInfo();
    // setId()를 호출하지 않음 - BeforeConvertCallback이 자동으로 UUID 생성
    userInfo.setProvider(provider);
    userInfo.setProviderId(providerId);
    userInfo.setUsername(uniqueUsername);
    userInfo.setEmail(email);
    userInfo.setAvatarUrl(avatarUrl);

    return save(userInfo);
  }

  /**
   * 신규 사용자 username: {@code <username>_<provider>}, 이미 있으면 {@code <provider>_<providerId>}. 대체 이름은
   * (provider, providerId) 유니크라 겹치지 않는다. 예전 대체 이름 {@code <username>_<provider>_<providerId>} 는
   * GitHub 로그인 39 자면 UserInfo.username VARCHAR(50) 을 넘어 첫 로그인이 실패했다(2026-11-08 감사).
   */
  static String uniqueUsername(
      String username,
      String provider,
      String providerId,
      Function<String, Optional<UserInfo>> findByUsername) {
    String preferred = username + "_" + provider;
    return findByUsername.apply(preferred).isPresent() ? provider + "_" + providerId : preferred;
  }
}
