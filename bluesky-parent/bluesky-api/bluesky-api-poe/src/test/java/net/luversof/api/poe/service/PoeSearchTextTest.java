package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** PoE1 검색어 비교(10-02 C23) — 대소문자 · 공백 무시, PoE2 와 같은 규칙. */
class PoeSearchTextTest {

  @Test
  void 공백_없이_쳐도_띄어_쓴_한국어_이름을_찾는다() {
    String q = PoeSearchText.query("가시나무갑옷");
    assertThat(PoeSearchText.matches(q, "Bramblejack", "가시나무 갑옷")).isTrue();
  }

  @Test
  void 띄어_쳐도_붙여_쓴_이름을_찾는다() {
    String q = PoeSearchText.query("가시 나무");
    assertThat(PoeSearchText.matches(q, "가시나무 갑옷")).isTrue();
  }

  @Test
  void 영어는_대소문자를_가리지_않는다() {
    assertThat(PoeSearchText.matches(PoeSearchText.query("BRAMBLE"), "Bramblejack")).isTrue();
    assertThat(PoeSearchText.matches(PoeSearchText.query("bramble jack"), "Bramblejack")).isTrue();
  }

  @Test
  void 없는_말은_못_찾고_null_이름은_건너뛴다() {
    String q = PoeSearchText.query("없는이름");
    assertThat(PoeSearchText.matches(q, "Bramblejack", null, "가시나무 갑옷")).isFalse();
  }

  @Test
  void 빈_검색어는_거르지_않는다() {
    assertThat(PoeSearchText.query(null)).isNull();
    assertThat(PoeSearchText.query("   ")).isNull();
    assertThat(PoeSearchText.query(" ")).isNull();
    assertThat(PoeSearchText.matches(null, "아무거나")).isTrue();
  }
}
