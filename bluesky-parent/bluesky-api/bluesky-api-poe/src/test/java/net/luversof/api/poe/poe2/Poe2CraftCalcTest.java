package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.poe2.Poe2CraftCalcService.Target;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Affix;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Item;
import net.luversof.api.poe.poe2.Poe2CraftEngine.Rarity;

/** PoE2 시뮬레이터 목표 판정(10-15 C352) — 목표마다 서로 다른 옵션으로. */
class Poe2CraftCalcTest {

  private static Item item(String... ids) {
    return new Item(
        "Gloves|Armour",
        82,
        Rarity.RARE,
        java.util.Arrays.stream(ids)
            .map(id -> new Affix(id, "prefix", 1, List.of(), false))
            .toList(),
        false);
  }

  @Test
  void 목표는_서로_다른_옵션으로() {
    Target high = new Target(List.of("L1", "L2"));
    Target any = new Target(List.of("L1", "L2", "L3"));
    assertThat(Poe2CraftCalcService.meets(item("L1"), List.of(high))).isTrue();
    assertThat(Poe2CraftCalcService.meets(item("L1"), List.of(high, any))).isFalse();
    assertThat(Poe2CraftCalcService.meets(item("L3", "L1"), List.of(any, high))).isTrue();
    assertThat(Poe2CraftCalcService.meets(item("X"), List.of(high))).isFalse();
  }

  @Test
  void 반쪽_재조합_비용은_두_반쪽_합을_성공_확률로_나눈다() {
    Poe2CraftCalcService.Method a =
        new Poe2CraftCalcService.Method(
            "CHAOS", "카오스", 100, 10, 0.1, java.util.Map.of("Chaos Orb", 10d), 1.0, null);
    Poe2CraftCalcService.Method b =
        new Poe2CraftCalcService.Method(
            "EXALT", "엑잘", 100, 25, 0.25, java.util.Map.of("Exalted Orb", 4d), 2.0, null);
    Poe2CraftCalcService.Method m =
        Poe2CraftCalcService.combineHalves(a, b, 0.5, false, 50, "접두 · 접미", 0);
    assertThat(m.successRate()).isEqualTo(0.5);
    assertThat(m.perSuccess()).containsEntry("Chaos Orb", 20d).containsEntry("Exalted Orb", 8d);
    assertThat(m.costPerSuccess()).isEqualTo(6.0);
    // 행운 = 1 - 0.5² = 0.75, 징조(0.3 div) 하나 / 시도
    Poe2CraftCalcService.Method l =
        Poe2CraftCalcService.combineHalves(a, b, 0.5, true, 50, "접두 · 접미", 0.3);
    assertThat(l.successRate()).isEqualTo(0.75);
    assertThat(l.costPerSuccess()).isCloseTo(3.0 / 0.75 + 0.3 / 0.75, within(1e-9));
    assertThat(l.perSuccess().get("Omen of Recombination")).isCloseTo(1 / 0.75, within(1e-9));
    // 한쪽이 한 번도 못 채우면 비용 없음
    Poe2CraftCalcService.Method z =
        new Poe2CraftCalcService.Method("ALCHEMY", "연금", 100, 0, 0, java.util.Map.of(), null, null);
    assertThat(Poe2CraftCalcService.combineHalves(a, z, 0.5, false, 50, "앞뒤 반", 0).costPerSuccess())
        .isNull();
    assertThat(Poe2CraftCalcService.combineHalves(a, z, 0.5, false, 50, "앞뒤 반", 0).successRate())
        .isZero();
  }
}
