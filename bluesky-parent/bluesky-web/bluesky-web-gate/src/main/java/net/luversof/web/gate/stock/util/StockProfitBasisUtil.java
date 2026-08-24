package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.util.List;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 화면이 쓰는 손익 기준과, 그 기준이 빼지 않는 금액을 계산한다.
 *
 * <p>api-stock 은 평가손익을 두 기준으로 준다 &mdash; {@code evaluationProfit} 은 매수 원가만, {@code
 * evaluationProfitNet} 은 매수 원가에 <b>매수 수수료</b>까지 더한 것을 원가로 본다 ({@code currentTotalCostNet =
 * currentTotalCost + fee}). 화면은 거래 행이 증권사 기록값이라 기본 기준으로 통일했고, 그래서 표시되는 평가손익에는 보유분에 남아 있는 매수 수수료가
 * <b>빠져 있지 않다</b>.
 *
 * <p>그 금액을 화면에 밝히지 않으면 사용자는 수수료가 반영된 값으로 오해한다(실측 2026-08-23: 24,986 원. 매도분까지 합친 기간 매수 수수료 33,787
 * 원과는 다른 값이다 &mdash; 판 만큼은 원가에서 이미 빠졌기 때문이다).
 *
 * <p>이 관계는 <b>근사가 아니라 등식</b>이다. 원장을 되짚어 아직 팔지 않은 수량에 실려 있는 매수 수수료를 모으면(매도할 때마다 수수료도 같은 비율로 덜어낸다)
 * 24,986.19 로 두 기준의 차이와 <b>잔차 0.00</b> 으로 맞는다. 검사 스크립트의 불변식 "평가손익 두 기준의 차이는 보유분 매수 수수료와 같다" 가 매번 이
 * 등식을 확인하므로, 원가에 무엇이 더 들어가거나 빠지면 그때 드러난다.
 */
public final class StockProfitBasisUtil {

  private StockProfitBasisUtil() {}

  /**
   * 표시되는 평가손익에 아직 반영되지 않은 매수 수수료(보유분).
   *
   * <p>{@code evaluationProfit - evaluationProfitNet} 의 합이다. 둘 중 하나라도 없는 행은 건너뛴다(뺄 수 없다).
   */
  public static BigDecimal excludedHoldingBuyFee(List<TradeProfit> profits) {
    if (profits == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal total = BigDecimal.ZERO;
    for (TradeProfit profit : profits) {
      if (profit == null
          || profit.evaluationProfit() == null
          || profit.evaluationProfitNet() == null) {
        continue;
      }
      total = total.add(profit.evaluationProfit().subtract(profit.evaluationProfitNet()));
    }
    return total;
  }
}
