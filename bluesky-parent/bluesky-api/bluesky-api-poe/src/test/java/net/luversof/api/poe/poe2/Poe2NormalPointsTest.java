package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** PoE2 빌드 요약의 일반 패시브 포인트(10-03 C38) — PoB 규칙: 공용 + 큰 쪽 무기 세트, 전직 · 직업 시작 제외. */
class Poe2NormalPointsTest {

  private static Poe2DataService.TreeIndex tree(int shared, int set1, int set2) {
    Map<Integer, Poe2DataService.TreeNodeLite> nodes = new HashMap<>();
    for (int i = 1; i <= shared + set1 + set2 + 20; i++) {
      nodes.put(
          i, new Poe2DataService.TreeNodeLite("n" + i, null, "normal", null, false, false, 0));
    }
    nodes.put(
        1000, new Poe2DataService.TreeNodeLite("start", null, "classStart", null, false, false, 0));
    nodes.put(
        2000, new Poe2DataService.TreeNodeLite("asc", null, "notable", "Deadeye", false, false, 0));
    return new Poe2DataService.TreeIndex(nodes, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
  }

  private static List<String> ids(int from, int count) {
    List<String> out = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      out.add(String.valueOf(from + i));
    }
    return out;
  }

  @Test
  void 공용_더하기_큰_쪽_세트() {
    // 실빌드 꼴: 공용 91 + 세트 I 24 + 세트 II 24 → 115 (예전 148 처럼 다 더하지 않는다)
    List<Integer> allocated = new ArrayList<>();
    for (int i = 1; i <= 91; i++) allocated.add(i);
    for (int i = 92; i <= 115; i++) allocated.add(i); // 켜진 세트 I 노드가 할당 목록에 합쳐져 있다
    allocated.add(1000);
    allocated.add(2000);
    List<List<String>> sets = List.of(ids(92, 24), ids(116, 24));
    assertThat(Poe2BuildService.normalPoints(allocated, sets, tree(91, 24, 24), java.util.Set.of()))
        .isEqualTo(115);
  }

  @Test
  void 세트가_다르면_큰_쪽만() {
    List<Integer> allocated = new ArrayList<>();
    for (int i = 1; i <= 50; i++) allocated.add(i);
    List<List<String>> sets = List.of(ids(51, 3), ids(54, 10));
    assertThat(Poe2BuildService.normalPoints(allocated, sets, tree(50, 3, 10), java.util.Set.of()))
        .isEqualTo(60);
  }

  @Test
  void 세트_없으면_공용만_직업시작_전직_제외() {
    List<Integer> allocated = new ArrayList<>(List.of(1, 2, 3, 1000, 2000));
    assertThat(
            Poe2BuildService.normalPoints(
                allocated, List.of(List.of(), List.of()), tree(3, 0, 0), java.util.Set.of()))
        .isEqualTo(3);
  }

  @Test
  void 장비가_부여한_노드는_세지_않는다() {
    // 장비 "Allocates n2" — PoB isFreeAllocate. 할당 목록엔 있어도 포인트가 아니다
    List<Integer> allocated = new ArrayList<>(List.of(1, 2, 3));
    assertThat(
            Poe2BuildService.normalPoints(
                allocated, List.of(List.of(), List.of()), tree(3, 0, 0), java.util.Set.of(2)))
        .isEqualTo(2);
  }

  @Test
  void 목소리_심연_홈은_순번_1부터_N개() {
    Map<Integer, Poe2DataService.TreeNodeLite> nodes = new HashMap<>();
    nodes.put(
        10,
        new Poe2DataService.TreeNodeLite(
            "Sinister Jewel Socket", null, "jewel", null, false, false, 1));
    nodes.put(
        11,
        new Poe2DataService.TreeNodeLite(
            "Sinister Jewel Socket", null, "jewel", null, false, false, 2));
    nodes.put(
        12,
        new Poe2DataService.TreeNodeLite(
            "Sinister Jewel Socket", null, "jewel", null, false, false, 3));
    nodes.put(
        20,
        new Poe2DataService.TreeNodeLite("Zarokh's Gift", null, "jewel", null, false, false, 0));
    Poe2DataService.TreeIndex t =
        new Poe2DataService.TreeIndex(nodes, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    assertThat(
            Poe2BuildService.grantedIds(
                java.util.Set.of("Zarokh's Gift"), 2, t, java.util.Set.of()))
        .containsExactly(10, 11, 20);
    assertThat(Poe2BuildService.grantedIds(java.util.Set.of(), 0, t, java.util.Set.of())).isEmpty();
  }

  @Test
  void 주얼이_준_다른_직업_시작점() {
    java.util.Set<String> names = new java.util.HashSet<>();
    java.util.Set<String> alt = new java.util.LinkedHashSet<>();
    int[] sinister = {0};
    // 실제 저장은 두 줄로 나뉘기도 한다(C47)
    Poe2BuildService.readGrants(
        String.join(
            String.valueOf((char) 10),
            "Rarity: UNIQUE",
            "Can Allocate Passives from the",
            "Sorceress's starting point"),
        names,
        sinister,
        alt);
    assertThat(alt).containsExactly("Sorceress");
    Poe2DataService.TreeIndex t =
        new Poe2DataService.TreeIndex(
            Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of("Sorceress", 54447));
    assertThat(Poe2BuildService.grantedIds(names, 0, t, alt)).containsExactly(54447);
    // PoE1 직업명 문구("Shadow's starting point") — 같은 시작 노드를 쓰는 PoE2 직업(Monk)으로
    Poe2DataService.TreeIndex t2 =
        new Poe2DataService.TreeIndex(
            Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of("Monk", 44683));
    assertThat(Poe2BuildService.grantedIds(names, 0, t2, java.util.Set.of("Shadow")))
        .containsExactly(44683);
  }

  @Test
  void From_Nothing_은_핵심_id_와_주얼_반경() {
    Map<Integer, Poe2DataService.TreeNodeLite> nodes = new HashMap<>();
    nodes.put(
        77,
        new Poe2DataService.TreeNodeLite("Blood Magic", null, "keystone", null, false, false, 0));
    Poe2DataService.TreeIndex t =
        new Poe2DataService.TreeIndex(nodes, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    String jewel =
        String.join(
            String.valueOf((char) 10),
            "Rarity: UNIQUE",
            "From Nothing",
            "Diamond",
            "Radius: Large",
            "Passives in Radius of Blood Magic can be Allocated",
            "without being connected to your tree"); // 실제 저장은 두 줄
    assertThat(Poe2BuildService.fromNothingRoots(jewel, t)).containsExactly("77:1560");
    assertThat(Poe2BuildService.fromNothingRoots("Rarity: UNIQUE", t)).isEmpty();
  }

  @Test
  void 주얼_칸_둘레_고리() {
    String nl = String.valueOf((char) 10);
    String cm =
        String.join(
            nl,
            "Rarity: UNIQUE",
            "Controlled Metamorphosis",
            "Diamond",
            "Radius: Variable",
            "Only affects Passives in Massive Ring",
            "Passives in Radius can be Allocated without being connected to your tree");
    assertThat(Poe2BuildService.leapRing(cm)).isEqualTo("2160:2520");
    String plain =
        String.join(
            nl,
            "Rarity: UNIQUE",
            "X",
            "Diamond",
            "Radius: Large",
            "Passives in Radius can be Allocated",
            "without being connected to your tree");
    assertThat(Poe2BuildService.leapRing(plain)).isEqualTo("0:1560");
    // From Nothing("Passives in Radius of X …")는 다른 규칙 — 여기선 null
    String fn =
        String.join(
            nl,
            "Radius: Small",
            "Passives in Radius of Conduit can be Allocated",
            "without being connected to your tree");
    assertThat(Poe2BuildService.leapRing(fn)).isNull();
  }
}
