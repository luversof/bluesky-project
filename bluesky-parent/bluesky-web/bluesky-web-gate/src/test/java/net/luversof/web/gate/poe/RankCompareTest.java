package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** 젬 DPS 랭킹 시즌 비교(2026-10-02) — 순위 변화 부호·새 젬·DPS % · 이전 시즌 고르기. */
class RankCompareTest {

  @Test
  void rankDeltaIsPositiveWhenRisingAndNewGemsAreMarked() {
    // 이전: A 1위 · B 2위 · C 3위 → 지금: C 1위(+2) · A 2위(-1) · D 3위(새로)
    Map<String, RankCompare.Change> c =
        RankCompare.compare(
            List.of("C", "A", "D"),
            List.of(300.0, 220.0, 100.0),
            List.of("A", "B", "C"),
            List.of(200.0, 150.0, 100.0));
    assertThat(c.get("C").rankDelta()).isEqualTo(2);
    assertThat(c.get("C").prevRank()).isEqualTo(3);
    assertThat(c.get("C").dpsPct()).isEqualTo(200.0);
    assertThat(c.get("A").rankDelta()).isEqualTo(-1);
    assertThat(c.get("A").dpsPct()).isEqualTo(10.0);
    assertThat(c.get("D").isNew()).isTrue();
    assertThat(c).doesNotContainKey("B"); // 지금 시즌에 없는 젬은 표에 없다
  }

  @Test
  void noPreviousSeasonMeansNoComparison() {
    assertThat(RankCompare.compare(List.of("A"), List.of(1.0), List.of(), List.of())).isEmpty();
    assertThat(RankCompare.compare(List.of("A"), List.of(1.0), null, null)).isEmpty();
  }

  @Test
  void sameRankIsZeroAndZeroDpsHasNoPercent() {
    Map<String, RankCompare.Change> c =
        RankCompare.compare(List.of("A"), List.of(5.0), List.of("A"), List.of(0.0));
    assertThat(c.get("A").rankDelta()).isZero();
    assertThat(c.get("A").dpsPct()).isNull();
  }

  @Test
  void previousSeasonIsTheNextOlderOne() {
    List<String> seasons = List.of("3.30", "3.29", "3.28");
    assertThat(RankCompare.previousSeason(seasons, "")).isEqualTo("3.29");
    assertThat(RankCompare.previousSeason(seasons, "3.29")).isEqualTo("3.28");
    assertThat(RankCompare.previousSeason(seasons, "3.28")).isNull();
    assertThat(RankCompare.previousSeason(List.of("3.29"), "")).isNull();
    assertThat(RankCompare.previousSeason(List.of(), "")).isNull();
  }
}
