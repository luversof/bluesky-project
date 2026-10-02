package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.poe.dto.PoeGroups;
import net.luversof.web.gate.poe2.dto.Poe2;

/** 아이템 분류 영어 이름(10-02 AAA) — 표에서 찾기, 없거나 비면 id 그대로, 이름 없는 항목은 표에 안 넣기. */
class PoeClassNamesTest {

  @Test
  void picksGameNameAndFallsBackToId() {
    Map<String, String> t = Map.of("HybridFlask", "Hybrid Flasks", "Blank", " ");
    assertThat(PoeClassNames.pick(t, "HybridFlask")).isEqualTo("Hybrid Flasks");
    assertThat(PoeClassNames.pick(t, "Claw")).isEqualTo("Claw");
    assertThat(PoeClassNames.pick(t, "Blank")).isEqualTo("Blank");
    assertThat(PoeClassNames.pick(null, "Claw")).isEqualTo("Claw");
  }

  @Test
  void poe1TableSkipsEntriesWithoutEnglishName() {
    List<PoeGroups.ClassGroup> groups =
        List.of(
            new PoeGroups.ClassGroup(
                "flask",
                List.of(
                    new PoeGroups.Entry("HybridFlask", "하이브리드 플라스크", "flask", "Hybrid Flasks"),
                    new PoeGroups.Entry("Old", "옛", "old", null))));
    Map<String, String> t = PoeClassNames.fromGroups(groups);
    assertThat(t).containsEntry("HybridFlask", "Hybrid Flasks").doesNotContainKey("Old");
    assertThat(PoeClassNames.fromGroups(null)).isEmpty();
  }

  @Test
  void poe2TableKeepsFirstName() {
    Map<String, String> t =
        PoeClassNames.fromClasses(
            List.of(
                new Poe2.ItemClass("TrapTool", "덫", "weapon", "Traps"),
                new Poe2.ItemClass("TrapTool", "덫", "weapon", "Other"),
                new Poe2.ItemClass("Focus", "집중구", "offhand", null)));
    assertThat(t).containsEntry("TrapTool", "Traps").doesNotContainKey("Focus");
  }
}
