package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 표 안의 비중은 함께 반올림해 표시값의 합이 100.0% 가 되게 한다.
 *
 * <p>실측 2026-09-11(전체 기간): 자산 현황 '종목별 현황' 9 행의 비중 표시값 합이 99.9%, '계좌 보유 종목 상세' 다섯 표 중 둘이 100.1% 였다
 * (합계행은 100.0%). 행마다 따로 반올림해서 생기는 어긋남이다.
 */
class BalancedPctTest {

  private static double sum(List<String> shown) {
    double total = 0;
    for (String s : shown) {
      total += Double.parseDouble(s.replace("%", ""));
    }
    return Math.round(total * 10) / 10d;
  }

  @Test
  void 세_행이_100_1_로_넘치던_경우() {
    // 64.9 + 32.1 + 2.5 = 99.5 가 아니라, 원값이 64.94 / 32.06 / 2.54 처럼 올림이 겹치면 100.1 이 된다.
    List<BigDecimal> values =
        List.of(new BigDecimal("64.95"), new BigDecimal("32.55"), new BigDecimal("2.50"));
    List<String> shown = StockFormatUtil.balancedPct(values, 1);

    assertThat(sum(shown)).isEqualTo(100.0);
    assertThat(shown).hasSize(3);
  }

  @Test
  void 아홉_행이_99_9_로_모자라던_경우() {
    List<BigDecimal> values =
        List.of(
            new BigDecimal("83.77"),
            new BigDecimal("4.70"),
            new BigDecimal("4.54"),
            new BigDecimal("3.43"),
            new BigDecimal("1.81"),
            new BigDecimal("1.29"),
            new BigDecimal("0.27"),
            new BigDecimal("0.13"),
            new BigDecimal("0.06"));
    List<String> shown = StockFormatUtil.balancedPct(values, 1);

    assertThat(sum(shown)).isEqualTo(100.0);
  }

  @Test
  void 행_하나가_원값에서_벗어나는_폭은_한_칸_이내다() {
    List<BigDecimal> values =
        List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
    List<String> shown = StockFormatUtil.balancedPct(values, 1);

    assertThat(sum(shown)).isEqualTo(100.0);
    for (int i = 0; i < values.size(); i++) {
      double diff =
          Math.abs(Double.parseDouble(shown.get(i).replace("%", "")) - values.get(i).doubleValue());
      assertThat(diff).as(i + " 행").isLessThanOrEqualTo(0.1001);
    }
  }

  @Test
  void 합이_100_이_아닌_표는_손대지_않는다() {
    // 필터로 일부만 보는 표처럼 합이 100 이 아니면 행마다 반올림한 값을 그대로 쓴다.
    List<BigDecimal> values = List.of(new BigDecimal("20.04"), new BigDecimal("10.06"));
    List<String> shown = StockFormatUtil.balancedPct(values, 1);

    assertThat(shown).containsExactly("20.0%", "10.1%");
  }

  @Test
  void 빈_목록은_빈_결과다() {
    assertThat(StockFormatUtil.balancedPct(List.of(), 1)).isEmpty();
  }
}
