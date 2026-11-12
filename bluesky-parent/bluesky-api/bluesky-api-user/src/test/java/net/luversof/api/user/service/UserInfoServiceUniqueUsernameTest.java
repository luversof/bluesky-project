package net.luversof.api.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.luversof.api.user.domain.UserInfo;

/** 대체 username 은 UserInfo.username VARCHAR(50) 안에 든다(2026-11-08 감사 - 39 자 GitHub 로그인이 가입하지 못했다). */
class UserInfoServiceUniqueUsernameTest {

  private static final String LOGIN_39 = "a".repeat(39);

  @Test
  void 겹치지_않으면_username_provider() {
    assertThat(UserInfoService.uniqueUsername("octo", "github", "123", name -> Optional.empty()))
        .isEqualTo("octo_github");
  }

  @Test
  void 겹치면_provider_providerId_로_50자_안에_든다() {
    String name =
        UserInfoService.uniqueUsername(
            LOGIN_39, "github", "123456789", n -> Optional.of(new UserInfo()));

    assertThat(name).isEqualTo("github_123456789");
    assertThat(name.length()).isLessThanOrEqualTo(50);
  }
}
