package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 상대 N 개월 프리셋은 화면이 보내는 값(1 ~ 1200)만 받는다. 2026-10-27 감사 전에는 배당 전기 비교가 {@code \d+} 만 보고 parseInt 해
 * rangeMode=99999999999 가 NumberFormatException, 999999999 가 minusMonths DateTimeException 으로 500
 * 이었다.
 */
class RangeRelativeMonthsTest {

  @Test
  void 화면이_보내는_값만_개월로_읽는다() {
    assertThat(StockRangePresetUtil.relativeMonths("3")).isEqualTo(3);
    assertThat(StockRangePresetUtil.relativeMonths(" 12 ")).isEqualTo(12);
    assertThat(StockRangePresetUtil.relativeMonths("1200")).isEqualTo(1200);
  }

  @Test
  void 그_밖의_값은_개월이_아니다() {
    assertThat(StockRangePresetUtil.relativeMonths("1201")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths("999999999")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths("99999999999")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths("0")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths("-3")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths("mtd")).isNull();
    assertThat(StockRangePresetUtil.relativeMonths(null)).isNull();
  }
}
