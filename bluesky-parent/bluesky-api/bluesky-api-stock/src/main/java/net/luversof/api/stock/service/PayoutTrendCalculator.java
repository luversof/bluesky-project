package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * 분배금 추세(최근 3 개월 평균 대 최근 12 개월 평균).
 *
 * <p>연배당 수익률만 보면 <b>줄어드는 중인 상품</b>을 못 가려낸다. 수익률은 최근 1 년 평균으로 내는데, 지난 1 년 앞쪽의 큰 분배금이 뒤쪽의 삭감을 가려 주기
 * 때문이다. 최근 석 달만 따로 떼어 1 년 평균과 견주면 그 삭감이 드러난다(실측 2026-09-21: PLUS 고배당주위클리커버드콜 -23.3%, RISE
 * 코리아밸류업위클리고정커버드콜 +62.9%).
 *
 * <p><b>회 단위로 센다.</b> 대상이 모두 월 1 회 지급이라 "최근 3 회 = 최근 석 달" 이 성립한다(실측 2026-09-21: 18 종목 모두 한 달에 두 번
 * 이상 준 적 없음, 최근 12 회가 335 일). 달력으로 자르지 않는 편이 지급일이 월말·월초로 밀리는 달에 흔들리지 않는다.
 *
 * <p><b>지급 이력이 모자라면 아무 값도 내지 않는다</b>(null). 두 회뿐이면 최근 3 회와 최근 12 회가 같은 두 건이 되어 추세가 언제나 0.00% 가 되는데,
 * 이 0 은 "안정적" 으로 읽혀 거짓이 된다(실측: KODEX 200커버드콜액티브 · PLUS 200커버드콜액티브).
 */
public final class PayoutTrendCalculator {

  private PayoutTrendCalculator() {}

  /** 최근 구간(회). */
  public static final int RECENT_COUNT = 3;

  /** 견줄 구간(회). */
  public static final int BASE_COUNT = 12;

  /** 이보다 적게 받은 종목은 추세를 내지 않는다. 최근 3 회가 견줄 구간을 거의 다 차지해 버린다. */
  public static final int MINIMUM_PAYOUTS = 6;

  /** 추세 결과. 화면이 "무엇과 무엇을 견줬는지" 를 말할 수 있게 두 평균과 센 횟수를 함께 돌려준다. */
  public record PayoutTrend(
      BigDecimal recentAveragePerShare,
      BigDecimal baseAveragePerShare,
      BigDecimal changePct,
      int recentCount,
      int baseCount) {}

  /**
   * 분배금 추세.
   *
   * @param amountsPerShareDesc 주당 분배금, <b>최근 것이 앞</b>(지급일 내림차순)
   * @return 이력이 모자라거나 견줄 평균이 0 이면 {@code null}
   */
  public static PayoutTrend compute(List<BigDecimal> amountsPerShareDesc) {
    if (amountsPerShareDesc == null) {
      return null;
    }

    List<BigDecimal> amounts =
        amountsPerShareDesc.stream().filter(amount -> amount != null).toList();
    if (amounts.size() < MINIMUM_PAYOUTS) {
      return null;
    }

    // 견줄 구간이 최근 구간보다 넓다는 것은 MINIMUM_PAYOUTS > RECENT_COUNT 가 보장한다
    // (PayoutTrendCalculatorTest 가 그 전제를 직접 지킨다). 여기서 다시 보면 아무도 못 밟는 가지가 된다.
    int recentCount = Math.min(RECENT_COUNT, amounts.size());
    int baseCount = Math.min(BASE_COUNT, amounts.size());

    BigDecimal recentAverage = average(amounts.subList(0, recentCount));
    BigDecimal baseAverage = average(amounts.subList(0, baseCount));
    if (baseAverage.signum() <= 0) {
      return null;
    }

    // 견줄 값은 반올림 전 평균으로 나눈다. 미리 반올림한 값으로 나누면 두 번 반올림한 수가 된다.
    BigDecimal changePct =
        recentAverage
            .subtract(baseAverage)
            .multiply(BigDecimal.valueOf(100))
            .divide(baseAverage, 2, RoundingMode.HALF_UP);
    return new PayoutTrend(
        recentAverage.setScale(2, RoundingMode.HALF_UP),
        baseAverage.setScale(2, RoundingMode.HALF_UP),
        changePct,
        recentCount,
        baseCount);
  }

  private static BigDecimal average(List<BigDecimal> amounts) {
    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal amount : amounts) {
      sum = sum.add(amount);
    }
    return sum.divide(BigDecimal.valueOf(amounts.size()), MathContext.DECIMAL64);
  }
}
