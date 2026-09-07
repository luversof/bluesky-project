package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.util.List;

import net.luversof.web.gate.stock.dto.response.YearlyCostSummary;

/**
 * 연도별 세금·비용 표의 <b>합계</b> 줄.
 *
 * <p>연말정산·금투세 때 필요한 수는 대개 "그 해" 가 아니라 "다 합쳐서 얼마" 다. 표는 해마다 한 줄씩만 적고 있어 눈으로 더해야 했다.
 *
 * <p>성과 표와 달리 여기는 <b>일곱 열이 모두 그냥 더하면 되는 값</b>이다 &mdash; 수익률처럼 곱해서 이어야 하는 열도, 기말 평가액처럼 합계라는 것이 없는 열도
 * 없다. 그래서 줄 이름도 '전체 기간' 이 아니라 '합계' 다.
 *
 * <p>실측 2026-09-03(14 개 해): 실현손익 225,584,549 · 수수료 98,836 · 증권거래세 1,885,967 · 배당 세전 73,423,094 ·
 * 과세금액 49,315,333 · 배당소득세 7,770,960 · 배당 세후 65,652,134. 세후 합이 세전 합에서 세금 합을 뺀 값과 정확히 같다(배당 수수료는 늘 0
 * 이다) &mdash; 즉 줄마다 성립하는 식이 합계에서도 성립한다.
 */
public final class StockYearlyCostTotalUtil {

  private StockYearlyCostTotalUtil() {}

  /** 합계 줄. 열 이름은 {@link YearlyCostSummary} 와 같다. */
  public record Totals(
      BigDecimal tradeFee,
      BigDecimal tradeTax,
      BigDecimal realizedProfit,
      BigDecimal dividendGross,
      BigDecimal dividendTaxable,
      BigDecimal dividendTax,
      BigDecimal dividendNet) {}

  public static Totals of(List<YearlyCostSummary> rows) {
    return new Totals(
        StockAmountUtil.sum(rows, YearlyCostSummary::tradeFee),
        StockAmountUtil.sum(rows, YearlyCostSummary::tradeTax),
        StockAmountUtil.sum(rows, YearlyCostSummary::realizedProfit),
        StockAmountUtil.sum(rows, YearlyCostSummary::dividendGross),
        StockAmountUtil.sum(rows, YearlyCostSummary::dividendTaxable),
        StockAmountUtil.sum(rows, YearlyCostSummary::dividendTax),
        StockAmountUtil.sum(rows, YearlyCostSummary::dividendNet));
  }
}
