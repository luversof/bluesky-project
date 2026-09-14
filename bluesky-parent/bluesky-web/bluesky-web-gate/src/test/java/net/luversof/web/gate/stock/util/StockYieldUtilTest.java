package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 연 환산 수익률.
 *
 * <p>실측 2026-09-12(배당 수익률 분석 카드, 전체 기간): 기간 일평균 투입원금 수익률 <b>43.55%</b> · 기간 <b>6,463일</b> &rarr;
 * 화면의 "연 환산 수익률" 이 <b>2.46%</b> 였다(43.55 x 365 / 6463 = 2.4594).
 *
 * <p>배당은 재투자 가정이 없어 복리가 아닌 단순 환산이다. 이 계산은 컨트롤러 안에 인라인이라 가드가 없었다 &mdash; 365 를 366 으로 바꾸거나 반올림 자리를
 * 옮겨도 아무도 알아채지 못했다.
 */
class StockYieldUtilTest {

  @Test
  void 화면에_나온_값을_그대로_낸다() {
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("43.55"), 6463))
        .isEqualByComparingTo("2.46");
  }

  @Test
  void 한_해_기간이면_그대로다() {
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("4.12"), 365))
        .isEqualByComparingTo("4.12");
    // 반년이면 두 배가 된다(단순 환산).
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("4.00"), 182))
        .isEqualByComparingTo("8.02");
  }

  @Test
  void 음수도_같은_규칙이다() {
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("-3.00"), 730))
        .isEqualByComparingTo("-1.50");
  }

  @Test
  void 기간이_없으면_값도_없다() {
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("4.12"), 0)).isNull();
    assertThat(StockYieldUtil.annualizedPct(new BigDecimal("4.12"), -5)).isNull();
    assertThat(StockYieldUtil.annualizedPct(null, 365)).isNull();
  }

  /** 계산이 한 곳에만 있어야 한다 - 컨트롤러에 다시 인라인으로 적히면 두 벌이 된다. */
  @Test
  void 계산은_한_곳에만_있다() throws IOException {
    String controller =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java"),
            StandardCharsets.UTF_8);

    assertThat(controller).contains("StockYieldUtil.annualizedPct(");
    assertThat(controller)
        .as("365 를 곱하는 인라인 계산이 남아 있으면 안 된다")
        .doesNotContain("multiply(BigDecimal.valueOf(365))");
  }
}
