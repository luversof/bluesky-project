package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 옵션 표 범위 표기(10-23 Q85) — '감소' 옵션은 작은 값부터, 0 을 가로지르는 범위 · 제작 굴림용 range 는 그대로. */
class PoeModRangeTest {

  @Test
  void 감소_옵션은_작은_값부터() {
    assertThat(
            PoeModRange.rangeAscending(
                "20% reduced Flask Charges used", "10% reduced Flask Charges used"))
        .isEqualTo("(10–20)% reduced Flask Charges used");
  }

  @Test
  void 보통_범위와_0을_가로지르는_범위는_그대로() {
    assertThat(PoeModRange.rangeAscending("+121 to Armour", "+150 to Armour"))
        .isEqualTo("+(121–150) to Armour");
    assertThat(PoeModRange.rangeAscending("-35% increased X", "35% increased X"))
        .isEqualTo("(-35–35)% increased X");
  }

  @Test
  void 제작_굴림용_range_는_순서를_바꾸지_않는다() {
    assertThat(
            PoeModRange.range("20% reduced Flask Charges used", "10% reduced Flask Charges used"))
        .isEqualTo("(20–10)% reduced Flask Charges used");
  }
}
