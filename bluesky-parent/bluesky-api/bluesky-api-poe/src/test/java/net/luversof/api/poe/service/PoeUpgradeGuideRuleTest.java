package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.poe.service.PoeUpgradeGuideService.ItemPick;

/**
 * PoE1 업그레이드 가이드 판정(10-02 SSS) — 바꿀 만한가(한 축 +1% 이상 · 어느 축도 -5% 넘게 안 깎임), 축 이름, 칸별 추천 고르기. 경계는 양쪽을 다
 * 본다(비교 방향이 뒤집혀도 잡히게).
 */
class PoeUpgradeGuideRuleTest {

  private static ItemPick pick(String slug, double dps, double ehp, Double minion) {
    return new ItemPick(
        "UNIQUE", slug, slug, slug, "Base", "베이스", null, dps, ehp, 0, minion, 0, 0, null, null,
        null);
  }

  @Test
  void upgradeNeedsOneAxisGainAndNoBigLoss() {
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 1.0, 0, null))).as("+1% 정확히").isTrue();
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 0.99, 0.99, null)))
        .as("+1% 미만")
        .isFalse();
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 10, -5.0, null)))
        .as("-5% 정확히 허용")
        .isTrue();
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 10, -5.01, null))).as("-5% 초과").isFalse();
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 0, 0, 1.0))).as("소환수 축만 +1%").isTrue();
    assertThat(PoeUpgradeGuideService.isUpgrade(pick("a", 10, 0, -5.01)))
        .as("소환수 축 -5% 초과")
        .isFalse();
  }

  @Test
  void axisNameFollowsLargestGainOrLoss() {
    assertThat(PoeUpgradeGuideService.axisName(pick("a", 5, 2, null), true)).isEqualTo("딜");
    assertThat(PoeUpgradeGuideService.axisName(pick("a", 2, 5, null), true)).isEqualTo("생존");
    assertThat(PoeUpgradeGuideService.axisName(pick("a", 2, 5, 9.0), true)).isEqualTo("소환수 생존");
    assertThat(PoeUpgradeGuideService.axisName(pick("a", -4, -1, null), false)).isEqualTo("딜");
    assertThat(PoeUpgradeGuideService.axisName(pick("a", -1, -4, null), false)).isEqualTo("생존");
  }

  @Test
  void choosePicksTakesBestPerAxisWithoutTradeOffs() {
    ItemPick dpsBest = pick("dps-best", 12, -2, null);
    ItemPick ehpBest = pick("ehp-best", 1, 8, null);
    ItemPick tradeOff = pick("trade-off", 30, -6, null); // DPS 는 가장 크지만 EHP -6% 라 뺀다
    ItemPick weak = pick("weak", 0.5, 0.5, null);
    List<ItemPick> picks =
        PoeUpgradeGuideService.choosePicks(List.of(weak, tradeOff, ehpBest, dpsBest));
    assertThat(picks).containsExactly(dpsBest, ehpBest); // 합 큰 순: 10 > 9
    ItemPick both = pick("both", 9, 9, null);
    assertThat(PoeUpgradeGuideService.choosePicks(List.of(both)))
        .as("두 축 최고가 같은 아이템이면 하나")
        .containsExactly(both);
    assertThat(PoeUpgradeGuideService.gainOf(pick("m", 1, 2, 3.0))).isEqualTo(6.0);
  }
}
