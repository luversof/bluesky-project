package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * PoE2 패시브 재분배 후보(10-14 P4) — 기여가 가장 작은 본 트리 특화 둘 × 다음 패시브 상위 셋, 전직 노드는 빼고, 예측 = 찍을 것 − 뺄 것. 목표 축으로
 * 고른다.
 */
class Poe2RespecCandidatesTest {

  private final Poe2RefineService refine = new Poe2RefineService(null, null, null, null, "unused");

  private static Poe2.BuildGuide guide(List<Poe2.GuideNode> nodes, List<Poe2.GuideNextNode> next) {
    return new Poe2.BuildGuide(
        true, null, null, null, null, null, null, null, nodes, null, null, null, null, null, null,
        null, null, next, List.of(), null, null, null, null);
  }

  private static Poe2.GuideNode node(int id, String ko, String asc, double dps, double ehp) {
    return new Poe2.GuideNode(
        id, "N" + id, ko, "Notable", asc, dps, ehp, asc == null ? 4 : null, null);
  }

  private static Poe2.GuideNextNode next(int id, String ko, int pts, double dps, double ehp) {
    return new Poe2.GuideNextNode(id, "X" + id, ko, "Notable", pts, dps, ehp, null, null);
  }

  @Test
  void 약한_특화_둘_곱하기_다음_패시브_셋() {
    List<Poe2RefineService.Candidate> out =
        refine.respecCandidates(
            guide(
                List.of(
                    node(1, "강함", null, 20, 5),
                    node(2, "무쓸모", null, 0, 0),
                    node(3, "약함", null, 1, 0.5),
                    node(4, "전직", "Pathfinder", 0, 0)),
                List.of(
                    next(10, "가", 4, 30, 0),
                    next(11, "나", 3, 10, 1),
                    next(12, "다", 5, 5, 0),
                    next(13, "라", 2, 1, 0))),
            "balanced");
    assertThat(out).hasSize(6); // 약한 셋 × 닿는 둘 — "다"(5점)는 풀리는 4점으로 못 닿아 빠진다
    assertThat(out.get(0).label()).isEqualTo("패시브 재분배: 무쓸모(풀리는 4점) 빼고 → 가 찍기(가는 길 4점)");
    assertThat(out.get(0).change()).isEqualTo("{\"type\":\"respec\",\"remove\":2,\"add\":10}");
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .noneMatch(l -> l.contains("전직") || l.contains("라 찍기") || l.contains("다 찍기"));
    Poe2RefineService.Candidate weakNa =
        out.stream().filter(c -> c.label().contains("약함(풀리는 4점) 빼고 → 나")).findFirst().orElseThrow();
    assertThat(weakNa.dpsPct()).isEqualTo(9.0);
    assertThat(weakNa.ehpPct()).isEqualTo(0.5);
  }

  @Test
  void 생존_목표면_생존_기여를_더_무겁게_본다() {
    List<Poe2RefineService.Candidate> out =
        refine.respecCandidates(
            guide(
                List.of(
                    node(1, "피해만", null, 50, 0),
                    node(2, "생존만", null, 0, 30),
                    node(3, "둘다", null, 10, 10)),
                List.of(next(10, "가", 4, 0, 20))),
            "ehp");
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .containsExactly(
            "패시브 재분배: 둘다(풀리는 4점) 빼고 → 가 찍기(가는 길 4점)",
            "패시브 재분배: 피해만(풀리는 4점) 빼고 → 가 찍기(가는 길 4점)",
            "패시브 재분배: 생존만(풀리는 4점) 빼고 → 가 찍기(가는 길 4점)");
  }

  @Test
  void 트리를_갈면_새_트리에도_있는_소켓의_주얼만_남긴다() {
    String old =
        "<Tree><Spec nodes=\"1,2,3\"><Sockets><Socket itemId=\"7\" nodeId=\"2\"/>"
            + "<Socket itemId=\"8\" nodeId=\"3\"/></Sockets><Overrides></Overrides></Spec></Tree>";
    String fresh = "<Tree><Spec nodes=\"1,2,9\"><Overrides></Overrides></Spec></Tree>";
    assertThat(Poe2RefineService.keepSockets(old, fresh, java.util.Set.of(1, 2, 9)))
        .isEqualTo(
            "<Tree><Spec nodes=\"1,2,9\"><Sockets><Socket itemId=\"7\" nodeId=\"2\"/></Sockets>"
                + "<Overrides></Overrides></Spec></Tree>");
    assertThat(Poe2RefineService.keepSockets("<Tree/>", fresh, java.util.Set.of(1)))
        .isEqualTo(fresh);
  }

  @Test
  void 무기_세트_노드는_같은_세트_길에만() {
    List<Poe2RefineService.Candidate> out =
        refine.respecCandidates(
            guide(
                List.of(
                    new Poe2.GuideNode(1, "N1", "본", "Notable", null, 0d, 0d, 4, null),
                    new Poe2.GuideNode(2, "N2", "세트", "Notable", null, 0d, 0d, 4, 2)),
                List.of(
                    new Poe2.GuideNextNode(10, "X10", "본길", "Notable", 3, 20d, 0d, null, null),
                    new Poe2.GuideNextNode(11, "X11", "세트길", "Notable", 3, 5d, 0d, null, 2))),
            "balanced");
    assertThat(out)
        .extracting(Poe2RefineService.Candidate::label)
        .containsExactlyInAnyOrder(
            "패시브 재분배: 본(풀리는 4점) 빼고 → 본길 찍기(가는 길 3점)",
            "패시브 재분배: 세트(풀리는 4점) 빼고 → 세트길 찍기(가는 길 3점 · 세트 2 점수)");
  }

  @Test
  void 가이드에_노드가_없으면_없다() {
    assertThat(refine.respecCandidates(null, "balanced")).isEmpty();
    assertThat(refine.respecCandidates(guide(null, List.of()), "balanced")).isEmpty();
  }
}
