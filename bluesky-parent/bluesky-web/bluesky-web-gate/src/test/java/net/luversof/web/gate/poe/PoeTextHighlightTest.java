package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 수치 강조(10-09 C240 — 0 을 가로지르는 범위 "(-40-40)" 가 반쪽만 칠해졌다). */
class PoeTextHighlightTest {

  @Test
  void 음수로_시작하는_범위를_통째로_감싼다() {
    assertThat(PoeText.highlightValues("발견하는 아이템 희귀도 (-40-40)% 감소"))
        .isEqualTo("발견하는 아이템 희귀도 <span class=\"poe-val\">(-40-40)%</span> 감소");
  }

  @Test
  void 기존_토큰은_그대로() {
    assertThat(PoeText.highlightValues("화염 저항 +25%"))
        .isEqualTo("화염 저항 <span class=\"poe-val\">+25%</span>");
    assertThat(PoeText.highlightValues("물리 피해 (25-35)~(36-45) 추가"))
        .isEqualTo(
            "물리 피해 <span class=\"poe-val\">(25-35)</span>~<span class=\"poe-val\">(36-45)</span> 추가");
    assertThat(PoeText.highlightValues("이동 속도 5% 감소"))
        .isEqualTo("이동 속도 <span class=\"poe-val\">5%</span> 감소");
  }
}
