package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 툴팁 속성 칸 로컬 옵션 반영(10-08 C182) — PoB Item.lua 와 같은 식. */
class PoeLocalPropsTest {

  private static PoeLocalProps.Base armour(double a, double e, double s, double w, double block) {
    return new PoeLocalProps.Base(a, a, e, e, s, s, w, w, block, 0, 0, 0, 0);
  }

  @Test
  void increasedArmourAndBlockOnShield() {
    // 해질녘(PoE2, 품질 0): 방어도 223 × (1 + 350~450%) = 1004~1227 · 막기 26 × (1 + 10~20%) 내림 = 28~31
    var r =
        PoeLocalProps.apply(
            armour(223, 0, 0, 0, 26),
            List.of(
                "(350-450)% increased Armour",
                "(10-20)% increased Block chance", "+150 Strength Requirement"),
            true,
            false,
            0);
    assertThat(r.armour().text()).isEqualTo("1004-1227");
    assertThat(r.armour().changed()).isTrue();
    assertThat(r.block().text()).isEqualTo("28-31");
    assertThat(r.evasion().present()).isFalse();
  }

  @Test
  void qualityMultipliesSeparately() {
    // 메드베드의 도전(PoE1, 품질 20): 수호 102 × (1 + 30~50%) × 1.2 = 159~184
    var r =
        PoeLocalProps.apply(
            armour(0, 0, 0, 102, 0), List.of("(30-50)% increased Ward"), true, false, 20);
    assertThat(r.ward().text()).isEqualTo("159-184");
    // 옵션 없으면 품질만 — 바뀌지 않음 표시
    var plain =
        PoeLocalProps.apply(armour(100, 0, 0, 0, 0), List.of("+10 to Strength"), true, false, 20);
    assertThat(plain.armour().text()).isEqualTo("120");
    assertThat(plain.armour().changed()).isFalse();
  }

  @Test
  void flatThenIncreasedAndHybridLines() {
    // (100 + 20) × (1 + 50% + 30%) = 216 / 회피 (0 + 20) × 1.3 = 26 / 에너지 보호막은 방어 전체만 → 50 × 1.3 = 65
    var r =
        PoeLocalProps.apply(
            armour(100, 0, 50, 0, 0),
            List.of(
                "+20 to Armour and Evasion Rating",
                "50% increased Armour",
                "30% increased Armour, Evasion and Energy Shield"),
            true,
            false,
            0);
    assertThat(r.armour().text()).isEqualTo("216");
    assertThat(r.evasion().text()).isEqualTo("26");
    assertThat(r.energyShield().text()).isEqualTo("65");
  }

  @Test
  void weaponPhysicalSpeedCrit() {
    // 물리 (10 + 5~8)–(20 + 12~15) × (1 + 100%) · 공격 속도 1.2 × 1.1 = 1.32 · 치명 5 + 1.5 = 6.5
    var base = new PoeLocalProps.Base(0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 20, 1.2, 5);
    var r =
        PoeLocalProps.apply(
            base,
            List.of(
                "Adds (5-8) to (12-15) Physical Damage",
                "100% increased Physical Damage",
                "10% increased Attack Speed",
                "+1.5% to Critical Hit Chance"),
            false,
            true,
            0);
    assertThat(r.physMin().damageText()).isEqualTo("(30-36)");
    assertThat(r.physMax().damageText()).isEqualTo("(64-70)");
    assertThat(r.attacksPerSecond().text2()).isEqualTo("1.32");
    assertThat(r.critChance().text2()).isEqualTo("6.50");
  }

  @Test
  void weaponAddedElementalAndChaos() {
    // C183: 무기 로컬 "Adds X to Y Fire Damage" → 원소 피해 칸(속성별), 카오스는 따로. 주문 · 공격 전역 줄은 제외
    var w = new PoeLocalProps.Base(0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 20, 1.0, 5);
    var r =
        PoeLocalProps.apply(
            w,
            List.of(
                "Adds (10-12) to (20-24) Fire Damage",
                "Adds 1 to 50 Lightning Damage",
                "Adds 5 to 9 Chaos Damage",
                "Adds 3 to 6 Cold Damage to Spells"),
            false,
            true,
            0);
    assertThat(r.fire().text()).isEqualTo("(10-12)-(20-24)");
    assertThat(r.lightning().text()).isEqualTo("1-50");
    assertThat(r.cold().present()).isFalse();
    assertThat(r.chaos().text()).isEqualTo("5-9");
    assertThat(r.hasElemental()).isTrue();
  }

  @Test
  void sceptreSpirit() {
    // C188: 셉터 정신력 (100 + 20) × (1 + 30~40%) = 156~168, 옵션 없으면 100 그대로
    var r =
        PoeLocalProps.spirit(
            100, List.of("+20 to Spirit", "(30-40)% increased Spirit", "+10 to Strength"));
    assertThat(r.text()).isEqualTo("156-168");
    assertThat(r.changed()).isTrue();
    assertThat(PoeLocalProps.spirit(100, List.of()).text()).isEqualTo("100");
  }

  @Test
  void crossbowReload() {
    // C190: 재장전 0.8초 ÷ (1 + 15~25% + 10%(공격 속도)) = 0.8/1.35 ~ 0.8/1.25 = 0.59~0.64 · 30% 감소 =
    // 0.8/0.7 = 1.14
    var r =
        PoeLocalProps.reload(
            0.8, List.of("(15-25)% increased Reload Speed", "10% increased Attack Speed"));
    assertThat(r.text2()).isEqualTo("0.59-0.64");
    assertThat(PoeLocalProps.reload(0.8, List.of("30% reduced Reload Speed")).text2())
        .isEqualTo("1.14");
  }

  @Test
  void nonLocalContextsIgnored() {
    // 장신구(방어구 아님)의 방어 옵션 · 무기 아닌 아이템의 공격 옵션 · 주문 전용 피해는 반영하지 않는다
    var r =
        PoeLocalProps.apply(
            armour(0, 0, 0, 0, 0),
            List.of("50% increased Armour", "10% increased Attack Speed"),
            false,
            false,
            0);
    assertThat(r.armour().present()).isFalse();
    var w = new PoeLocalProps.Base(0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 20, 1.0, 5);
    var spells =
        PoeLocalProps.apply(w, List.of("Adds 10 to 20 Physical Damage to Spells"), false, true, 0);
    assertThat(spells.physMin().damageText()).isEqualTo("10");
    assertThat(spells.physMin().changed()).isFalse();
    // 감소는 음수 — 50% reduced Armour: 200 × 0.5 = 100
    assertThat(
            PoeLocalProps.apply(
                    armour(200, 0, 0, 0, 0), List.of("50% reduced Armour"), true, false, 0)
                .armour()
                .text())
        .isEqualTo("100");
  }
}
