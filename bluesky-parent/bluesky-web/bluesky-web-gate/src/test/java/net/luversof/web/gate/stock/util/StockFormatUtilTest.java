package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StockFormatUtilTest {

  @Test
  void compactKrw_formatsByEokAndMan() {
    assertThat(StockFormatUtil.compactKrw(0)).isEqualTo("0");
    assertThat(StockFormatUtil.compactKrw(5_300)).isEqualTo("5,300");
    assertThat(StockFormatUtil.compactKrw(10_000)).isEqualTo("1만");
    assertThat(StockFormatUtil.compactKrw(23_100_000)).isEqualTo("2,310만");
    assertThat(StockFormatUtil.compactKrw(123_456_789)).isEqualTo("1억 2,345만");
    assertThat(StockFormatUtil.compactKrw(100_000_000)).isEqualTo("1억"); // 만 자리 0이면 생략
    assertThat(StockFormatUtil.compactKrw(1_200_000_000)).isEqualTo("12억");
  }

  @Test
  void compactKrw_handlesNegative() {
    assertThat(StockFormatUtil.compactKrw(-2_310_000)).isEqualTo("-231만");
    assertThat(StockFormatUtil.compactKrw(-123_456_789)).isEqualTo("-1억 2,345만");
  }
}
