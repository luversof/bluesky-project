package net.luversof.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import net.luversof.api.user.controller.UserInfoController.OAuth2UserRequest;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;

/** 로그인 때 부르는 /oauth2 는 "UserAuthority" 역할을 같이 돌려주고 password 는 내보내지 않는다(2026-11-17). */
class UserInfoControllerOAuth2Test {

  private static final UUID ID = UUID.randomUUID();

  @Test
  void 역할을_같이_돌려준다() {
    var service =
        new UserInfoService() {
          @Override
          public UserInfo saveOAuth2User(
              String provider, String providerId, String username, String email, String avatarUrl) {
            var userInfo = new UserInfo();
            userInfo.setId(ID);
            userInfo.setUsername(username + "_" + provider);
            userInfo.setProvider(provider);
            userInfo.setProviderId(providerId);
            userInfo.setPassword("secret");
            return userInfo;
          }

          @Override
          public List<String> findAuthorities(UUID userInfoId) {
            return userInfoId.equals(ID) ? List.of("ROLE_ADMIN") : List.of();
          }
        };
    var controller = new UserInfoController();
    ReflectionTestUtils.setField(controller, "userInfoService", service);

    var response =
        controller.saveOAuth2User(new OAuth2UserRequest("github", "1", "octo", null, null));

    assertThat(response.id()).isEqualTo(ID.toString());
    assertThat(response.username()).isEqualTo("octo_github");
    assertThat(response.authorities()).containsExactly("ROLE_ADMIN");
    assertThat(response.toString()).doesNotContain("secret");
  }
}
