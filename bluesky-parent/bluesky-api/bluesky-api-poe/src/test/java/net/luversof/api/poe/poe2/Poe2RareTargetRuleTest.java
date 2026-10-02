package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 가이드 레어 목표 규칙(10-02) — 보일 만한 레어만 남기기(PoE1 isUpgrade 와 같은 기준: 한 축 +1% 이상, 어느 축도 -5% 넘게 안 깎임)와 PoB 에
 * 붙여 넣을 아이템 텍스트(엔진이 잰 그 모양).
 */
class Poe2RareTargetRuleTest {

  private static Poe2.GuideRare rare(Double dps, Double ehp) {
    return new Poe2.GuideRare(dps, ehp, List.of("x"), List.of("x"), "t");
  }

  @Test
  void keepsOnlyRaresWorthSwapping() {
    assertThat(Poe2BuildService.upgradeOrNull(rare(1.0, 0.0))).as("경계: +1% 정확히").isNotNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(0.99, 0.99))).as("두 축 다 +1% 미만").isNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(13.7, -5.0))).as("경계: -5% 정확히는 허용").isNotNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(13.7, -5.01))).as("다른 축 -5% 초과").isNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(-9.6, 13.7)))
        .as("장갑 사례: 생존용인데 DPS -9.6%")
        .isNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(null, 7.6))).as("측정 없는 축은 0").isNotNull();
    assertThat(Poe2BuildService.upgradeOrNull(rare(null, null))).isNull();
    assertThat(Poe2BuildService.upgradeOrNull(null)).isNull();
  }

  @Test
  void itemTextMatchesWhatTheEngineMeasured() {
    var spec =
        new Poe2BuildService.RareSpec(
            "Runeforged Secured Wraps", "룬 장갑", List.of("+10 to Armour"), List.of());
    String text =
        Poe2BuildService.rareItemText(
            spec, List.of("12% increased Attack Speed", "Adds 22 to 35 Fire damage to Attacks"));
    assertThat(text)
        .isEqualTo(
            "Rarity: RARE\nGuide Rare\nRuneforged Secured Wraps\nItem Level: 82\nImplicits: 1\n"
                + "+10 to Armour\n12% increased Attack Speed\nAdds 22 to 35 Fire damage to Attacks");
    var bare = new Poe2BuildService.RareSpec("Amulet", null, null, List.of());
    assertThat(Poe2BuildService.rareItemText(bare, List.of())).endsWith("Implicits: 0");
  }
}
