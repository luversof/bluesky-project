package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 파레토 개선(10-14 C328) — 한 축 목표에서 목표 축 손해 0 · 다른 축 +1% 이상. balanced 는 해당 없음. */
class Poe2ParetoTest {

  @Test
  void 생존_목표에서_DPS_만_오르는_교체() {
    assertThat(Poe2RefineService.pareto("ehp", 8.9, 0.0)).isTrue();
    assertThat(Poe2RefineService.pareto("ehp", 8.9, -0.1)).isFalse(); // 목표 축 손해
    assertThat(Poe2RefineService.pareto("ehp", 0.5, 0.0)).isFalse(); // 다른 축이 1% 미만
    assertThat(Poe2RefineService.acceptable("ehp", 8.9, 0.0)).isFalse(); // 목표 축 교체로는 안 받는다
  }

  @Test
  void 공격_목표에서_EHP_만_오르는_교체() {
    assertThat(Poe2RefineService.pareto("dps", 0.0, 3.0)).isTrue();
    assertThat(Poe2RefineService.pareto("dps", -1.0, 3.0)).isFalse();
  }

  @Test
  void 균형은_해당_없음() {
    assertThat(Poe2RefineService.pareto("balanced", 0.0, 3.0)).isFalse();
  }
}
