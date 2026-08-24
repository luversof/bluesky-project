package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;

/**
 * 화면에 적는 실현손익(기록값)이 그 계좌 자체의 원가로 계산한 값과 크게 갈리는지.
 *
 * <p>기록된 실현손익은 <b>계좌를 합친</b> 원가를 따른다 &mdash; 실측 2026-08-23: 매도 54 건을 원장에서 재계산하니 종목 단위(계좌 합산) 원가로는
 * 50 건(92%)이 재현되는데 계좌x종목 단위로는 38 건(70%)뿐이다. 그래서 계좌별 배분은 그 계좌의 실제 성과와 다를 수 있다.
 *
 * <pre>
 *   연금저축1  기록   415,053  vs 계좌 기준 2,063,739  (차이 1,648,686)
 *   ISA       기록 1,555,597  vs 계좌 기준    14,921  (차이 1,540,676)
 *   연금저축2  기록   478,711  vs 계좌 기준   146,347  (차이   332,364)
 *   위탁      기록 190,029,870 vs 계좌 기준 190,009,539 (차이  20,331)
 *   KB       기록     9,438  vs 계좌 기준       570  (차이     8,868)
 *   동양      기록 33,095,880 vs 계좌 기준 33,095,880 (차이       0)
 * </pre>
 *
 * <p>종목 축에서는 이 문제가 없다 &mdash; 기록값이 종목 단위 기준이라 36 종목 전부 최대 11,835 원(값의 0.009%) 안에서 맞는다. 그래서 종목 상세에는
 * 이 안내를 붙이지 않는다.
 *
 * <p>임계값을 두는 이유: 차이는 거의 모든 계좌에 조금씩 있다. 전부 적으면 안내가 무뎌지고, 정작 5 배 / 100 배로 갈린 두 계좌가 묻힌다. 위 실측에서
 * 연금저축1·ISA·연금저축2 만 걸리고 위탁·KB·동양은 걸리지 않는 값으로 정했다.
 */
public final class RealizedBasisGap {

  /** 이 금액 이하의 차이는 적지 않는다. */
  private static final BigDecimal MIN_AMOUNT = new BigDecimal("10000");

  /** 표시값의 이 비율(1/10)을 넘는 차이만 적는다. */
  private static final BigDecimal MIN_RATIO_DIVISOR = BigDecimal.TEN;

  private RealizedBasisGap() {}

  public static BigDecimal amount(BigDecimal recorded, BigDecimal ownBasis) {
    BigDecimal left = recorded != null ? recorded : BigDecimal.ZERO;
    BigDecimal right = ownBasis != null ? ownBasis : BigDecimal.ZERO;
    return left.subtract(right).abs();
  }

  /** 차이가 금액으로도 비율로도 커서 밝힐 만한지. */
  public static boolean isNotable(BigDecimal recorded, BigDecimal ownBasis) {
    BigDecimal gap = amount(recorded, ownBasis);
    if (gap.compareTo(MIN_AMOUNT) <= 0) {
      return false;
    }
    BigDecimal shown = (recorded != null ? recorded : BigDecimal.ZERO).abs();
    return gap.multiply(MIN_RATIO_DIVISOR).compareTo(shown) > 0;
  }
}
