package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 성유 등급 순서(10-23 Q74) — 남색(mushrune6b)이 담청빛(mushrune6)과 같은 순위였다. 게임 tier 순: … 담청빛 6 · 남색 7 · 보라 8 …
 * 분광 최상.
 */
class PoeTreeGraphOilRankTest {

  @Test
  void 남색_성유는_담청빛과_보라_사이() {
    int azure = PoeTreeGraphService.oilRank("mushrune6");
    int indigo = PoeTreeGraphService.oilRank("mushrune6b");
    int violet = PoeTreeGraphService.oilRank("mushrune7");
    assertThat(azure).isLessThan(indigo);
    assertThat(indigo).isLessThan(violet);
  }

  @Test
  void 분광_성유가_가장_높고_황금이_그다음() {
    assertThat(PoeTreeGraphService.oilRank("mushruneuber"))
        .isGreaterThan(PoeTreeGraphService.oilRank("mushrune12"));
    assertThat(PoeTreeGraphService.oilRank("mushrune12"))
        .isGreaterThan(PoeTreeGraphService.oilRank("mushrune11"));
    assertThat(PoeTreeGraphService.oilRank(null)).isEqualTo(-1);
  }
}
