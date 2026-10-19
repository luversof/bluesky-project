package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 계산기 목표 라벨 — 여러 등급 합친 범위(10-16 C373). */
class PoeModSpanTest {

  @Test
  void 가장_낮은_등급_최솟값부터_가장_높은_등급_최댓값까지() {
    assertThat(
            PoeModSpan.span(
                "+(115-129) to maximum Life",
                "+(115-129) to maximum Life",
                "+(85-99) to maximum Life"))
        .isEqualTo("+(85-129) to maximum Life");
    // PoE2 한국어(괄호 없는 범위) — 위치는 영어 기준
    assertThat(
            PoeModSpan.span(
                "생명력 최대치 +115~129", "+(115-129) to maximum Life", "+(85-99) to maximum Life"))
        .isEqualTo("생명력 최대치 +85~129");
    // 추가 피해 두 범위
    assertThat(
            PoeModSpan.span(
                "공격 시 화염 피해 (25~29)~(37~45) 추가",
                "Adds (25-29) to (37-45) Fire damage to Attacks",
                "Adds (12-15) to (20-24) Fire damage to Attacks"))
        .isEqualTo("공격 시 화염 피해 (12~29)~(20~45) 추가");
    // 한 등급뿐이면 그대로
    assertThat(
            PoeModSpan.span(
                "화염 저항 +(41-45)%", "+(41-45)% to Fire Resistance", "+(41-45)% to Fire Resistance"))
        .isEqualTo("화염 저항 +(41-45)%");
  }

  @Test
  void 고정값_자리도_짝지어_밀리지_않는다() {
    // C378 검토 — 범위만 순번으로 짝지어 "Adds 1 to (2-3)" 이면 첫 범위가 "2~7" 로 밀렸다
    assertThat(
            PoeModSpan.span(
                "Adds (5-7) to (11-13) Fire Damage",
                "Adds (5-7) to (11-13) Fire Damage",
                "Adds 1 to (2-3) Fire Damage"))
        .isEqualTo("Adds (1-7) to (2-13) Fire Damage");
    // 자리 수가 다르면 그대로
    assertThat(PoeModSpan.span("+(10-20) Life", "+(10-20) Life", "+5 Life and 3 more"))
        .isEqualTo("+(10-20) Life");
  }
}
