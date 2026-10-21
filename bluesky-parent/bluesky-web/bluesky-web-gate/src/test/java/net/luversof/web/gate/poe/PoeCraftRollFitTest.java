package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe.dto.PoeCraft;

/** 가져온 아이템 수치 → 굴림 씨앗(10-20 C485) — 찾은 씨앗으로 다시 굴리면 같은 수치. */
class PoeCraftRollFitTest {

  private static PoeCraft.Line line(String id, List<String> en, List<String> enMin) {
    return new PoeCraft.Line(
        id, "prefix", id, id, en, List.of(), enMin, List.of(), 1, 1, false, false, false);
  }

  @Test
  void 한_줄_범위는_정확히_그_수치를_내는_씨앗() {
    PoeCraft.Line l =
        line("IncreasedLife8", List.of("+129 to maximum Life"), List.of("+115 to maximum Life"));
    Map<String, Long> seeds =
        PoeCraftRollFit.fit(List.of(l), List.of(), Map.of("IncreasedLife8", List.of(120d)));
    String range = PoeModRange.range("+115 to maximum Life", "+129 to maximum Life");
    assertThat(PoeModRoll.roll(range, seeds.get("IncreasedLife8")))
        .isEqualTo("+120 to maximum Life");
  }

  @Test
  void 두_범위_더하기_피해도_둘_다_맞춘다() {
    PoeCraft.Line l =
        line(
            "AddedFireDamage6",
            List.of("Adds 15 to 27 Fire Damage to Attacks"),
            List.of("Adds 11 to 23 Fire Damage to Attacks"));
    Map<String, Long> seeds =
        PoeCraftRollFit.fit(List.of(l), List.of(), Map.of("AddedFireDamage6", List.of(13d, 25d)));
    String range =
        PoeModRange.range(
            "Adds 11 to 23 Fire Damage to Attacks", "Adds 15 to 27 Fire Damage to Attacks");
    assertThat(PoeModRoll.roll(range, seeds.get("AddedFireDamage6")))
        .isEqualTo("Adds 13 to 25 Fire Damage to Attacks");
  }

  @Test
  void 수치_개수가_다르거나_범위가_없으면_건너뛴다() {
    PoeCraft.Line fixed =
        line(
            "Fixed",
            List.of("+1 to Level of Socketed Gems"),
            List.of("+1 to Level of Socketed Gems"));
    PoeCraft.Line life =
        line("IncreasedLife8", List.of("+129 to maximum Life"), List.of("+115 to maximum Life"));
    Map<String, Long> seeds =
        PoeCraftRollFit.fit(
            List.of(fixed, life),
            List.of(),
            Map.of("Fixed", List.of(1d), "IncreasedLife8", List.of(120d, 5d)));
    assertThat(seeds).isEmpty();
  }

  @Test
  void PoE2_범위_문장도() {
    Map<String, Long> seeds =
        PoeCraftRollFit.fitRanges(
            Map.of("IncreasedLife8", List.of("+(100-119) to maximum Life")),
            Map.of("IncreasedLife8", List.of(110d)));
    assertThat(PoeModRoll.roll("+(100-119) to maximum Life", seeds.get("IncreasedLife8")))
        .isEqualTo("+110 to maximum Life");
  }
}
