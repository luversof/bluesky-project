package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * PoE2 시뮬 닌자 공통 특성 후보(10-14 사용자 요청 "특정 1명이 아니라 그 직업/스킬의 특성을 참고") — 실빌드 절반 이상이 쓰는 고유 · 보조 젬만, 이미 끼고
 * 있는 것은 빼고, 조건부 보조젬은 자동 적용에서 뺀다(가이드 후보와 같은 규칙).
 */
class Poe2ConsensusCandidatesTest {

  private final Poe2RefineService refine =
      new Poe2RefineService(null, null, null, null, "unused") {
        @Override
        String uniqueRaw(String title) {
          return "Rarity: UNIQUE\n" + title + "\nBase";
        }

        @Override
        String uniqueKo(String name) {
          return name + "(ko)";
        }

        @Override
        String gemKo(String name) {
          return name + "(ko)";
        }
      };

  private static final String XML =
      "<Items><Item id=\"1\">\nRarity: UNIQUE\nOlroth's Resolve\nUltimate Life Flask\n</Item></Items>"
          + "<Skill><Gem nameSpec=\"Spark\"/><Gem nameSpec=\"Execute III\"/>"
          + "<Gem nameSpec=\"Rakiata&apos;s Flow\"/></Skill>";

  private static Poe2.BuildGuide guide() {
    return new Poe2.BuildGuide(
        true,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Weak Support",
        "약한 보조",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static Poe2NinjaService.Consensus consensus(
      int n, Map<String, Integer> supports, Map<String, Map<String, Integer>> uniques) {
    return new Poe2NinjaService.Consensus(
        n, "Witch", "Stormweaver", List.of(), Map.of(), Map.of(), 0.9, supports, uniques, Map.of());
  }

  @Test
  void 절반_이상만_후보() {
    List<Poe2RefineService.Candidate> out =
        refine.consensusCandidates(
            XML,
            guide(),
            consensus(
                8,
                Map.of("Controlled Destruction", 4, "Rare Support", 3),
                Map.of("Amulet", Map.of("Astramentis", 5), "Belt", Map.of("Headhunter", 3))));
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .containsExactlyInAnyOrder(
            "닌자 공통 고유(5/8명): Amulet ← Astramentis(ko)",
            "닌자 공통 보조젬(4/8명): 약한 보조 → Controlled Destruction(ko)");
    assertThat(out).allMatch(c -> c.dpsPct() == 0 && c.ehpPct() == 0);
  }

  @Test
  void 이미_쓰는_고유_보조와_조건부_보조는_뺀다() {
    List<Poe2RefineService.Candidate> out =
        refine.consensusCandidates(
            XML,
            guide(),
            consensus(
                4,
                Map.of("Execute III", 4, "Inexorable Critical", 4, "Rakiata's Flow", 4),
                Map.of("Flask 1", Map.of("Olroth's Resolve", 4))));
    assertThat(out).isEmpty();
  }

  @Test
  void 실빌드_한_명이면_공통이_아니다() {
    assertThat(
            refine.consensusCandidates(
                XML, guide(), consensus(1, Map.of("Controlled Destruction", 1), Map.of())))
        .isEmpty();
    assertThat(refine.consensusCandidates(XML, guide(), null)).isEmpty();
  }

  @Test
  void 고유_착용_판정은_이름_줄만() {
    assertThat(Poe2RefineService.equipsUnique(XML, "Olroth's Resolve")).isTrue();
    assertThat(Poe2RefineService.equipsUnique(XML, "Ultimate Life Flask")).isFalse();
    assertThat(Poe2RefineService.equipsUnique(XML, "Olroth")).isFalse();
  }
}
