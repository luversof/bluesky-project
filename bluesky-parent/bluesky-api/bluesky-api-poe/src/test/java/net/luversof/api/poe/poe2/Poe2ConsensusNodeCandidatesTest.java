package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * PoE2 닌자 공통 패시브 후보(10-14 C315) — 실빌드 4분의 3 이상이 찍는데 내 빌드에 없는 본 트리 특화 · 핵심을, 약한 셋 중 풀리는 점수가 가장 큰 특화를
 * 빼고 찍는 재분배로 제안한다(인원 순 셋).
 */
class Poe2ConsensusNodeCandidatesTest {

  private final Poe2RefineService refine =
      new Poe2RefineService(null, null, null, null, "unused") {
        @Override
        boolean isNotableOrKeystone(int id) {
          return id != 99; // 99 = 작은 노드
        }

        @Override
        String nodeKo(int id) {
          return "노드" + id;
        }
      };

  private static final String XML =
      "<Tree><Spec ascendClassId=\"1\" nodes=\"1,2,3,10\"></Spec></Tree>";

  private static Poe2.BuildGuide guide(List<Poe2.GuideNode> nodes) {
    return new Poe2.BuildGuide(
        true, null, null, null, null, null, null, null, nodes, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
  }

  private static Poe2NinjaService.Consensus consensus(int n, Map<Integer, Integer> counts) {
    return new Poe2NinjaService.Consensus(
        n, "Ranger", "Deadeye", List.of(), Map.of(), Map.of(), 0.5, Map.of(), Map.of(), counts);
  }

  @Test
  void 많이_찍는데_없는_특화를_재분배로() {
    List<Poe2RefineService.Candidate> out =
        refine.consensusNodeCandidates(
            XML,
            guide(
                List.of(
                    new Poe2.GuideNode(1, "A", "약한1", "Notable", null, 0d, 0d, 2, null),
                    new Poe2.GuideNode(2, "B", "약한2", "Notable", null, 0.5, 0d, 5, null),
                    new Poe2.GuideNode(3, "C", "강한", "Notable", null, 30d, 10d, 9, null))),
            consensus(8, Map.of(10, 8, 20, 7, 21, 6, 22, 5, 99, 8, 23, 8)),
            "balanced");
    // 10 은 이미 찍음 · 22(5/8) 는 4분의 3 미만 · 99 는 작은 노드 → 23(8) · 20(7) · 21(6)
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .containsExactly(
            "닌자 공통 패시브(8/8명): 약한2(풀리는 5점) 빼고 → 노드23 찍기",
            "닌자 공통 패시브(7/8명): 약한2(풀리는 5점) 빼고 → 노드20 찍기",
            "닌자 공통 패시브(6/8명): 약한2(풀리는 5점) 빼고 → 노드21 찍기");
    assertThat(out.get(0).change()).isEqualTo("{\"type\":\"respec\",\"remove\":2,\"add\":23}");
  }

  @Test
  void 가는_길이_풀리는_점수를_넘으면_뺀다() {
    Poe2.BuildGuide g =
        new Poe2.BuildGuide(
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(new Poe2.GuideNode(2, "B", "약한2", "Notable", null, 0.5, 0d, 5, null)),
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
            null,
            null,
            null,
            Map.of(23, 6, 20, 5)); // 21 은 길이 모름(8점 밖)
    List<Poe2RefineService.Candidate> out =
        refine.consensusNodeCandidates(
            XML, g, consensus(8, Map.of(20, 7, 21, 7, 23, 8)), "balanced");
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .containsExactly("닌자 공통 패시브(7/8명): 약한2(풀리는 5점) 빼고 → 노드20 찍기");
  }

  @Test
  void 뺄_특화가_없거나_공통_특성이_없으면_없다() {
    Poe2NinjaService.Consensus c = consensus(8, Map.of(20, 8));
    assertThat(refine.consensusNodeCandidates(XML, guide(List.of()), c, "dps")).isEmpty();
    assertThat(
            refine.consensusNodeCandidates(
                XML,
                guide(
                    List.of(
                        new Poe2.GuideNode(1, "A", "전직", "Notable", "Deadeye", 0d, 0d, 3, null))),
                c,
                "dps"))
        .isEmpty();
    assertThat(refine.consensusNodeCandidates(XML, guide(List.of()), null, "dps")).isEmpty();
  }
}
