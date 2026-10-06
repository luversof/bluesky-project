package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 렐리쿼리언 진열장 선택지(10-03 C52) — GGG isMultipleChoiceOption 노드는 전직 포인트가 들지 않고(PoB CountAllocNodes)
 * 진열장마다 하나만 고를 수 있다. 최적화기가 선택지를 유료로 세면 렐릭 하나에 2포인트를 쓰고, 막지 않으면 한 진열장에서 둘을 고른다.
 */
class PoeTreeChoiceOptionTest {

  // 전직 시작 10 — 진열장 20 — 선택지 21 · 22 / 시작 10 — 일반 30
  private static final String TREE =
      """
      {"nodes":[
        {"id":10,"name":"Reliquarian","type":"normal","ascendancy":"Reliquarian","ascendancyStart":true,"stats":[]},
        {"id":20,"name":"Martial Weapon Display","type":"normal","ascendancy":"Reliquarian","stats":[]},
        {"id":21,"name":"Kaom's Roots","type":"normal","ascendancy":"Reliquarian","multipleChoiceOption":true,"stats":["a"]},
        {"id":22,"name":"Elevore","type":"normal","ascendancy":"Reliquarian","multipleChoiceOption":true,"stats":["b"]},
        {"id":30,"name":"Passive Point","type":"normal","ascendancy":"Reliquarian","stats":["Grants 1 Passive Skill Point"]}
      ],
      "edges":[[10,20],[20,21],[20,22],[10,30]],
      "classes":[]}
      """;

  private PoeTreeGraphService service(Path dir) throws Exception {
    Files.writeString(dir.resolve("passive-tree.json"), TREE);
    return new PoeTreeGraphService(dir.toString());
  }

  @Test
  void 선택지는_포인트가_들지_않는다(@TempDir Path dir) throws Exception {
    PoeTreeGraphService tree = service(dir);
    List<Integer> path = tree.shortestPathInAscendancy(Set.of(10), 21, "Reliquarian");
    assertThat(path).containsExactly(20, 21);
    assertThat(tree.ascendancyPathCost(path)).isEqualTo(1); // 진열장 1 + 선택지 0
    assertThat(tree.ascendancyPointsUsed(List.of(10, 20, 21, 30))).isEqualTo(2); // 시작 · 선택지 제외
  }

  @Test
  void 같은_진열장의_두번째_선택지는_못_간다(@TempDir Path dir) throws Exception {
    PoeTreeGraphService tree = service(dir);
    assertThat(tree.shortestPathInAscendancy(Set.of(10, 20, 21), 22, "Reliquarian")).isNull();
    // 자가검사: 첫 선택지가 없으면 같은 노드로 간다(막힌 이유가 형제 선택지라는 증거)
    assertThat(tree.shortestPathInAscendancy(Set.of(10, 20), 22, "Reliquarian"))
        .containsExactly(22);
  }
}
