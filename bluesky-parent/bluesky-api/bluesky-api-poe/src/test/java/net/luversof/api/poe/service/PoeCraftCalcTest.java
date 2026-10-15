package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.service.PoeCraftCalcService.Target;
import net.luversof.api.poe.service.PoeCraftEngine.Affix;
import net.luversof.api.poe.service.PoeCraftEngine.Item;
import net.luversof.api.poe.service.PoeCraftEngine.Rarity;

/** 계산기 목표 판정(10-15 C336) — 목표마다 서로 다른 옵션으로 채워야 한다(한 옵션이 두 목표를 채우지 않는다). */
class PoeCraftCalcTest {

  private static Item item(String... ids) {
    return new Item(
        "Gloves",
        "str_armour",
        "",
        84,
        Rarity.RARE,
        java.util.Arrays.stream(ids)
            .map(id -> new Affix(id, "prefix", List.of(), false, false))
            .toList(),
        false);
  }

  @Test
  void 목표는_서로_다른_옵션으로() {
    Target lifeHigh = new Target(List.of("Life1", "Life2"));
    Target anyLife = new Target(List.of("Life1", "Life2", "Life3"));
    assertThat(PoeCraftCalcService.meets(item("Life1"), List.of(lifeHigh))).isTrue();
    // 생명력 옵션 하나로 두 목표를 채울 수 없다
    assertThat(PoeCraftCalcService.meets(item("Life1"), List.of(lifeHigh, anyLife))).isFalse();
    // 배정 순서가 바뀌어도 찾는다(Life3 → anyLife, Life1 → lifeHigh)
    assertThat(PoeCraftCalcService.meets(item("Life3", "Life1"), List.of(anyLife, lifeHigh)))
        .isTrue();
    assertThat(PoeCraftCalcService.meets(item("Res"), List.of(lifeHigh))).isFalse();
    assertThat(PoeCraftCalcService.meets(item(), List.of())).isTrue();
  }

  @Test
  void 첫_수치() {
    assertThat(PoeCraftCalcService.firstNumber(List.of("+90 to maximum Life"))).isEqualTo(90.0);
    assertThat(
            PoeCraftCalcService.firstNumber(
                List.of("0.2% of Physical Attack Damage Leeched as Life")))
        .isEqualTo(0.2);
    assertThat(PoeCraftCalcService.firstNumber(List.of("Cannot be Frozen"))).isNull();
    assertThat(PoeCraftCalcService.firstNumber(null)).isNull();
  }
}
