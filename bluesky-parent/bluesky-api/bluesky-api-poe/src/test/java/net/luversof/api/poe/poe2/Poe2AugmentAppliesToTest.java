package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 증강물 효과를 아이템 종류로 거르는 규칙(10-29 A1). */
class Poe2AugmentAppliesToTest {

  private static Poe2.AugmentEffect effect(String target, List<String> targetClasses) {
    return new Poe2.AugmentEffect(
        target, null, targetClasses, List.of("x"), List.of("x"), null, null, null);
  }

  @Test
  void 무도_또는_시전_무기는_무도_무기에도_들어간다() {
    // SoulCoreStatCategories 'Martial Or Caster Weapon' 의 대상 클래스엔 지팡이류만 적혀 있다 - 무도 무기는 Id 로만 뜻한다
    Poe2.AugmentEffect weapon =
        effect("Martial Or Caster Weapon", List.of("Wand", "Staff", "Sceptre"));
    assertThat(Poe2DataService.appliesTo(weapon, "One Hand Mace")).as("예전엔 빠졌다").isTrue();
    assertThat(Poe2DataService.appliesTo(weapon, "Bow")).isTrue();
    assertThat(Poe2DataService.appliesTo(weapon, "Wand")).isTrue();
    assertThat(Poe2DataService.appliesTo(weapon, "Helmet")).isFalse();
    assertThat(Poe2DataService.appliesTo(weapon, "Amulet")).isFalse();

    Poe2.AugmentEffect martialOrStaff =
        effect("Martial Weapon Wand or Staff", List.of("Wand", "Staff"));
    assertThat(Poe2DataService.appliesTo(martialOrStaff, "Spear")).isTrue();
    assertThat(Poe2DataService.appliesTo(martialOrStaff, "Sceptre")).as("셉터는 대상이 아니다").isFalse();
  }

  @Test
  void 대상_클래스가_적힌_분류는_그대로() {
    Poe2.AugmentEffect caster = effect("Caster Weapon", List.of("Wand", "Staff", "Sceptre"));
    assertThat(Poe2DataService.appliesTo(caster, "Wand")).isTrue();
    assertThat(Poe2DataService.appliesTo(caster, "Bow")).isFalse();
    assertThat(Poe2DataService.appliesTo(effect("Martial Weapon", List.of()), "Wand")).isFalse();
    assertThat(Poe2DataService.appliesTo(effect("Martial Weapon", List.of()), "Crossbow")).isTrue();
  }
}
