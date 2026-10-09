package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** 고유 아이템 표시 줄(10-03 C58) — 한국어가 영문 두 줄을 한 줄로 합친 경우 영문 꼬리 줄이 또 붙지 않아야. */
class PoeMergeLocaleLinesTest {

  private static List<String> merge(List<String> en, List<String> ko) {
    List<String> out = new ArrayList<>();
    PoeOptimizeService.mergeLocaleLines(out, en, ko);
    return out;
  }

  @Test
  void 합쳐진_한국어는_그대로() {
    // 빛나는 묘약(Coruscating Elixir) 실제 데이터
    List<String> en =
        List.of(
            "25% increased Duration",
            "Chaos Damage taken does not bypass Energy Shield during effect",
            "Removes all but one Life on use",
            "Removed life is Regenerated as Energy Shield over (1-2) seconds");
    List<String> ko =
        List.of(
            "지속시간 25% 증가",
            "플라스크 효과를 받는 동안 받는 카오스 피해가 에너지 보호막에 막힘",
            "생명력을 1만 남기고 제거 제거된 생명력은 (1-2)초에 걸쳐 에너지 보호막으로 재생");
    assertThat(merge(en, ko)).isEqualTo(ko);
  }

  @Test
  void 같은_줄수면_빈_한국어만_영문() {
    assertThat(merge(List.of("a", "b"), Arrays.asList("가", ""))).containsExactly("가", "b");
    assertThat(merge(List.of("a", "b"), null)).containsExactly("a", "b");
  }

  @Test
  void 줄수가_달라도_한국어가_비었으면_줄마다() {
    // 한국어 일부가 빈 줄이면 어느 영문과 짝인지 모르니 예전 규칙(순번) — 영문이 사라지지 않게
    assertThat(merge(List.of("a", "b", "c"), Arrays.asList("가", "")))
        .containsExactly("가", "b", "c");
  }
}
