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

  /**
   * 들고 있던 날 기준 연 수익률(%, 소수 네 자리) = 세후 배당 x 365 / (날마다 들고 있던 원금의 합) x 100.
   *
   * <p>= (세후 배당 / 들고 있던 날 평균 원금) x (365 / 들고 있던 날 수). 기간 전체로 나누는 {@link #annualizedPct} 와 달리 들고 있지
   * 않은 날이 분모를 줄이지 않아, 최근 산 종목도 오래 든 종목과 같은 잣대가 된다(사용자 요청 2026-09-28 - 예전 기준으로 284.68% 이던 종목이 연
   * 44.14%). 원금 x 일 합은 행끼리 더할 수 있어 합계행 · 선택 합계도 같은 식을 쓴다. 합이 0 이하이거나 없으면 {@code null}.
   */
  public static BigDecimal annualizedPctOnHeldDays(
      BigDecimal netAmount, BigDecimal principalCostDaySum) {
    if (netAmount == null || principalCostDaySum == null || principalCostDaySum.signum() <= 0) {
      return null;
    }
    return netAmount
        .multiply(BigDecimal.valueOf(365 * 100))
        .divide(principalCostDaySum, 4, RoundingMode.HALF_UP);
  }
}
