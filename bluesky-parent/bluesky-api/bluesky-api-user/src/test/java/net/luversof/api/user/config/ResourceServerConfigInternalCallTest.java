package net.luversof.api.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Session and user-creation paths need the internal token (2026-11-08 audit: they were open). */
class ResourceServerConfigInternalCallTest {

  @Test
  void 맞는_토큰만_통과한다() {
    assertThat(ResourceServerConfig.isInternalCall("s3cret", "s3cret")).isTrue();
    assertThat(ResourceServerConfig.isInternalCall("s3cret", "wrong")).isFalse();
    assertThat(ResourceServerConfig.isInternalCall("s3cret", null)).isFalse();
  }

  @Test
  void 토큰을_설정하지_않으면_모두_막는다() {
    assertThat(ResourceServerConfig.isInternalCall("", "")).isFalse();
    assertThat(ResourceServerConfig.isInternalCall(null, null)).isFalse();
  }
}
