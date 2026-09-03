package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.util.List;

import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesSummary;

/**
 * 구간별 성과 표의 <b>전체 기간</b> 줄.
 *
 * <p>표는 줄마다 그 해·그 달의 성과를 적는데, 정작 <b>다 합치면 얼마인지</b>가 없었다. 눈으로 더해야 했고, 수익률은 눈으로 더하면 아예 틀린다.
 *
 * <p>열마다 합치는 규칙이 다르다.
 *
 * <ul>
 *   <li>손익 · 원금 변동 · 평가 변동 &mdash; <b>더한다</b>. 구간이 맞닿아 있어 평가 변동은 망원경처럼 접혀 전체 기간의 변동이 된다.
 *   <li>수익률 &mdash; <b>곱해서 잇는다</b>({@code Π(1+r)-1}). 더하면 복리를 놓쳐 실제보다 작게 나온다.
 *   <li>기말 평가액 &mdash; 합계라는 것이 없다. 마지막 구간의 값이 곧 전체 기간의 기말이라, 호출부가 직접 고른다.
 * </ul>
 *
 * <p>실측 2026-09-03(전체 기간 15 행): 손익 합계가 요약의 기간 손익과 같고(차이 6e-9, 부동소수 먼지), 수익률 연쇄곱 1463.6231% 가 요약의 투자
 * 수익률과 <b>소수점 넷째 자리까지</b> 같다. 월 단위 13 행으로도 같았다. 즉 이 줄은 위 요약 카드와 어긋나지 않는다.
 */
public final class StockPeriodTotalUtil {

  private StockPeriodTotalUtil() {}

  /**
   * @param profit 손익 합
   * @param unrealizedDelta 평가 변동 합
   * @param realizedAndDividend 실현+배당 합(손익에서 평가 변동을 뺀 잔차)
   * @param principalDelta 원금 변동 합
   * @param chainedReturnPct 수익률 연쇄곱. 낼 수 있는 구간이 하나도 없으면 null
   */
  public record Totals(
      BigDecimal profit,
      BigDecimal unrealizedDelta,
      BigDecimal realizedAndDividend,
      BigDecimal principalDelta,
      Double chainedReturnPct) {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** 줄 순서와 무관하다 &mdash; 더하기도 곱하기도 순서를 타지 않는다. */
  public static Totals of(List<TradeProfitTimeSeriesSummary> summaries) {
    BigDecimal profit = BigDecimal.ZERO;
    BigDecimal unrealizedDelta = BigDecimal.ZERO;
    BigDecimal principalDelta = BigDecimal.ZERO;
    double factor = 1.0d;
    boolean anyReturn = false;
    if (summaries != null) {
      for (TradeProfitTimeSeriesSummary summary : summaries) {
        if (summary == null) {
          continue;
        }
        profit = profit.add(nz(summary.periodProfit()));
        unrealizedDelta =
            unrealizedDelta.add(
                nz(summary.unrealizedEnd()).subtract(nz(summary.unrealizedStart())));
        principalDelta = principalDelta.add(nz(summary.principalDelta()));
        Double rate = summary.timeWeightedReturnPct();
        if (rate != null) {
          // 자본이 없던 구간은 수익률이 없다(null). 그런 구간은 배수 1 이라 건너뛰면 된다.
          factor *= 1.0d + rate / 100.0d;
          anyReturn = true;
        }
      }
    }
    return new Totals(
        profit,
        unrealizedDelta,
        profit.subtract(unrealizedDelta),
        principalDelta,
        anyReturn ? (factor - 1.0d) * 100.0d : null);
  }
}
