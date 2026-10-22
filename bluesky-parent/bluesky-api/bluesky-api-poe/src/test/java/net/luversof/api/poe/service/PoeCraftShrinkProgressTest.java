package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 드문 결과 깎기(10-22 C563 ⑧) — 목표를 새로 맞힌 결과의 확률은 Wilson 하한으로 깎이고(표본이 적을수록 많이), 깎은 몫은 진전 없는 결과로 가 합이 1
 * 이다. 진전 결과가 없거나 진전 결과뿐이면 그냥 횟수 / n.
 */
class PoeCraftShrinkProgressTest {

  // 열쇠 "p…" = 목표 1 을 새로 맞힘, 그 밖 = 진전 없음
  private static int mask(String k) {
    return k.startsWith("p") ? 1 : 0;
  }

  private static Map<String, Double> counts(Object... kv) {
    Map<String, Double> m = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
    }
    return m;
  }

  @Test
  void 드문_진전_결과는_깎이고_합은_1() {
    Map<String, Double> few = counts("p1", 2, "a", 598, "b", 400);
    PoeCraftOptimizer.shrinkProgress(few, PoeCraftShrinkProgressTest::mask, 0, 1000, 1.0);
    assertThat(few.get("p1")).isLessThan(2 / 1000.0).isGreaterThan(0);
    assertThat(few.values().stream().mapToDouble(Double::doubleValue).sum())
        .isCloseTo(1, within(1e-12));
    // 진전 없는 결과는 같은 비율로 늘어난다
    assertThat(few.get("a") / few.get("b")).isCloseTo(598 / 400.0, within(1e-9));
    // 표본이 많은 진전 결과는 덜 깎인다(상대적으로)
    Map<String, Double> many = counts("p1", 200, "a", 99_800);
    PoeCraftOptimizer.shrinkProgress(many, PoeCraftShrinkProgressTest::mask, 0, 100_000, 1.0);
    double fewRatio = few.get("p1") / (2 / 1000.0);
    double manyRatio = many.get("p1") / (200 / 100_000.0);
    assertThat(manyRatio).isGreaterThan(fewRatio).isGreaterThan(0.9);
  }

  @Test
  void 진전이_없거나_진전뿐이면_그대로() {
    Map<String, Double> none = counts("a", 3, "b", 1);
    PoeCraftOptimizer.shrinkProgress(none, PoeCraftShrinkProgressTest::mask, 0, 4, 1.0);
    assertThat(none).containsEntry("a", 0.75).containsEntry("b", 0.25);
    Map<String, Double> only = counts("p1", 4);
    PoeCraftOptimizer.shrinkProgress(only, PoeCraftShrinkProgressTest::mask, 0, 4, 1.0);
    assertThat(only).containsEntry("p1", 1.0);
    // 이미 맞힌 목표(curMask)는 진전이 아니다
    Map<String, Double> already = counts("p1", 1, "a", 3);
    PoeCraftOptimizer.shrinkProgress(already, PoeCraftShrinkProgressTest::mask, 1, 4, 1.0);
    assertThat(already).containsEntry("p1", 0.25).containsEntry("a", 0.75);
  }
}
