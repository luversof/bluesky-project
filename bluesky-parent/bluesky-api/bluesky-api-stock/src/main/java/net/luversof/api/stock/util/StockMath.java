package net.luversof.api.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액 · 비율 계산의 작은 공용 도우미.
 *
 * <p>같은 한 줄짜리가 서비스마다 복사돼 있었다(null 을 0 으로 - nz 4 · safe 4, 백분율 2). 2026-10-21 정리에서 한 곳으로 모았다. 동작은 원래
 * 복사본과 같다.
 */
public final class StockMath {

  private StockMath() {}

  /** null 이면 0. */
  public static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  /** numerator / denominator x 100, 소수 2 자리. 분모가 0 이하면 0 - 시세를 못 받은 종목은 수익률을 지어내지 않는다. */
  public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
