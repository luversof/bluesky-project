package net.luversof.api.stock.service.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

/** 토큰 발급 시각은 응답의 남은 수명으로 적는다(2026-10-27 감사) - KIS 가 이미 발급한 토큰을 다시 주면 그 토큰은 우리 시계보다 일찍 만료된다. */
class KisTokenIssuedAtTest {

  private static final Instant NOW = Instant.parse("2026-10-27T05:00:00Z");

  @Test
  void 남은_수명이_하루면_지금이다() {
    assertThat(KisAuthService.tokenIssuedAt(NOW, 86400)).isEqualTo(NOW);
  }

  @Test
  void 남은_수명이_짧으면_그만큼_앞당겨_적는다() {
    // 다른 환경이 18 시간 전에 받은 토큰을 다시 받았다 - 남은 6 시간.
    Instant issuedAt = KisAuthService.tokenIssuedAt(NOW, "21600");
    assertThat(issuedAt).isEqualTo(NOW.minus(Duration.ofHours(18)));
    // 23 시간 판정은 실제 만료(지금 + 6 시간)의 1 시간 전에 걸린다.
    assertThat(issuedAt.plus(Duration.ofHours(23))).isEqualTo(NOW.plus(Duration.ofHours(5)));
  }

  @Test
  void 모르는_꼴이면_지금이다() {
    assertThat(KisAuthService.tokenIssuedAt(NOW, null)).isEqualTo(NOW);
    assertThat(KisAuthService.tokenIssuedAt(NOW, "abc")).isEqualTo(NOW);
    assertThat(KisAuthService.tokenIssuedAt(NOW, 0)).isEqualTo(NOW);
  }
}
