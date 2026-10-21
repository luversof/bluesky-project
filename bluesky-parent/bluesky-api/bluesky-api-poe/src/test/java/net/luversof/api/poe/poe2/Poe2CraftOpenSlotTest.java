package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** PoE2 빈 자리 조건(10-20 C521, PoE1 C519 짝) — 쪽마다 (한도 − 옵션 수) ≥ 요구 수, 옵션 목표와 함께. 주얼은 쪽당 2. */
class Poe2CraftOpenSlotTest {

  private static final Poe2CraftCalcService.Target OPEN_P =
      new Poe2CraftCalcService.Target(List.of(Poe2CraftCalcService.OPEN_PREFIX));

  private static final Poe2CraftCalcService.Target OPEN_S =
      new Poe2CraftCalcService.Target(List.of(Poe2CraftCalcService.OPEN_SUFFIX));

  private static final Poe2CraftCalcService.Target A =
      new Poe2CraftCalcService.Target(List.of("A"));

  private static Poe2CraftEngine.Affix affix(String id, String gen) {
    return new Poe2CraftEngine.Affix(id, gen, 1, List.of(), false);
  }

  @Test
  void 레어는_쪽당_셋_주얼은_둘() {
    Poe2CraftEngine.Item rare =
        new Poe2CraftEngine.Item(
            "Gloves|Armour",
            80,
            Poe2CraftEngine.Rarity.RARE,
            List.of(affix("A", "prefix"), affix("B", "prefix"), affix("C", "suffix")),
            false);
    assertThat(Poe2CraftCalcService.meets(rare, List.of(A, OPEN_P))).isTrue();
    assertThat(Poe2CraftCalcService.meets(rare, List.of(A, OPEN_P, OPEN_P))).isFalse();
    assertThat(Poe2CraftCalcService.meets(rare, List.of(A, OPEN_S, OPEN_S))).isTrue();
    Poe2CraftEngine.Item jewel =
        new Poe2CraftEngine.Item(
            "Jewel|Ruby",
            80,
            Poe2CraftEngine.Rarity.RARE,
            List.of(affix("A", "prefix"), affix("C", "suffix")),
            false);
    assertThat(Poe2CraftCalcService.meets(jewel, List.of(A, OPEN_S))).isTrue();
    assertThat(Poe2CraftCalcService.meets(jewel, List.of(A, OPEN_S, OPEN_S))).isFalse();
    assertThat(Poe2CraftCalcService.real(List.of(A, OPEN_P, OPEN_S))).containsExactly(A);
  }
}
