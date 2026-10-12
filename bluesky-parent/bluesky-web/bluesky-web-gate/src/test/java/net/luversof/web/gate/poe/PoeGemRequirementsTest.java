package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 젬 요구 능력치 — PoB getGemStatRequirement(10-09 C225). */
class PoeGemRequirementsTest {

  @Test
  void 회오리바람_70레벨은_68힘_98민첩() {
    assertThat(PoeGemRequirements.stat(70, false, 60)).isEqualTo(98);
    assertThat(PoeGemRequirements.stat(70, false, 40)).isEqualTo(68);
  }

  @Test
  void 낮은_레벨은_14미만이라_요구_없음() {
    assertThat(PoeGemRequirements.stat(1, false, 40)).isZero();
    assertThat(PoeGemRequirements.stat(70, false, 0)).isZero();
    assertThat(PoeGemRequirements.stat(70, false, null)).isZero();
  }

  @Test
  void PoE2_얼음_화살_90레벨은_157민첩() {
    assertThat(PoeGemRequirements.poe2Stat(90, false, 100)).isEqualTo(157);
    // 1레벨: round((5 - 3.4) × 1) + 4 = 6 → 8 미만이라 요구 없음
    assertThat(PoeGemRequirements.poe2Stat(1, false, 100)).isZero();
    assertThat(PoeGemRequirements.poe2Stat(90, true, 100)).isZero();
  }

  @Test
  void 보조_젬은_계수_0점5() {
    // (20 + 67 × 3) × 1^0.9 × 0.5 = 110.5 → 111(round)
    assertThat(PoeGemRequirements.stat(70, true, 100)).isEqualTo(111);
  }
}
