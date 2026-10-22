package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * 월배당 출처 파서들이 함께 쓰는 글자 다듬기(PayoutSourceText).
 *
 * <p>2026-10-22 까지 파서마다 한 벌씩 있었다. 날짜 맞추기는 두 갈래가 <b>일부러</b> 다르다 &mdash; RISE · SOL · TIME 은 시각이 붙은
 * 값도 날짜로 자르고, 날짜만 오는 KODEX · PLUS 는 정확히 8 자리일 때만 바꾼다. 모으면서 그 차이를 여기서 고정한다.
 */
class PayoutSourceTextTest {

  @Test
  void 날짜는_여러_표기를_yyyy_MM_dd_로() {
    assertThat(PayoutSourceText.isoDate("2026-09-15T00:00:00")).isEqualTo("2026-09-15");
    assertThat(PayoutSourceText.isoDate("2026.09.15")).isEqualTo("2026-09-15");
    assertThat(PayoutSourceText.isoDate("20260915")).isEqualTo("2026-09-15");
    assertThat(PayoutSourceText.isoDate(" 2026-9 ")).isEqualTo("2026-9");
    assertThat(PayoutSourceText.isoDate(null)).isEmpty();
  }

  @Test
  void 날짜만_오는_출처는_정확히_8자리일_때만() {
    assertThat(PayoutSourceText.isoDateOfCompact("20260915")).isEqualTo("2026-09-15");
    assertThat(PayoutSourceText.isoDateOfCompact("2026-09-15")).isEqualTo("2026-09-15");
    assertThat(PayoutSourceText.isoDateOfCompact("2026-09-15T00:00:00"))
        .as("시각이 붙은 값은 자르지 않고 그대로 - 가져오기 파서가 형식 오류로 알린다")
        .isEqualTo("2026-09-15T00:00:00");
    assertThat(PayoutSourceText.isoDateOfCompact(null)).isEmpty();
  }

  @Test
  void 숫자는_평문으로_펀드_코드는_마지막_영숫자_조각() {
    assertThat(PayoutSourceText.plain(new BigDecimal("540.0"))).isEqualTo("540");
    assertThat(PayoutSourceText.plain(new BigDecimal("1E+3"))).isEqualTo("1000");
    assertThat(PayoutSourceText.lastCodeSegment("/prod/finderDetail/44J2")).isEqualTo("44J2");
    assertThat(PayoutSourceText.lastCodeSegment("/ko/fund/etf/211097/")).isEqualTo("211097");
    assertThat(PayoutSourceText.lastCodeSegment("/a/b-c")).isEqualTo("a");
    assertThat(PayoutSourceText.lastCodeSegment(null)).isEmpty();
    assertThat(PayoutSourceText.BULK_INPUT_HEADER).startsWith("지급기준일").contains("\t");
  }
}
