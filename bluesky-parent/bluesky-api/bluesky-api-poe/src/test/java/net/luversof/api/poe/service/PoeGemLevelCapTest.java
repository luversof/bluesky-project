package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * 보조젬 레벨 = 자연 최대 레벨(10-15 C334) — 최적화기가 보조젬을 모두 20 으로 넣어 각성 강화 20(인게임 최대 4) · 상급 주문 메아리 20(최대 3)이
 * DPS 를 수 배 부풀렸다. 업그레이드 가이드의 보조젬 교체도 원래 레벨 20 을 그대로 둬 같은 착시가 났다.
 */
class PoeGemLevelCapTest {

  private static PoeGem gem(String name, int tableLevels, Integer naturalMax) {
    List<PoeGemLevel> levels =
        IntStream.rangeClosed(1, tableLevels)
            .mapToObj(
                i ->
                    new PoeGemLevel(
                        i, 1, null, null, null, null, null, null, null, null, null, null, List.of(),
                        null, null))
            .toList();
    return new PoeGem(
        name,
        name,
        name,
        null,
        true,
        false,
        null,
        null,
        "blue",
        1,
        false,
        false,
        true,
        null,
        null,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        levels,
        null,
        null,
        null,
        null,
        null,
        naturalMax);
  }

  @Test
  void 자연_최대_레벨이_레벨_표보다_우선() {
    assertThat(PoeOptimizeService.defaultGemLevel(gem("Greater Spell Echo Support", 15, 3)))
        .isEqualTo(3);
    assertThat(PoeOptimizeService.defaultGemLevel(gem("Awakened Empower Support", 20, 4)))
        .isEqualTo(4);
    assertThat(PoeOptimizeService.defaultGemLevel(gem("Spell Echo Support", 40, 20))).isEqualTo(20);
    // PoB 에 없는 젬은 예전처럼 레벨 표 최대(20 상한)
    assertThat(PoeOptimizeService.defaultGemLevel(gem("Unknown", 15, null))).isEqualTo(15);
    assertThat(PoeOptimizeService.defaultGemLevel(gem("Unknown", 30, null))).isEqualTo(20);
  }

  @Test
  void 가이드_교체_젬_레벨을_자연_최대로_깎는다() {
    String tag =
        "<Gem enabled=\"true\" level=\"20\" nameSpec=\"Awakened Empower\" quality=\"20\"/>";
    assertThat(PoeUpgradeGuideService.capLevel(tag, gem("Awakened Empower Support", 20, 4)))
        .contains(" level=\"4\"")
        .doesNotContain(" level=\"20\"");
    // 최대가 20 인 젬은 부패 21 도 그대로
    String t21 = "<Gem level=\"21\" nameSpec=\"Controlled Destruction\"/>";
    assertThat(PoeUpgradeGuideService.capLevel(t21, gem("Controlled Destruction Support", 40, 20)))
        .isEqualTo(t21);
    // 이미 낮으면 그대로
    String t3 = "<Gem level=\"3\" nameSpec=\"Awakened Empower\"/>";
    assertThat(PoeUpgradeGuideService.capLevel(t3, gem("Awakened Empower Support", 20, 4)))
        .isEqualTo(t3);
    // level 속성이 없으면(PoB 기본 20 이 된다) 자연 최대를 넣는다
    assertThat(
            PoeUpgradeGuideService.capLevel(
                "<Gem nameSpec=\"Greater Spell Echo\"/>", gem("Greater Spell Echo Support", 15, 3)))
        .contains(" level=\"3\"");
  }
}
