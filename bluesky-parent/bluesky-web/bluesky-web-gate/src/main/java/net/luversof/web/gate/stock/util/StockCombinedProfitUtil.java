package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.TradeProfit;

/**
 * 자산 현황의 종목별 <b>실현손익 · 누적배당 · 합산손익</b>.
 *
 * <p>이 세 값은 종목 상세에만 있었다. 그래서 "이 종목으로 지금까지 얼마 벌었나" 를 보려면 종목을 하나씩 열어야 했고, 종목끼리 견주는 것은 아예 되지 않았다.
 *
 * <p>정의는 <b>종목 상세와 같은 것을 쓴다</b> &mdash; 실현손익은 {@link TradeProfit#realizedProfit()}(수수료·거래세 제외 기준.
 * {@code realizedProfitNet} 을 쓰면 같은 화면의 거래 행 합계와 어긋난다), 배당은 {@code /api/dividend/totalByStockItem}
 * 의 <b>세후</b> 합이다. 두 화면이 다른 잣대를 쓰면 같은 종목이 두 값을 갖게 된다.
 *
 * <p>합산은 <b>현재 평가손익 + 누적 실현손익 + 누적 배당</b>이다. 종목 상세의 합산 손익은 <i>기간</i> 평가 변동을 쓰지만, 이 화면은 기간이 없는 '지금
 * 보유' 스냅샷이라(컨트롤러가 기간을 떨어낸다) 전 기간이 대상이고, 전 기간의 평가 변동은 곧 현재 평가손익이다(기초가 0 이므로). 그래서 두 화면이 어긋나지 않는다.
 *
 * <p><b>주의</b> &mdash; 이 표에는 지금 <b>보유 중인 종목만</b> 나온다(보유 수량 0 은 걸러진다). 그래서 열을 다 더해도 전체 누적과 같지 않다(실측
 * 2026-09-03: 보유 9 종목의 실현 140,350,295 · 배당 59,067,537 인데, 이미 다 판 34 종목에 실현 85,279,840 · 배당
 * 6,582,497 이 더 있다). 화면이 이 사실을 밝힌다.
 */
public final class StockCombinedProfitUtil {

  private StockCombinedProfitUtil() {}

  /**
   * @param realizedProfit 누적 실현손익
   * @param dividendTotal 누적 배당(세후)
   * @param combinedProfit 현재 평가손익 + 실현손익 + 배당
   */
  public record Breakdown(
      BigDecimal realizedProfit, BigDecimal dividendTotal, BigDecimal combinedProfit) {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** 종목 id 로 찾는다. 화면은 줄마다 한 번 찾아 세 칸을 채운다. */
  public static Map<UUID, Breakdown> byStockItem(
      List<TradeProfit> stockAggregated, Map<UUID, BigDecimal> dividendByStockItem) {
    Map<UUID, Breakdown> result = new LinkedHashMap<>();
    if (stockAggregated == null) {
      return result;
    }
    for (TradeProfit profit : stockAggregated) {
      if (profit == null || profit.stockItemId() == null) {
        continue;
      }
      BigDecimal realized = nz(profit.realizedProfit());
      BigDecimal dividend =
          dividendByStockItem == null
              ? BigDecimal.ZERO
              : nz(dividendByStockItem.get(profit.stockItemId()));
      result.put(
          profit.stockItemId(),
          new Breakdown(
              realized, dividend, nz(profit.evaluationProfit()).add(realized).add(dividend)));
    }
    return result;
  }

  /** 표 아래 합계. 보유 중인 종목만 더한 값이라는 점은 화면이 밝힌다. */
  public static Breakdown total(Map<UUID, Breakdown> byStockItem) {
    var rows = byStockItem == null ? java.util.List.<Breakdown>of() : byStockItem.values();
    return new Breakdown(
        StockAmountUtil.sum(rows, Breakdown::realizedProfit),
        StockAmountUtil.sum(rows, Breakdown::dividendTotal),
        StockAmountUtil.sum(rows, Breakdown::combinedProfit));
  }
}
