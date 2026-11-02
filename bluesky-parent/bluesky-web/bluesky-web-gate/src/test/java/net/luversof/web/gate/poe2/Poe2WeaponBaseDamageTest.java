package net.luversof.web.gate.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe.PoeLocalProps;
import net.luversof.web.gate.poe2.dto.Poe2;

/** PoE2 베이스 무기의 원소 · 카오스 피해(10-30 R14 — 달 단검 냉기 11-26 이 물리로 나왔다). */
class Poe2WeaponBaseDamageTest {

  private static final Poe2.Weapon MOON_DAGGER =
      new Poe2.Weapon(
          0,
          0,
          15.0,
          1.55,
          10,
          null,
          List.of(new Poe2.ElementalDamage("Cold", 11, 26)),
          null,
          null);

  private static PoeLocalProps.Result apply(List<String> lines) {
    return PoeLocalProps.apply(
        new PoeLocalProps.Base(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.55, 15),
        lines,
        false,
        true,
        0,
        MOON_DAGGER.baseAdded());
  }

  @Test
  void 물리가_0_이어도_피해가_있는_무기() {
    assertThat(MOON_DAGGER.hasDamage()).isTrue();
    assertThat(MOON_DAGGER.averageDamage()).isEqualTo(18.5);
    assertThat(new Poe2.Weapon(0, 0, 5.0, 1.2, 120, null).hasDamage()).isFalse();
  }

  @Test
  void 베이스_냉기는_바뀌지_않은_값_로컬_추가는_더해_바뀐_값() {
    PoeLocalProps.Result base = apply(List.of());
    assertThat(base.cold().text()).isEqualTo("11-26");
    assertThat(base.cold().max().changed()).isFalse();
    assertThat(base.physMax().present()).isFalse();
    PoeLocalProps.Result added = apply(List.of("Adds 2 to 4 Cold Damage"));
    assertThat(added.cold().text()).isEqualTo("13-30");
    assertThat(added.cold().max().changed()).isTrue();
  }
}
