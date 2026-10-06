package net.luversof.web.gate.poe.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 감시자의 눈 공통 줄 최대롤(10-05 C156) — 계산(합성 감시자의 눈 · 최대롤)과 같게 범위의 큰 값, 범위 없는 줄 · null 은 그대로. */
class PoeHtmxControllerMaxRollTest {

  @Test
  void rangeBecomesUpperBound() {
    assertThat(PoeHtmxController.maxRoll("(4-6)% increased maximum Life"))
        .isEqualTo("6% increased maximum Life");
    assertThat(PoeHtmxController.maxRoll("최대 생명력 (4-6)% 증가")).isEqualTo("최대 생명력 6% 증가");
    assertThat(PoeHtmxController.maxRoll("Adds (1.5-2.5) to (3-4) Fire"))
        .isEqualTo("Adds 2.5 to 4 Fire");
  }

  @Test
  void otherLinesUnchanged() {
    assertThat(PoeHtmxController.maxRoll("+16 to all Attributes"))
        .isEqualTo("+16 to all Attributes");
    assertThat(PoeHtmxController.maxRoll(null)).isNull();
  }
}
