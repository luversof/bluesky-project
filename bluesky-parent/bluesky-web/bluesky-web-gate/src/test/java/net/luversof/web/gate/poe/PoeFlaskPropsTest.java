package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 플라스크 속성 로컬 옵션 반영(10-08 C186) — PoB Item.lua 식. */
class PoeFlaskPropsTest {

  @Test
  void lifeFlaskRecoveryQualityAndRate() {
    // 생명력 1000 · 지속 4초, 품질 20: 회복량 1000 × 1.2 × (1 + 20~30%) = 1440~1560, 지속 4 ÷ 1.5(회복 속도 50%) =
    // 2.7
    var r =
        PoeFlaskProps.apply(
            1000,
            0,
            4.0,
            60,
            15,
            false,
            List.of("(20-30)% increased Amount Recovered", "50% increased Recovery rate"),
            20);
    assertThat(r.life().text()).isEqualTo("1440-1560");
    assertThat(r.life().changed()).isTrue();
    assertThat(r.duration().seconds()).isEqualTo("2.70");
    assertThat(r.mana().text()).isEqualTo("0");
  }

  @Test
  void utilityDurationQualityAndLess() {
    // 유틸 4초, 품질 20: 4 × (1 + 30~40%) × 1.2 × 0.75(25% less) = 4.7~5.0
    var r =
        PoeFlaskProps.apply(
            0,
            0,
            4.0,
            60,
            30,
            true,
            List.of("(30-40)% increased Duration", "25% less Duration"),
            20);
    assertThat(r.duration().seconds()).isEqualTo("4.70-5.00");
  }

  @Test
  void chargesMaxAndUsedWithSignedRanges() {
    // 최대 60 + (-10~10) = 50~70 · 소모 30 × (1 + (-10~10)%) 내림 = 27~33, "(-10-10)% reduced" 는 부호 반전 →
    // 같은 범위
    var r =
        PoeFlaskProps.apply(
            0,
            0,
            5.0,
            60,
            30,
            true,
            List.of("+(-10-10) to Maximum Charges", "(-10-10)% reduced Charges per use"),
            0);
    assertThat(r.maxCharges().text()).isEqualTo("50-70");
    assertThat(r.chargesUsed().text()).isEqualTo("27-33");
    // C190: PoE2 "N% increased Charges" = 최대 충전 증가 — 40 × (1 + 20~30%) = 48~52
    assertThat(
            PoeFlaskProps.apply(0, 0, 4.0, 40, 30, true, List.of("(20-30)% increased Charges"), 0)
                .maxCharges()
                .text())
        .isEqualTo("48-52");
    // 옵션 없으면 그대로
    var plain = PoeFlaskProps.apply(0, 0, 5.0, 60, 30, true, List.of("Grants Onslaught"), 0);
    assertThat(plain.maxCharges().changed()).isFalse();
    assertThat(plain.duration().seconds()).isEqualTo("5.00");
  }
}
