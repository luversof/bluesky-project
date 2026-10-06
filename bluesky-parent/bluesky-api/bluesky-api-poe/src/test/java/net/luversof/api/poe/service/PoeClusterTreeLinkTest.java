package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** PoE1 빌드 → 트리 보기 c= 항목(10-03 C41) — PoB XML 의 클러스터 주얼 원문에서 크기 · 노드 수 · 스킬 키 · 노터블 · 주얼 칸 수. */
class PoeClusterTreeLinkTest {

  private static final String NL = String.valueOf((char) 10);

  private static final Map<String, String> LARGE =
      Map.of(
          "affliction_fire_damage_large", "10% increased Fire Damage",
          "affliction_attack_damage_", "10% increased Attack Damage");

  @Test
  void 대형_클러스터() {
    String text =
        String.join(
            NL,
            "Rarity: RARE",
            "Glyph Glimmer",
            "Large Cluster Jewel",
            "Item Level: 84",
            "Implicits: 3",
            "{crafted}Adds 8 Passive Skills",
            "{crafted}2 Added Passive Skills are Jewel Sockets",
            "{crafted}Added Small Passive Skills grant: 12% increased Fire Damage",
            "1 Added Passive Skill is Prismatic Heart",
            "1 Added Passive Skill is Burning Bright");
    assertThat(PoePobImportService.clusterEntry("26725", text, size -> LARGE))
        .isEqualTo("26725:Large:8:affliction_fire_damage_large:Prismatic Heart|Burning Bright:2");
  }

  @Test
  void 주얼_칸_하나_표기와_스킬_못_찾으면_빈_키() {
    String text =
        String.join(
            NL,
            "Rarity: MAGIC",
            "Medium Cluster Jewel",
            "Adds 4 Passive Skills",
            "1 Added Passive Skill is a Jewel Socket",
            "Added Small Passive Skills grant: 5% increased Something Unknown");
    assertThat(PoePobImportService.clusterEntry("65600", text, size -> Map.of()))
        .isEqualTo("65600:Medium:4:::1");
  }

  @Test
  void 클러스터가_아니면_null() {
    assertThat(
            PoePobImportService.clusterEntry(
                "1",
                "Rarity: UNIQUE" + NL + "Thread of Hope" + NL + "Crimson Jewel",
                s -> Map.of()))
        .isNull();
  }

  @Test
  void 매직_클러스터_이름에_섞인_베이스와_PoB_스킬_줄() {
    // 실빌드(10-03 C42): 매직 대형 클러스터는 베이스가 이름에 섞여 있고, PoB 가 스킬 키 줄을 적어 둔다 — 놓치면 그 아래 중형 2개까지 9 노드가 빠졌다
    String text =
        String.join(
            NL,
            "Rarity: MAGIC",
            "Notable Large Cluster Jewel of Significance",
            "Cluster Jewel Skill: affliction_minion_damage",
            "Cluster Jewel Node Count: 8",
            "{crafted}Adds 8 Passive Skills",
            "{crafted}2 Added Passive Skills are Jewel Sockets",
            "{crafted}Added Small Passive Skills grant: Minions deal 10% increased Damage",
            "1 Added Passive Skill is Renewal");
    assertThat(PoePobImportService.clusterEntry("21984", text, size -> Map.of()))
        .isEqualTo("21984:Large:8:affliction_minion_damage:Renewal:2");
  }

  @Test
  void Voices_는_소켓_덮어쓰기와_빈_작은_패시브() {
    // 실빌드(10-03 C48): Voices 를 j= 로만 보내 그 안 중첩 소켓의 클러스터 노드가 걷혔다(93 / 123). PoB 규칙: 소켓 수 = "Adds N
    // Jewel
    // Socket Passive Skills", 노드 수 = 소켓 + "Adds N Small Passive Skills which grant nothing", 스킬 없음
    String text =
        String.join(
            NL,
            "Rarity: UNIQUE",
            "Voices",
            "Large Cluster Jewel",
            "Adds 3 Jewel Socket Passive Skills",
            "Adds 5 Small Passive Skills which grant nothing",
            "Corrupted");
    assertThat(PoePobImportService.clusterEntry("7960", text, size -> Map.of()))
        .isEqualTo("7960:Large:8:::3");
    assertThat(
            PoePobImportService.clusterEntry(
                "7960",
                text.replace(
                    "Adds 5 Small Passive Skills which grant nothing",
                    "Adds 1 Small Passive Skill which grants nothing"),
                size -> Map.of()))
        .isEqualTo("7960:Large:4:::3");
  }

  @Test
  void 연결_없이_찍기_주얼() {
    // 실빌드(10-03 C64): 링크에 이 규칙이 없어 첫 편집에서 그 노드들이 고아로 걷혔다
    String ie =
        String.join(
            NL,
            "Rarity: UNIQUE",
            "Impossible Escape",
            "Viridian Jewel",
            "Radius: Medium",
            "Passives in radius of Point Blank can be Allocated",
            "without being connected to your tree");
    assertThat(
            PoePobImportService.impossibleEscapeKey(
                ie, name -> "Point Blank".equals(name) ? 22088 : null))
        .isEqualTo("22088:1440");
    assertThat(PoePobImportService.leapRing(ie)).isNull(); // "in radius of X" 는 IE 규칙
    String toh =
        String.join(
            NL,
            "Rarity: UNIQUE",
            "Thread of Hope",
            "Crimson Jewel",
            "Radius: Variable",
            "Only affects Passives in Large Ring",
            "Passives in Radius can be Allocated without being connected to your tree");
    assertThat(PoePobImportService.leapRing(toh)).isEqualTo("1680:2040");
    String leap =
        String.join(
            NL,
            "Rarity: UNIQUE",
            "Intuitive Leap",
            "Viridian Jewel",
            "Radius: Small",
            "Passives in Radius can be Allocated without being connected to your tree");
    assertThat(PoePobImportService.leapRing(leap)).isEqualTo("0:960");
    assertThat(PoePobImportService.impossibleEscapeKey(toh, name -> 1)).isNull();
  }
}
