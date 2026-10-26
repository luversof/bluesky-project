package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe.controller.PoeCraftController;
import net.luversof.web.gate.poe.dto.PoeBuild;
import net.luversof.web.gate.poe2.controller.Poe2CraftController;
import net.luversof.web.gate.poe2.dto.Poe2;

/** PoB 빌드 코드 → 빌드 아이템 → PoB 아이템 글(10-21 C525 · C526) — 붙여넣기 해석기(fromPob)가 읽는 모양. */
class PoeCraftBuildImportTextTest {

  @Test
  void 빌드_코드는_공백_없는_긴_base64() {
    assertThat(PoeCraftController.buildCode("eNr" + "A".repeat(300))).isTrue();
    assertThat(PoeCraftController.buildCode("Item Class: Gloves\nRarity: Rare")).isFalse();
    assertThat(PoeCraftController.buildCode("eNrShort")).isFalse();
  }

  @Test
  void PoE1_희귀_아이템_글() {
    PoeBuild.BuildItem bi =
        new PoeBuild.BuildItem(
            "Gloves",
            "장갑",
            "RARE",
            "Doom Grip",
            null,
            "Titan Gauntlets",
            null,
            null,
            null,
            List.of(
                "+120 to maximum Life", "+44% to Cold Resistance", "15% increased Attack Speed"),
            null,
            List.of("+12 to Strength"),
            null,
            true,
            20,
            null,
            null,
            null,
            null,
            null,
            0,
            List.of("shaper", "fractured"),
            List.of(""),
            List.of("", "crafted", "fractured"),
            null,
            null,
            null,
            null,
            null);
    String t = PoeCraftController.pobText(bi);
    assertThat(t)
        .startsWith(
            "Rarity: RARE\nDoom Grip\nTitan Gauntlets\nQuality: 20\nImplicits: 1\n+12 to Strength\n")
        .contains("{crafted}+44% to Cold Resistance\n")
        .contains("{fractured}15% increased Attack Speed\n")
        .contains("Corrupted\n")
        .endsWith("Shaper Item\n");
  }

  @Test
  void PoE2_앞_N줄은_고정속성() {
    Poe2.BuildItem bi =
        new Poe2.BuildItem(
            "Gloves",
            "MAGIC",
            "Stocky Mitts of the Ice",
            null,
            "Stocky Mitts",
            null,
            null,
            null,
            null,
            List.of("+15 to Armour", "+20% to Cold Resistance"),
            null,
            1,
            List.of(),
            null,
            null,
            null,
            0,
            null,
            null,
            null);
    assertThat(Poe2CraftController.pobText(bi))
        .isEqualTo(
            "Rarity: MAGIC\nStocky Mitts of the Ice\nImplicits: 1\n+15 to Armour\n+20% to Cold Resistance\n");
  }
}
