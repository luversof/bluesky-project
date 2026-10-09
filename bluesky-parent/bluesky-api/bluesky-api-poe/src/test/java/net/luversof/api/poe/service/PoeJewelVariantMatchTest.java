package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 빌드 임포트의 고유 주얼 변형 맞추기(10-03 C77) — 트리 주소 j= 의 "slug:v번호". */
class PoeJewelVariantMatchTest {

  private static PoeUniqueVariant v(int index, String... explicits) {
    return new PoeUniqueVariant(
        index, "v" + index, null, List.of(), List.of(), List.of(explicits), List.of());
  }

  private static final List<PoeUniqueVariant> THREAD_OF_HOPE =
      List.of(
          v(
              1,
              "Only affects Passives in Small Ring",
              "-(20-10)% to all Elemental Resistances",
              "Passage"),
          v(
              2,
              "-(20-10)% to all Elemental Resistances",
              "Passage",
              "Only affects Passives in Medium Ring"),
          v(
              5,
              "-(20-10)% to all Elemental Resistances",
              "Passage",
              "Only affects Passives in Massive Ring"));

  @Test
  void gameCopiedTextPicksVariantByLines() {
    // poe.ninja 실빌드 원문(Selected Variant 없음, 굴린 숫자)
    String text =
        String.join(
            "\n",
            "Rarity: UNIQUE",
            "Thread of Hope",
            "Crimson Jewel",
            "Radius: Variable",
            "Implicits: 0",
            "Only affects Passives in Medium Ring",
            "Passive Skills in Radius can be Allocated without being connected to your tree",
            "-11% to all Elemental Resistances",
            "Passage");
    assertThat(PoePobImportService.matchVariant(THREAD_OF_HOPE, text)).isEqualTo(2);
  }

  @Test
  void selectedVariantLineWins() {
    String text =
        "Rarity: UNIQUE\nThread of Hope\nCrimson Jewel\nSelected Variant: 5\nOnly affects Passives in Small Ring";
    assertThat(PoePobImportService.matchVariant(THREAD_OF_HOPE, text)).isEqualTo(5);
  }

  @Test
  void ambiguousOrSingleVariantGivesNull() {
    // 고리 줄이 없으면 세 변형이 같은 수로 맞는다 → 가릴 수 없다
    assertThat(
            PoePobImportService.matchVariant(
                THREAD_OF_HOPE, "-15% to all Elemental Resistances\nPassage"))
        .isNull();
    assertThat(PoePobImportService.matchVariant(List.of(v(1, "a")), "a")).isNull();
    assertThat(PoePobImportService.matchVariant(null, "a")).isNull();
  }

  @Test
  void impossibleEscapeKeystoneLine() {
    List<PoeUniqueVariant> ie =
        List.of(
            v(
                1,
                "Passive Skills in radius of Acrobatics can be allocated without being connected to your tree",
                "Corrupted"),
            v(
                2,
                "Passive Skills in radius of Ancestral Bond can be allocated without being connected to your tree",
                "Corrupted"));
    String text =
        "Rarity: UNIQUE\nImpossible Escape\nViridian Jewel\nPassive Skills in radius of Ancestral Bond can be allocated without being connected to your tree\nCorrupted";
    assertThat(PoePobImportService.matchVariant(ie, text)).isEqualTo(2);
  }
}
