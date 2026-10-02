package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * PoE2 가이드 조각 합치기(10-02) — ⑦ 다음 패시브를 조각마다 나눠 재고 API 가 다시 상위 8 을 고른다. 조각별 상위 8 을 모아 같은 기준(점수당 내림차순,
 * 동점은 id)으로 줄 세우면 한 프로세스로 잰 상위 8 과 같아야 한다.
 */
class Poe2GuideMergeTest {

  private static final JsonNodeFactory F = JsonNodeFactory.instance;

  private static ObjectNode node(int id, int points, double dps, double ehp) {
    ObjectNode n = F.objectNode();
    n.put("id", id);
    n.put("points", points);
    n.put("dps", dps);
    n.put("ehp", ehp);
    return n;
  }

  /** guide2.lua topPer 와 같은 규칙 — 거름(key ≥ 1, 다른 축 ≥ -5) → 점수당 내림차순, 동점 id 오름차순 → 상위 n. */
  private static List<ObjectNode> topPer(List<ObjectNode> list, String key, String other, int n) {
    List<ObjectNode> picked = new ArrayList<>();
    for (ObjectNode s : list) {
      if (s.path(key).asDouble() >= 1 && s.path(other).asDouble() >= -5) {
        picked.add(s);
      }
    }
    picked.sort(
        Comparator.<ObjectNode>comparingDouble(
                s -> -s.path(key).asDouble() / s.path("points").asDouble())
            .thenComparingInt(s -> s.path("id").asInt()));
    return picked.subList(0, Math.min(n, picked.size()));
  }

  @Test
  void shardedTopEightEqualsSingleProcessTopEight() {
    Random r = new Random(7);
    List<ObjectNode> all = new ArrayList<>();
    for (int i = 0; i < 300; i++) {
      // 동점(같은 점수당 값)도 섞는다 — id 로 갈려야 한다
      double dps = (i % 17 == 0) ? 6.0 : Math.round(r.nextDouble() * 400) / 10.0 - 5;
      all.add(node(1000 + i, 1 + r.nextInt(8), dps, Math.round(r.nextDouble() * 200) / 10.0 - 8));
    }
    for (int shards : new int[] {1, 4, 6}) {
      List<JsonNode> parts = new ArrayList<>();
      for (int s = 0; s < shards; s++) {
        List<ObjectNode> mine = new ArrayList<>();
        for (int k = 0; k < all.size(); k++) {
          if (k % shards == s) {
            mine.add(all.get(k));
          }
        }
        ObjectNode part = F.objectNode();
        ArrayNode dpsList = part.putArray("nextDps");
        topPer(mine, "dps", "ehp", 8).forEach(dpsList::add);
        part.put("nextTried", mine.size());
        parts.add(part);
      }
      ArrayNode merged = Poe2PobEngineService.mergePerPoint(parts, "nextDps", "dps");
      List<Integer> got = new ArrayList<>();
      merged.forEach(n -> got.add(n.path("id").asInt()));
      List<Integer> want =
          topPer(all, "dps", "ehp", 8).stream().map(n -> n.path("id").asInt()).toList();
      assertThat(got).as("%d 조각", shards).isEqualTo(want);
    }
  }

  @Test
  void mergeGuideSumsTriedAndKeepsSingleShardFields() {
    List<JsonNode> parts = new ArrayList<>();
    for (int s = 0; s < 3; s++) {
      ObjectNode p = F.objectNode();
      p.put("shard", s);
      p.put("nextTried", 10 + s);
      p.put("tried", 1);
      p.put("gemTried", 2);
      p.putArray("nextDps").add(node(s, 1, 5 + s, 0));
      p.putArray("nextEhp");
      p.putArray("rareTargets").add(F.objectNode().put("slot", "S" + s));
      if (s == 2) {
        p.put("nextWeaponSetSkipped", 4); // 한 조각만 센다
      }
      parts.add(p);
    }
    ObjectNode m = Poe2PobEngineService.mergeGuide(parts);
    assertThat(m.path("nextTried").asInt()).isEqualTo(33);
    assertThat(m.path("tried").asInt()).isEqualTo(3);
    assertThat(m.path("gemTried").asInt()).isEqualTo(6);
    assertThat(m.path("nextWeaponSetSkipped").asInt()).isEqualTo(4);
    assertThat(m.path("nextDps").get(0).path("id").asInt()).as("점수당 가장 큰 것(조각 2)").isEqualTo(2);
    assertThat(m.path("rareTargets").size()).isEqualTo(3);
    assertThat(m.has("shard")).isFalse();
  }
}
