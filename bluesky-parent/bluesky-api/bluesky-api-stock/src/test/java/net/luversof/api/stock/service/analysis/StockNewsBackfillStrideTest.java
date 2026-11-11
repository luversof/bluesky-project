package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class StockNewsBackfillStrideTest {

  @Test
  void 한_쪽이_짧은_시간을_덮으면_보폭을_늘리고_하루를_넘으면_한_쪽씩() {
    // 대형주: 한 쪽이 1 시간 - 하루 몇 쪽만 읽도록 두 배씩(상한 64).
    assertThat(StockNewsCollectService.nextStride(1, Duration.ofHours(1))).isEqualTo(2);
    assertThat(StockNewsCollectService.nextStride(64, Duration.ofHours(1))).isEqualTo(64);
    // 적당하면 그대로.
    assertThat(StockNewsCollectService.nextStride(4, Duration.ofHours(12))).isEqualTo(4);
    // 기사가 드문 구간(한 쪽이 며칠)에 들어서면 건너뛰지 않는다.
    assertThat(StockNewsCollectService.nextStride(16, Duration.ofDays(3))).isEqualTo(1);
  }
}
