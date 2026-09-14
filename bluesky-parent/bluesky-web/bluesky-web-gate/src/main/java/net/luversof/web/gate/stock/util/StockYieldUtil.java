package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 기간이 다른 구간끼리 견줄 수 있게 수익률을 1 년 기준으로 환산한다.
 *
 * <p>배당은 재투자 가정이 없으므로 복리가 아닌 단순 환산이다 &mdash; {@code 기간 수익률 x (365 / 기간일수)}.
 *
 * <p>실측 2026-09-12(배당 수익률 분석, 전체 기간): 기간 일평균 투입원금 수익률 43.55% · 기간 6,463 일 &rarr; 연 환산 <b>2.46%</b>.
 * 이 계산은 컨트롤러 안에 인라인으로 있어 가드가 없었다.
 */
public final class StockYieldUtil {

  private StockYieldUtil() {}

  /** 연 환산 수익률(소수 두 자리). 기간이 0 일 이하이거나 기간 수익률이 없으면 {@code null} &mdash; 화면은 그때 이 줄을 통째로 감춘다. */
  public static BigDecimal annualizedPct(BigDecimal periodYieldPct, long periodDayCount) {
    if (periodYieldPct == null || periodDayCount <= 0) {
      return null;
    }
    return periodYieldPct
        .multiply(BigDecimal.valueOf(365))
        .divide(BigDecimal.valueOf(periodDayCount), 2, RoundingMode.HALF_UP);
  }
}
