package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 툴팁 요구 능력치 반영 규칙(10-08 C177) — PoB Item.lua 와 같은 (베이스 + 고정) × (1 + 증가%), 내림. */
class PoeRequirementsTest {

  @Test
  void flatAddsToBase() {
    // 해질녘(PoE2): Glacial Fortress 힘 99 + 150 = 249
    var r =
        PoeRequirements.apply(
            99, 0, 0, List.of("+150 Strength Requirement", "(10-20)% increased Block chance"));
    assertThat(r.str().text()).isEqualTo("249");
    assertThat(r.dex().present()).isFalse();
  }

  @Test
  void rangeShowsMinMax() {
    // 블런더보어: 베이스 힘 180 + (500-700)
    var r = PoeRequirements.apply(180, 0, 0, List.of("+(500-700) Strength Requirement"));
    assertThat(r.str().text()).isEqualTo("(680-880)");
  }

  @Test
  void increasedAllAttributesMultipliesAndFloors() {
    // 메드베드의 도전: 38 × (1 + 800%) = 342
    var r = PoeRequirements.apply(38, 38, 38, List.of("800% increased Attribute Requirements"));
    assertThat(r.str().text()).isEqualTo("342");
    assertThat(r.dex().text()).isEqualTo("342");
    assertThat(r.intel().text()).isEqualTo("342");
    // 탐욕의 포옹: 152 × 0.7 = 106.4 → 106
    assertThat(
            PoeRequirements.apply(152, 0, 0, List.of("30% reduced Strength Requirement"))
                .str()
                .text())
        .isEqualTo("106");
  }

  @Test
  void flatThenIncreasedAndCombinedAttributes() {
    // 순서: 고정 먼저, 그다음 증가 — (100 + 50) × 1.5 = 225
    var r =
        PoeRequirements.apply(
            100,
            0,
            100,
            List.of(
                "+50 Strength and Intelligence Requirement",
                "50% increased Attribute Requirements"));
    assertThat(r.str().text()).isEqualTo("225");
    assertThat(r.intel().text()).isEqualTo("225");
    assertThat(r.dex().present()).isFalse();
  }

  @Test
  void negativeFlatAndNoRequirements() {
    // 샤이바: 민첩 +50 · 힘 -15
    var r =
        PoeRequirements.apply(
            40, 40, 0, List.of("+50 Dexterity Requirement", "-15 Strength Requirement"));
    assertThat(r.str().text()).isEqualTo("25");
    assertThat(r.dex().text()).isEqualTo("90");
    // 디알라의 악의: 요구 없음
    var none = PoeRequirements.apply(0, 0, 104, List.of("Has no Attribute Requirements"));
    assertThat(none.intel().present()).isFalse();
  }

  @Test
  void globalModifiersDoNotChangeOwnRequirement() {
    // 지르피의 심장 · 아트지리의 실수: 다른 아이템 · 젬에 거는 전역 옵션 — 자기 요구치는 그대로
    var r =
        PoeRequirements.apply(
            50,
            0,
            0,
            List.of(
                "Items and Gems have 50% increased Attribute Requirements",
                "Equipment and Skill Gems have 25% increased Attribute Requirements",
                "Skill Gems have no Attribute Requirements"));
    assertThat(r.str().text()).isEqualTo("50");
    assertThat(PoeRequirements.apply(50, 0, 0, null).str().text()).isEqualTo("50");
  }
}
