package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** 업그레이드 가이드 고유 후보의 변형 고르기(10-04 C97) — 키워드에 가장 맞는 변형 사본, 엔진 원문 제목은 정식 이름. */
class PoeGuideBestVariantTest {

  private static PoeUniqueVariant v(int index, String name, String line) {
    return new PoeUniqueVariant(index, name, null, List.of(), List.of(), List.of(line), List.of());
  }

  private static PoeUniqueItem impresence() {
    List<PoeUniqueVariant> vs =
        List.of(
            v(1, "Physical", "Adds (10-14) to (17-20) Physical Damage"),
            v(2, "Fire", "Adds (20-24) to (33-36) Fire Damage"),
            v(5, "Chaos", "Adds (17-23) to (29-31) Chaos Damage"));
    return new PoeUniqueItem(
        "Impresence",
        "무존재",
        "impresence",
        "Onyx Amulet",
        null,
        "amulet",
        64,
        null,
        false,
        null,
        List.of(),
        List.of(),
        List.of("Adds (17-23) to (29-31) Chaos Damage"),
        List.of(),
        vs,
        5,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void picksVariantMatchingKeywords() {
    PoeUniqueItem best = PoeUpgradeGuideService.bestVariant(impresence(), List.of("fire"));
    assertThat(best.name()).isEqualTo("Impresence (Fire)");
    assertThat(best.defaultVariant()).isEqualTo(2);
    assertThat(best.explicits()).containsExactly("Adds (20-24) to (33-36) Fire Damage");
    // 엔진 원문 제목은 정식 이름(꼬리 없음)
    assertThat(PoeUpgradeGuideService.uniqueItemText(best, "Impresence"))
        .startsWith("Rarity: UNIQUE\nImpresence\n");
  }

  @Test
  void keepsDefaultWhenNoVariantScoresHigher() {
    PoeUniqueItem u = impresence();
    assertThat(PoeUpgradeGuideService.bestVariant(u, List.of("chaos"))).isSameAs(u);
    assertThat(PoeUpgradeGuideService.bestVariant(u, List.of("cold"))).isSameAs(u);
  }
}
