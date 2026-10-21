package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 최적 조합 상태 설명(10-19 C465) — 상태 열쇠의 제작대 · 분열 표시가 결정 표 문구로 바뀐다. */
class PoeCraftOptimizerTest {

  @Test
  void 분열된_옵션은_목표와_잡옵션을_가른다() {
    // 레어 · 목표 1 맞음 · 잡옵션 접두 1 · 막힘 없음 · 분열된 목표(F) + 분열된 접두 잡옵션(f)
    String d = PoeCraftOptimizer.describe("R|1|1,0|0|Ff", 2);
    assertThat(d).contains("목표 1 맞음").contains("분열된 목표 옵션").contains("분열된 잡옵션(접두)");
    assertThat(PoeCraftOptimizer.describe("R|0|0,1|0|g", 1)).contains("분열된 잡옵션(접미)");
  }

  @Test
  void 제작대_표시와_엘드리치_우세() {
    assertThat(PoeCraftOptimizer.describe("Re|3|0,0|0|PT", 2))
        .startsWith("레어 · 작열의 총주교 우세")
        .contains("목표 1·2 맞음")
        .contains("접두어 변경 불가")
        .contains("제작대 목표 옵션");
    assertThat(PoeCraftOptimizer.describe("M|0|0,0|1|", 1))
        .isEqualTo("매직 · 맞은 목표 없음 · 목표 1 자리를 잡옵션이 막음");
  }

  @Test
  void 바꾸기_표시도_설명한다() {
    // C469m — 열쇠 끝 c + 바꾸기마다 0 · 1 · 2
    assertThat(PoeCraftOptimizer.describe("R|4|0,1|0||c0021", 3)).endsWith(" · 수확 바꾸기로 목표 +1");
    assertThat(PoeCraftOptimizer.describe("R|4|0,1|0||c0010", 3)).endsWith(" · 수확 바꾸기 가능");
    assertThat(PoeCraftOptimizer.describe("R|4|0,1|0|", 3)).doesNotContain("바꾸기");
  }

  @Test
  void 빈_자리_조건이면_쪽별_옵션_수를_설명한다() {
    // C520 — 빈 자리 조건이 있으면 열쇠 끝 'o접두수,접미수'(바꾸기 칸 뒤)
    assertThat(PoeCraftOptimizer.describe("R|1|0,1|0||o1,1", 1)).endsWith(" · 옵션 접두 1/접미 1");
    assertThat(PoeCraftOptimizer.describe("R|1|0,1|0||c01|o2,1", 1))
        .contains("수확 바꾸기 가능")
        .endsWith(" · 옵션 접두 2/접미 1");
  }

  @Test
  void 각인은_열쇠_뒤에_붙어_따로_설명한다() {
    // C468 — 지금 상태(레어, 목표 1) + '#' + 각인한 매직 상태(목표 1)
    assertThat(PoeCraftOptimizer.describe("R|1|1,0|0|#M|1|0,0|0|", 2))
        .isEqualTo("레어 · 목표 1 맞음 · 잡옵션 접두 1/접미 0 · 각인: 매직 · 목표 1 맞음");
  }
}
