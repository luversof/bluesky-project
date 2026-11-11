package net.luversof.web.gate.poe2.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe2.dto.Poe2;

/**
 * PoE2 베이스 목록 방어구 속성 칩(2026-10-01, PoE1 베이스 목록과 같은 규칙) — 7 칩이 방어 조합을 겹침 없이 나누는지.
 *
 * <p>글자 대조로는 비교 뒤집기(ar && !ev → !ar && ev)가 살아남으니 판정 함수를 직접 부른다.
 */
class Poe2HtmxControllerAttrTest {

  private static Poe2.Armour armour(int ar, int ev, int es) {
    return new Poe2.Armour(ar, ev, es, null, null);
  }

  @Test
  void everyDefenceComboLandsInExactlyOneChip() {
    List<String> chips = List.of("str", "dex", "int", "strdex", "strint", "dexint", "strdexint");
    Map<String, Poe2.Armour> expected =
        Map.of(
            "str", armour(10, 0, 0),
            "dex", armour(0, 10, 0),
            "int", armour(0, 0, 10),
            "strdex", armour(10, 10, 0),
            "strint", armour(10, 0, 10),
            "dexint", armour(0, 10, 10),
            "strdexint", armour(10, 10, 10));
    expected.forEach(
        (want, a) -> {
          for (String chip : chips) {
            assertThat(Poe2HtmxController.matchesAttr(a, chip))
                .as(want + " in " + chip)
                .isEqualTo(chip.equals(want));
          }
        });
  }

  @Test
  void allPassesEverythingAndEmptyDefenceMatchesNoChip() {
    assertThat(Poe2HtmxController.matchesAttr(null, "all")).isTrue();
    assertThat(Poe2HtmxController.matchesAttr(armour(0, 0, 0), "all")).isTrue();
    assertThat(Poe2HtmxController.matchesAttr(null, "str")).isFalse();
    // 방어 값이 없는 베이스(PoE2 갑옷 33 개 등)는 어느 속성 칩에도 안 든다
    for (String chip : List.of("str", "dex", "int", "strdex", "strint", "dexint", "strdexint")) {
      assertThat(Poe2HtmxController.matchesAttr(armour(0, 0, 0), chip)).as(chip).isFalse();
    }
    // null 필드도 0 처럼
    assertThat(Poe2HtmxController.matchesAttr(new Poe2.Armour(null, 5, null, null, null), "dex"))
        .isTrue();
  }
}
